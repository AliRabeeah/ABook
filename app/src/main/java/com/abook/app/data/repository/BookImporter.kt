package com.abook.app.data.repository

import android.content.Context
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.graphics.Bitmap
import com.abook.app.data.local.BookFormat
import org.apache.commons.compress.archivers.zip.ZipFile
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

data class ExtractedBookInfo(
    val title: String,
    val author: String?,
    val format: BookFormat,
    val localFilePath: String,
    val coverPath: String?,
    val totalUnits: Int
)

/**
 * مسؤول عن نسخ ملف الكتاب المختار إلى تخزين التطبيق الداخلي،
 * واستخراج البيانات الوصفية (عنوان/مؤلف/غلاف) منه.
 */
class BookImporter(private val context: Context) {

    private val booksDir: File
        get() = File(context.filesDir, "books").apply { mkdirs() }

    private val coversDir: File
        get() = File(context.filesDir, "covers").apply { mkdirs() }

    suspend fun importFromUri(uri: Uri): ExtractedBookInfo? {
        val fileName = queryFileName(uri) ?: "book_${UUID.randomUUID()}"
        val format = when {
            fileName.endsWith(".epub", true) -> BookFormat.EPUB
            fileName.endsWith(".pdf", true) -> BookFormat.PDF
            else -> BookFormat.UNKNOWN
        }
        if (format == BookFormat.UNKNOWN) return null

        // نسخ الملف لتخزين التطبيق الداخلي حتى يبقى متاحًا لو المستخدم حذف الأصل
        val localFile = File(booksDir, "${UUID.randomUUID()}_$fileName")
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(localFile).use { output -> input.copyTo(output) }
        } ?: return null

        return when (format) {
            BookFormat.EPUB -> extractEpubInfo(localFile)
            BookFormat.PDF -> extractPdfInfo(localFile)
            else -> null
        }
    }

    private fun extractEpubInfo(file: File): ExtractedBookInfo {
        var title = file.nameWithoutExtension
        var author: String? = null
        var coverPath = extractEpubCoverInternal(file)
        var chapterCount = 0

        try {
            ZipFile(file).use { zip ->
                val containerEntry = zip.getEntry("META-INF/container.xml")
                val opfPath = if (containerEntry != null) {
                    val containerXml = zip.getInputStream(containerEntry).bufferedReader().readText()
                    Regex("full-path=\"([^\"]+)\"").find(containerXml)?.groupValues?.get(1)
                } else null

                if (opfPath != null) {
                    val opfEntry = zip.getEntry(opfPath)
                    if (opfEntry != null) {
                        val opfXml = zip.getInputStream(opfEntry).bufferedReader().readText()

                        Regex("<dc:title[^>]*>([^<]+)</dc:title>").find(opfXml)
                            ?.groupValues?.get(1)?.let { title = it.trim() }

                        Regex("<dc:creator[^>]*>([^<]+)</dc:creator>").find(opfXml)
                            ?.groupValues?.get(1)?.let { author = it.trim() }

                        chapterCount = Regex("<itemref[^>]*>").findAll(opfXml).count()
                    }
                }
            }
        } catch (e: Exception) {
            // ملف EPUB غير سليم أو تركيبته غير متوقعة — نكتفي بالاسم الافتراضي
        }

        return ExtractedBookInfo(
            title = title,
            author = author,
            format = BookFormat.EPUB,
            localFilePath = file.absolutePath,
            coverPath = coverPath,
            totalUnits = if (chapterCount > 0) chapterCount else 1
        )
    }

    /** يعيد استخراج غلاف EPUB من الملف الأصلي (يُستخدم عند "استعادة الغلاف الأصلي"). */
    fun reExtractEpubCover(filePath: String): String? = extractEpubCoverInternal(File(filePath))

    private fun extractEpubCoverInternal(file: File): String? {
        var coverPath: String? = null
        try {
            ZipFile(file).use { zip ->
                val containerEntry = zip.getEntry("META-INF/container.xml") ?: return null
                val containerXml = zip.getInputStream(containerEntry).bufferedReader().readText()
                val opfPath = Regex("full-path=\"([^\"]+)\"").find(containerXml)
                    ?.groupValues?.get(1) ?: return null
                val opfEntry = zip.getEntry(opfPath) ?: return null
                val opfXml = zip.getInputStream(opfEntry).bufferedReader().readText()

                val coverId = Regex("<meta[^>]*name=\"cover\"[^>]*content=\"([^\"]+)\"").find(opfXml)
                    ?.groupValues?.get(1)
                if (coverId != null) {
                    val hrefMatch = Regex("<item[^>]*id=\"$coverId\"[^>]*href=\"([^\"]+)\"").find(opfXml)
                        ?: Regex("<item[^>]*href=\"([^\"]+)\"[^>]*id=\"$coverId\"").find(opfXml)
                    val coverHref = hrefMatch?.groupValues?.get(1)
                    if (coverHref != null) {
                        val opfDir = opfPath.substringBeforeLast("/", "")
                        val fullCoverPath = if (opfDir.isEmpty()) coverHref else "$opfDir/$coverHref"
                        val coverEntry = zip.getEntry(fullCoverPath)
                        if (coverEntry != null) {
                            val outFile = File(coversDir, "${file.nameWithoutExtension}_cover.jpg")
                            zip.getInputStream(coverEntry).use { input ->
                                FileOutputStream(outFile).use { output -> input.copyTo(output) }
                            }
                            coverPath = outFile.absolutePath
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // تجاهل — سيبقى الغلاف null
        }
        return coverPath
    }

    /** يعيد استخراج غلاف PDF (الصفحة الأولى) من الملف الأصلي. */
    fun reExtractPdfCover(filePath: String): String? {
        val file = File(filePath)
        var coverPath: String? = null
        try {
            val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            PdfRenderer(pfd).use { renderer ->
                if (renderer.pageCount > 0) {
                    renderer.openPage(0).use { page ->
                        val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        val outFile = File(coversDir, "${file.nameWithoutExtension}_cover.jpg")
                        FileOutputStream(outFile).use { out -> bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out) }
                        coverPath = outFile.absolutePath
                    }
                }
            }
            pfd.close()
        } catch (e: Exception) { /* تجاهل */ }
        return coverPath
    }

    private fun extractPdfInfo(file: File): ExtractedBookInfo {
        var coverPath: String? = null
        var pageCount = 0

        try {
            val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            PdfRenderer(pfd).use { renderer ->
                pageCount = renderer.pageCount
                if (pageCount > 0) {
                    renderer.openPage(0).use { page ->
                        val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                        page.render(bitmap, null, null, android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        val outFile = File(coversDir, "${file.nameWithoutExtension}_cover.jpg")
                        FileOutputStream(outFile).use { out ->
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
                        }
                        coverPath = outFile.absolutePath
                    }
                }
            }
            pfd.close()
        } catch (e: Exception) {
            // ملف PDF تالف أو محمي — نكتفي ببيانات افتراضية
        }

        return ExtractedBookInfo(
            title = file.nameWithoutExtension,
            author = null,
            format = BookFormat.PDF,
            localFilePath = file.absolutePath,
            coverPath = coverPath,
            totalUnits = pageCount.coerceAtLeast(1)
        )
    }

    private fun queryFileName(uri: Uri): String? {
        var name: String? = null
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && nameIndex >= 0) {
                name = cursor.getString(nameIndex)
            }
        }
        return name
    }
}
