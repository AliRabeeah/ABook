plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}

// ملاحظة مزامنة Google Drive: لا حاجة لملف google-services.json هنا
// لأننا نستخدم GoogleSignInOptions.DEFAULT_SIGN_IN فقط (بدون requestIdToken)،
// والتحقق يتم عبر SHA-1 + اسم الحزمة المسجّلين بـ OAuth Client ID على Google Cloud Console.

android {
    namespace = "com.abook.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.abook.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"
    }

    // إعدادات توقيع الإصدار (release) — تُقرأ من متغيرات بيئة يوفرها GitHub Actions
    signingConfigs {
        create("release") {
            val storeFilePath = System.getenv("KEYSTORE_PATH")
            if (storeFilePath != null) {
                storeFile = file(storeFilePath)
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (System.getenv("KEYSTORE_PATH") != null) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.activity:activity-compose:1.9.1")

    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // قاعدة بيانات محلية للمكتبة وتقدم القراءة
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // تحميل الصور من روابط خارجية (لأغلفة الكتب)
    implementation("io.coil-kt:coil-compose:2.6.0")

    // حفظ إعدادات القراءة (خط، سطوع، وضع)
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // دعم اختيار الملفات وقراءة PDF
    implementation("androidx.documentfile:documentfile:1.0.1")

    // ViewModel + Lifecycle مع Compose
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.4")

    // فك ضغط ملفات EPUB (هي أرشيف ZIP)
    implementation("org.apache.commons:commons-compress:1.26.2")

    // أيقونات إضافية
    implementation("androidx.compose.material:material-icons-extended:1.6.8")

    // خطوط عربية (Cairo/Tajawal) — الملفات نفسها تُضاف يدويًا لمجلد res/font (راجع README)
    // implementation("androidx.compose.ui:ui-text-google-fonts:1.6.8") // غير مستخدم — استبدلناه بخطوط مضمّنة محليًا

    // مزامنة Google Drive: تسجيل الدخول + طلبات REST لـ Drive API
    implementation("com.google.android.gms:play-services-auth:21.2.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // تشغيل النص صوتيًا (يعتمد على TextToSpeech المدمج بأندرويد، هذا فقط للـ coroutines wrapper)
    implementation("androidx.core:core-ktx:1.13.1")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
