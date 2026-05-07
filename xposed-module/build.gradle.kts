plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.android.s22present.xposed"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.android.s22present.xposed"
        minSdk = 30
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        resources {
            merges += "META-INF/xposed/*"
            excludes += "**"
        }
    }
}

dependencies {
    compileOnly(files("libs/libxposed-api-stub.jar"))
}
