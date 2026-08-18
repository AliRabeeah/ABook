package com.abook.app.data.sync

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * يتعامل مع تسجيل الدخول عبر حساب Google وصلاحية الوصول لمجلد بيانات
 * التطبيق الخاص بـ Drive (appDataFolder) — وهو مجلد مخفي لا يظهر
 * بمساحة تخزين المستخدم العادية ولا يمكن لأي تطبيق آخر رؤيته.
 *
 * ⚠️ يتطلب هذا إعداد مسبق من طرفك (مرة واحدة فقط):
 * 1) أنشئ مشروعًا على https://console.cloud.google.com
 * 2) فعّل "Google Drive API" من مكتبة الـ APIs
 * 3) أنشئ بيانات اعتماد OAuth Client ID من نوع "Android"
 *    - Package name: com.abook.app
 *    - SHA-1: احصل عليه من نفس الـ Keystore المستخدم بالتوقيع (راجع قسم GitHub Actions بالـ README)
 * 4) لا حاجة لأي Client Secret أو مفتاح داخل الكود — GoogleSignIn يتحقق عبر SHA-1 + Package name فقط
 */
object GoogleAuthManager {

    private const val DRIVE_APPDATA_SCOPE = "https://www.googleapis.com/auth/drive.appdata"

    fun getSignInClient(context: Context): GoogleSignInClient {
        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestScopes(Scope(DRIVE_APPDATA_SCOPE))
            .requestEmail()
            .build()
        return GoogleSignIn.getClient(context, options)
    }

    fun getSignInIntent(context: Context): Intent = getSignInClient(context).signInIntent

    fun getLastSignedInAccount(context: Context): GoogleSignInAccount? =
        GoogleSignIn.getLastSignedInAccount(context)

    suspend fun signOut(context: Context) = withContext(Dispatchers.IO) {
        runCatching { getSignInClient(context).signOut() }
    }

    /** يجلب رمز وصول (Access Token) صالح لاستدعاء Drive REST API. */
    suspend fun getAccessToken(context: Context, account: GoogleSignInAccount): String? =
        withContext(Dispatchers.IO) {
            runCatching {
                val scope = "oauth2:$DRIVE_APPDATA_SCOPE"
                com.google.android.gms.auth.GoogleAuthUtil.getToken(context, account.account!!, scope)
            }.getOrNull()
        }
}
