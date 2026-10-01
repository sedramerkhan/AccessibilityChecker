plugins {
    alias(libs.plugins.kotlin.jvm)
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

dependencies {
    compileOnly(libs.lint.api)
    compileOnly(libs.lint.checks)
    testImplementation(libs.lint.tests)
    testImplementation(libs.lint.api)
    testImplementation(libs.lint.checks)
    testImplementation(libs.junit)
}

tasks.jar {
    manifest {
        attributes("Lint-Registry-v2" to "org.svu.sedra.a11ylint.ComposeA11yIssueRegistry")
    }
}
