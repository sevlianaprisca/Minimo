plugins {
    id("com.android.application")
    id("com.google.gms.google-services") // PASTIKAN BARIS INI ADA
}

android {
    namespace = "id.sevliana.minimo"
    compileSdk = 34

    defaultConfig {
        applicationId = "id.sevliana.minimo"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        viewBinding = true
    }

    tasks.withType<JavaCompile>().configureEach {
        options.compilerArgs.add("-Xlint:deprecation")
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // Firebase
    implementation("com.google.firebase:firebase-auth:22.3.1")
    implementation("com.google.firebase:firebase-firestore:24.11.1")
    implementation("com.google.firebase:firebase-storage:20.3.0")

    // Chart & Glide
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
    implementation("com.github.bumptech.glide:glide:4.16.0")

    // Tambahkan ini jika belum ada
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    // Ini adalah library yang menyediakan InstrumentationRegistry
    androidTestImplementation("androidx.test:monitor:1.6.1")
    androidTestImplementation("androidx.test:runner:1.5.2")

    // Library untuk Swipe Decorator
    implementation ("com.github.xabaras:RecyclerViewSwipeDecorator:1.4")

    // ViewModel & LiveData
    implementation ("androidx.lifecycle:lifecycle-viewmodel:2.6.1")
    implementation ("androidx.lifecycle:lifecycle-livedata:2.6.1")

    // Glide untuk gambar (jika belum ada)
    implementation ("com.github.bumptech.glide:glide:4.15.1")

    implementation("com.airbnb.android:lottie:6.0.0")

    // Library Kalender
    implementation("com.github.prolificinteractive:material-calendarview:2.0.1") {
        exclude(group = "com.android.support")
    }    // Library pendukung waktu
    implementation ("com.jakewharton.threetenabp:threetenabp:1.4.4")


}