plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "polete.utaplayer"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "polete.utaplayer"
        minSdk = 33
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")

        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        }
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.room.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    //noinspection UseTomlInstead
    implementation("com.google.accompanist:accompanist-permissions:0.37.3")
    //noinspection UseTomlInstead,GradleDependency
    implementation("androidx.media3:media3-exoplayer:1.9.1")
    //noinspection UseTomlInstead,GradleDependency
    implementation("androidx.media3:media3-session:1.9.1")
    //noinspection UseTomlInstead,GradleDependency
    implementation("androidx.media3:media3-ui:1.9.1")
    //noinspection UseTomlInstead
    implementation("androidx.compose.material:material-icons-extended")
    //noinspection UseTomlInstead
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation(libs.androidx.room.runtime)
    //noinspection UseTomlInstead
    implementation("me.saket.squigglyslider:squigglyslider:1.0.0")
    ksp(libs.androidx.room.compiler)
    //noinspection UseTomlInstead
    implementation("androidx.palette:palette-ktx:1.0.0")
    //noinspection UseTomlInstead
    implementation("com.squareup.retrofit2:retrofit:3.0.0")
    //noinspection UseTomlInstead
    implementation("com.squareup.retrofit2:converter-gson:3.0.0")
    implementation("com.mocharealm.accompanist:lyrics-ui:1.0.15")
    implementation("com.mocharealm.accompanist:lyrics-core:0.2.1") //https://github.com/6xingyv/accompanist-lyrics-ui
    implementation("com.materialkolor:material-kolor:4.0.2")
}