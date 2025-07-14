plugins {
    // Aplicamos los plugins definidos en el catálogo de versiones
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
}

android {
    // Namespace único para tu aplicación, evita conflictos.
    namespace = "com.kybers.play"
    compileSdk = 34 // Usamos el SDK de Android 14

    defaultConfig {
        applicationId = "com.kybers.play"
        minSdk = 24 // Mínima versión de Android soportada (Android 7.0)
        targetSdk = 34 // Versión objetivo, debe coincidir con compileSdk
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false // Desactivado por ahora para facilitar la depuración
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    // Opciones de compilación para Kotlin
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }

    // Habilitamos ViewBinding para acceder a las vistas de forma segura
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    // Aquí añadimos todas las librerías usando los alias del archivo libs.versions.toml

    // AndroidX Core & UI - La base de la interfaz de usuario moderna
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.fragment.ktx)

    // Lifecycle - Para manejar el ciclo de vida de los componentes
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)

    // Room - Para la base de datos local (caché)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler) // Procesador de anotaciones para Room

    // WorkManager - Para tareas en segundo plano (sincronización)
    implementation(libs.androidx.work.runtime.ktx)

    // Shimmer - Efecto de carga visual
    implementation(libs.shimmer)

    // ExoPlayer (Media3) - El corazón de nuestro reproductor de video
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.media3.exoplayer.hls) // Para streams HLS (.m3u8)
    implementation(libs.androidx.media3.exoplayer.dash)
    implementation(libs.androidx.media3.exoplayer.smoothstreaming)

    // Retrofit - Para las llamadas a la API de Xtream Codes
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)

    // Glide - Para cargar imágenes de portadas y logos de canales
    implementation(libs.glide)
    ksp(libs.glide.ksp) // Procesador de anotaciones para Glide

    // Testing - Librerías para pruebas
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
