plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.restoselfservice"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.restoselfservice"
        minSdk = 21
        targetSdk = 36
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
}

dependencies {
	// Use double quotes for string arguments
	implementation("com.github.bumptech.glide:glide:4.16.0")
	annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")
	implementation("com.android.volley:volley:1.2.1")
	implementation("androidx.recyclerview:recyclerview:1.3.2")
	implementation("androidx.cardview:cardview:1.0.0")
	implementation("com.xendit:xendit-android:4.2.4")
	implementation("com.journeyapps:zxing-android-embedded:4.3.0")
	implementation("com.google.code.gson:gson:2.10.1")
	implementation("com.google.android.material:material:1.9.0")
	implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

	// These are already correct
	implementation(libs.appcompat)
	implementation(libs.material)
	implementation(libs.activity)
	implementation(libs.constraintlayout)
	testImplementation(libs.junit)
	androidTestImplementation(libs.ext.junit)
	androidTestImplementation(libs.espresso.core)
}

