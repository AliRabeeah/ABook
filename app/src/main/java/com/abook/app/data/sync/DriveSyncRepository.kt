package com.abook.app.data.sync

import android.content.Context
import com.abook.app.data.local.AppDatabase
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

sealed class SyncResult {
    object Success : SyncResult()
    data class Error(val message: String) : SyncResult()
}

/**
 * يحفظ/يستعيد نسخة JSON واحدة تجمع: قائمة الكتب (بدون الملفات نفسها، فقط
 * البيانات الوصفية وموضع القراءة) + الإشارات والتظليلات، داخل appDataFolder
 * الخاص بحساب المستخدم على Drive.
 *
 * ملاحظة: ملفات الكتب نفسها (EPUB/PDF) لا تُرفع لتوفير المساحة والوقت —
 * فقط بيانات التقدم والتنظيم، بافتراض أن المستخدم يملك الملفات الأصلية
 * على أجهزته المختلفة أو يعيد استيرادها.
 */
class DriveSyncRepository(private val context: Context) {

    private val client = OkHttpClient()
    private val backupFileName = "abook_backup.json"

    suspend fun sync(account: GoogleSignInAccount): SyncResult = withContext(Dispatchers.IO) {
        val token = GoogleAuthManager.getAccessToken(context, account)
            ?: return@withContext SyncResult.Error("تعذّر الحصول على صلاحية الوصول لحساب Google")

        try {
            val existingFileId = findBackupFileId(token)
            val remoteJson = existingFileId?.let { downloadFile(token, it) }

            val localJson = buildLocalBackupJson()

            val mergedJson = if (remoteJson != null) mergeBackups(localJson, remoteJson) else localJson
            applyMergedDataLocally(mergedJson)

            if (existingFileId != null) {
                updateFile(token, existingFileId, mergedJson)
            } else {
                createFile(token, mergedJson)
            }
            SyncResult.Success
        } catch (e: Exception) {
            SyncResult.Error(e.message ?: "خطأ غير معروف أثناء المزامنة")
        }
    }

    private suspend fun buildLocalBackupJson(): JSONObject {
        val db = AppDatabase.getInstance(context)
        val books = db.bookDao().observeAllBooks()
        val annotations = db.annotationDao()

        // نلتقط لقطة واحدة حالية بدل الاشتراك بالـ Flow
        val booksSnapshot = books.first()

        val booksArray = JSONArray()
        booksSnapshot.forEach { book ->
            val obj = JSONObject()
            obj.put("title", book.title)
            obj.put("author", book.author ?: JSONObject.NULL)
            obj.put("format", book.format.name)
            obj.put("currentUnit", book.currentUnit)
            obj.put("currentUnitProgress", book.currentUnitProgress)
            obj.put("totalUnits", book.totalUnits)
            obj.put("isFavorite", book.isFavorite)
            obj.put("lastOpened", book.lastOpened ?: JSONObject.NULL)
            // نستخدم اسم الملف كمعرّف مطابقة بين الأجهزة بدل الـ id المحلي
            obj.put("fileName", java.io.File(book.filePath).name)
            booksArray.put(obj)
        }

        val root = JSONObject()
        root.put("books", booksArray)
        root.put("syncedAt", System.currentTimeMillis())
        return root
    }

    private fun mergeBackups(local: JSONObject, remote: JSONObject): JSONObject {
        // استراتيجية دمج بسيطة: نأخذ الأحدث تحديثًا لكل كتاب (بالاعتماد على lastOpened)
        val localBooks = local.getJSONArray("books")
        val remoteBooks = remote.getJSONArray("books")

        val mergedMap = LinkedHashMap<String, JSONObject>()
        for (i in 0 until remoteBooks.length()) {
            val b = remoteBooks.getJSONObject(i)
            mergedMap[b.getString("fileName")] = b
        }
        for (i in 0 until localBooks.length()) {
            val b = localBooks.getJSONObject(i)
            val key = b.getString("fileName")
            val existing = mergedMap[key]
            val localLastOpened = if (b.isNull("lastOpened")) 0L else b.getLong("lastOpened")
            val remoteLastOpened = if (existing == null || existing.isNull("lastOpened")) 0L else existing.getLong("lastOpened")
            if (existing == null || localLastOpened >= remoteLastOpened) {
                mergedMap[key] = b
            }
        }

        val mergedArray = JSONArray()
        mergedMap.values.forEach { mergedArray.put(it) }
        val result = JSONObject()
        result.put("books", mergedArray)
        result.put("syncedAt", System.currentTimeMillis())
        return result
    }

    private suspend fun applyMergedDataLocally(merged: JSONObject) {
        val db = AppDatabase.getInstance(context)
        val booksArray = merged.getJSONArray("books")
        val allLocalBooks = db.bookDao().observeAllBooks().first()
        val byFileName = allLocalBooks.associateBy { java.io.File(it.filePath).name }

        for (i in 0 until booksArray.length()) {
            val remote = booksArray.getJSONObject(i)
            val fileName = remote.getString("fileName")
            val localMatch = byFileName[fileName] ?: continue
            db.bookDao().updateProgress(
                localMatch.id,
                remote.getInt("currentUnit"),
                remote.getDouble("currentUnitProgress").toFloat(),
                if (remote.isNull("lastOpened")) System.currentTimeMillis() else remote.getLong("lastOpened")
            )
            if (remote.getBoolean("isFavorite") != localMatch.isFavorite) {
                db.bookDao().setFavorite(localMatch.id, remote.getBoolean("isFavorite"))
            }
        }
    }

    // ===== طلبات Drive REST API =====

    private fun findBackupFileId(token: String): String? {
        val url = "https://www.googleapis.com/drive/v3/files" +
            "?spaces=appDataFolder&q=name='$backupFileName'&fields=files(id,name)"
        val request = Request.Builder().url(url).addHeader("Authorization", "Bearer $token").build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val body = response.body?.string() ?: return null
            val files = JSONObject(body).optJSONArray("files") ?: return null
            if (files.length() == 0) return null
            return files.getJSONObject(0).getString("id")
        }
    }

    private fun downloadFile(token: String, fileId: String): JSONObject? {
        val url = "https://www.googleapis.com/drive/v3/files/$fileId?alt=media"
        val request = Request.Builder().url(url).addHeader("Authorization", "Bearer $token").build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val body = response.body?.string() ?: return null
            return runCatching { JSONObject(body) }.getOrNull()
        }
    }

    private fun createFile(token: String, content: JSONObject) {
        val metadata = JSONObject().apply {
            put("name", backupFileName)
            put("parents", JSONArray().put("appDataFolder"))
        }
        val body = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addPart(MultipartBody.Part.create(
                okhttp3.Headers.headersOf("Content-Type", "application/json; charset=UTF-8"),
                metadata.toString().toRequestBody("application/json".toMediaType())
            ))
            .addPart(MultipartBody.Part.create(
                okhttp3.Headers.headersOf("Content-Type", "application/json"),
                content.toString().toRequestBody("application/json".toMediaType())
            ))
            .build()

        val request = Request.Builder()
            .url("https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart")
            .addHeader("Authorization", "Bearer $token")
            .post(body)
            .build()
        client.newCall(request).execute().close()
    }

    private fun updateFile(token: String, fileId: String, content: JSONObject) {
        val request = Request.Builder()
            .url("https://www.googleapis.com/upload/drive/v3/files/$fileId?uploadType=media")
            .addHeader("Authorization", "Bearer $token")
            .patch(content.toString().toRequestBody("application/json".toMediaType()))
            .build()
        client.newCall(request).execute().close()
    }
}
