// `-PhidePeople` builds keep people out of the top photo of each pin, for repo screenshots. It pulls in ML Kit,
// MediaPipe and a 4.6 MB model, so it's off by default.
val hidePeople = providers.gradleProperty("hidePeople").isPresent

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.nbradbury.memorymap"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.nbradbury.memorymap"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    sourceSets.getByName("main") {
        val dir = if (hidePeople) "src/hidePeople" else "src/showPeople"
        kotlin.srcDir("$dir/kotlin")
        assets.srcDir("$dir/assets")
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.exifinterface)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(libs.maplibre.android)
    implementation(libs.coil.compose)
    implementation(libs.kotlinx.serialization.json)
    if (hidePeople) {
        implementation(libs.mlkit.face.detection)
        implementation(libs.mediapipe.tasks.vision)
    }

    testImplementation(libs.junit)
}
