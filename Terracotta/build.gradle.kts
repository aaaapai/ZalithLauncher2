plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "net.burningtnt.terracotta"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        minSdk = 26
    }

    lint {
        targetSdk = 36
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_25
        targetCompatibility = JavaVersion.VERSION_25
    }
}

dependencies {
    implementation(libs.androidx.appcompat)
}
