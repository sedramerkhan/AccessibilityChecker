plugins {
    alias(libs.plugins.android.library)
    id("maven-publish")
}

android {
    namespace = "org.svu.sedra.a11ylint.library"
    compileSdk = 37

    defaultConfig {
        minSdk = 24
    }
}

dependencies {
    lintPublish(project(":lint-rules"))
}

group = "org.svu.sedra"
version = "0.1.0"
