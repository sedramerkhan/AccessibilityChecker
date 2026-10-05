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

    // Needed for the maven-publish component below: without it there is no "release" component
    // to publish, and publishToMavenLocal silently produces nothing.
    publishing {
        singleVariant("release")
    }
}

dependencies {
    // Not transitive: lintPublish accepts exactly one jar, and the Kotlin JVM plugin adds
    // kotlin-stdlib (and its annotations dependency) to lint-rules automatically. Lint runs the
    // rules in its own classloader, which already provides the stdlib, which is also why
    // lint-api and lint-checks are compileOnly in lint-rules.
    lintPublish(project(":lint-rules")) { isTransitive = false }
}

group = "org.svu.sedra"
version = "0.1.0"

publishing {
    publications {
        create<MavenPublication>("release") {
            artifactId = "a11ylint"
            afterEvaluate { from(components["release"]) }
        }
    }
}
