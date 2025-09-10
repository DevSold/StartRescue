plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose) 
}

android {
    namespace = "com.devstart.startrescue"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.devstart.startrescue"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        // 👇 URL da política de privacidade
        buildConfigField(
            "String",
            "PRIVACY_URL_PT",
            "\"https://devsold.github.io/startrescue-privacy/politica.html\""
        )
        buildConfigField(
            "String",
            "PRIVACY_URL_EN",
            "\"https://devsold.github.io/startrescue-privacy/privacy.html\""
        )
    }


    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    // HABILITAÇÃO DO BuildConfig (necessário p/ campos customizados)
    buildFeatures {
        compose = true
        buildConfig = true
    }

    // Kotlin 2.0+: não usar composeOptions.kotlinCompilerExtensionVersion

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.2")
    implementation(libs.androidx.compiler)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}

configurations.all {
    exclude(group = "com.google.guava", module = "listenablefuture")
}
