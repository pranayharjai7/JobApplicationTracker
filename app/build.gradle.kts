import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
}
val geminiApiKey = localProperties.getProperty("GEMINI_API_KEY", "")
val groqApiKey = localProperties.getProperty("GROQ_API_KEY", "")
val openRouterApiKey = localProperties.getProperty("OPENROUTER_API_KEY", "")

android {
    namespace = "com.pranay.jobtracker"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.pranay.jobtracker"
        minSdk = 26
        targetSdk = 34
        versionCode = 2
        versionName = "1.0.1"

        testInstrumentationRunner = "com.pranay.jobtracker.HiltTestRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        buildConfigField("String", "GEMINI_API_KEY", "\"$geminiApiKey\"")
        buildConfigField("String", "GROQ_API_KEY", "\"$groqApiKey\"")
        buildConfigField("String", "OPENROUTER_API_KEY", "\"$openRouterApiKey\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            isCrunchPngs = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/DEPENDENCIES"
            excludes += "META-INF/LICENSE.md"
            excludes += "META-INF/LICENSE-notice.md"
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    
    // Compose
    implementation(platform("androidx.compose:compose-bom:2024.05.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    
    // Extended Icons
    implementation("androidx.compose.material:material-icons-extended")

    // Room
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    // Hilt
    implementation("com.google.dagger:hilt-android:2.50")
    ksp("com.google.dagger:hilt-compiler:2.50")
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Coil for image loading
    implementation("io.coil-kt:coil-compose:2.5.0")
    implementation("io.coil-kt:coil-gif:2.5.0")

    // Google Auth & API Client
    implementation("com.google.android.gms:play-services-auth:21.0.0")
    implementation("com.google.api-client:google-api-client-android:1.33.0")
    implementation("com.google.apis:google-api-services-gmail:v1-rev110-1.25.0")

    // Wear OS Data Layer
    implementation("com.google.android.gms:play-services-wearable:18.1.0")

    // Gemini
    implementation("com.google.ai.client.generativeai:generativeai:0.9.0")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("com.google.http-client:google-http-client-android:1.43.3")
    implementation("com.google.apis:google-api-services-gmail:v1-rev20231023-2.0.0")

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // Biometric & Security
    implementation("androidx.biometric:biometric:1.2.0-alpha05")
    implementation("androidx.security:security-crypto-ktx:1.1.0-alpha06")
    implementation("net.zetetic:android-database-sqlcipher:4.5.4@aar")
    implementation("androidx.sqlite:sqlite-ktx:2.4.0")

    testImplementation("junit:junit:4.13.2")
    testImplementation("io.mockk:mockk:1.13.10")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("app.cash.turbine:turbine:1.0.0")
    testImplementation("com.google.truth:truth:1.1.5")
    
    // Room Testing
    testImplementation("androidx.room:room-testing:$roomVersion")

    // Hilt Testing
    testImplementation("com.google.dagger:hilt-android-testing:2.50")
    androidTestImplementation("com.google.dagger:hilt-android-testing:2.50")
    androidTestImplementation("io.mockk:mockk-android:1.13.10")
    kspAndroidTest("com.google.dagger:hilt-compiler:2.50")

    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.05.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("com.google.truth:truth:1.1.5")
    
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
