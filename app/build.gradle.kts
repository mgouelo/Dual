plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    // id("org.jetbrains.kotlin.kapt")
    alias(libs.plugins.ksp)
    id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin")
}

android {
    namespace = "fr.iutvannes.dual"
    compileSdk =36

    defaultConfig {
        applicationId = "fr.iutvannes.dual"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Permet d'exclure des fichiers du APK final afin d'éviter des conflits de dépendances.
    packaging {
        resources {
            excludes += setOf(
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/license.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt",
                "META-INF/notice.txt",
                "META-INF/ASL2.0",
                "META-INF/*.kotlin_module"
            )
        }
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // On peut retirer le bloc buildFeatures { compose = false }
    // car c'est la valeur par défaut. Je le laisse pour la clarté.
    buildFeatures {
        compose = false
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "META-INF/INDEX.LIST"
            excludes += "META-INF/io.netty.versions.properties"
            excludes += "META-INF/*.kotlin_module"
        }
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    // Base UI
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation("com.google.android.material:material:1.11.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation(libs.androidx.constraintlayout)
    implementation(libs.filament.android)
    implementation(libs.material)

    // Tests
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation("androidx.room:room-testing:2.6.1")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    androidTestImplementation("androidx.test:core:1.5.0")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    testImplementation("org.robolectric:robolectric:4.11.1")

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    implementation("androidx.room:room-runtime:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")

    // Ktor Server CIO
    implementation("io.ktor:ktor-server-core-jvm:3.0.0")
    implementation("io.ktor:ktor-server-cio-jvm:3.0.0")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:3.0.0")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:3.0.0")
    implementation("io.ktor:ktor-server-call-logging-jvm:3.0.0")
    implementation("io.ktor:ktor-server-default-headers-jvm:3.0.0")
    implementation("io.ktor:ktor-server-cors-jvm:3.0.0")
    implementation("io.ktor:ktor-server-compression-jvm:3.0.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    // Logging
    implementation("org.slf4j:slf4j-simple:2.0.13")

    // Fragments KTX
    implementation("androidx.fragment:fragment-ktx:1.8.4")

    // QR Code
    implementation("com.google.zxing:core:3.5.3")

    // Lifecycle / ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.0")

    // Navigation
    implementation("androidx.navigation:navigation-fragment-ktx:2.8.3")
    implementation("androidx.navigation:navigation-ui-ktx:2.8.3")

    //Hash du password
    implementation("at.favre.lib:bcrypt:0.10.2")
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // Envoie de l'email de réinitialisation du mot de passe
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Lecture fichier Excel (.xls et .xlsx)
    implementation("org.apache.poi:poi:5.2.5")
    implementation("org.apache.poi:poi-ooxml:5.2.5")

    // Lecture fichier libre office .odf (ODF Toolkit Simple API)
    implementation("org.odftoolkit:odfdom-java:0.9.0")
    implementation("org.odftoolkit:simple-odf:0.9.0")

    // permet de chager une image depuis une url (utilisé pour la pdp)
    implementation("com.github.bumptech.glide:glide:4.16.0")
}
