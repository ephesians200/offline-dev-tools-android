plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// AdMob IDs. These defaults are Google's public TEST ids (safe to ship while testing, but they earn nothing).
// Put your real ones in gradle.properties (ADMOB_APP_ID=ca-app-pub-xxx~yyy, ADMOB_BANNER_ID=ca-app-pub-xxx/zzz) or pass -P.
val admobAppId = (project.findProperty("ADMOB_APP_ID") as String?) ?: "ca-app-pub-4955598430856685~7274176665"
val admobBannerId = (project.findProperty("ADMOB_BANNER_ID") as String?) ?: "ca-app-pub-4955598430856685/1859952472"
if (admobAppId.startsWith("ca-app-pub-3940256099942544")) logger.warn("WARNING: building with Google's TEST AdMob ids. Set ADMOB_APP_ID / ADMOB_BANNER_ID before releasing.")

android {
    namespace = "com.codingtools.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.codingtools.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 2
        versionName = "1.2"
        manifestPlaceholders["admobAppId"] = admobAppId
        buildConfigField("String", "ADMOB_BANNER_ID", "\"$admobBannerId\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures { compose = true; buildConfig = true }
    testOptions { unitTests.isReturnDefaultValues = true }
    packaging { resources.excludes += setOf("/META-INF/{AL2.0,LGPL2.1}", "META-INF/versions/**", "META-INF/MANIFEST.MF") }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.navigation:navigation-compose:2.8.5")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.fragment:fragment-ktx:1.8.5")          // FragmentActivity for BiometricPrompt
    implementation("androidx.biometric:biometric:1.1.0")             // vault unlock (fingerprint / face / screen lock)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    // On-device only libraries (no server involved)
    implementation("com.google.zxing:core:3.5.3")                 // QR / barcode generation
    implementation("org.bouncycastle:bcprov-jdk18on:1.79")        // BLAKE2, SHA-3, bcrypt, scrypt, Argon2
    // Monetisation: one-time Pro purchase + free-tier banner ad
    implementation("com.android.billingclient:billing-ktx:9.1.0")  // Google Play Billing Library 9.1.0
    implementation("com.google.android.gms:play-services-ads:25.4.0") // AdMob (brings the UMP consent SDK; needs Kotlin 2.1+)
    debugImplementation("androidx.compose.ui:ui-tooling")
    testImplementation("junit:junit:4.13.2")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        freeCompilerArgs.add("-Xskip-metadata-version-check")
    }
}
