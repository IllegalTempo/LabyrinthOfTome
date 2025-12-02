plugins {
    java
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.19"
}

group = "com.yourfault"
version = "1.0-SNAPSHOT"

// Repositories are managed in settings.gradle.kts via dependencyResolutionManagement
// and pluginManagement. Do not declare project repositories here (FAIL_ON_PROJECT_REPOS).

val targetJavaVersion = 21
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(targetJavaVersion))
    }
}

val paperVersion: String by project

dependencies {
    // Keep the Paper API as provided for plugin runtime
    compileOnly("io.papermc.paper:paper-api:${paperVersion}")
    paperweight.paperDevBundle("1.21.10-R0.1-SNAPSHOT")
}

tasks {
    // set Java compatibility
    withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.release.set(targetJavaVersion)
    }

    // Copy the standard jar instead of the shadow jar
    register<Copy>("installPlugin") {
        dependsOn(named("jar"))
        val jarName = "${project.name}-${project.version}.jar"
        from(layout.buildDirectory.file("libs/$jarName"))
        into(file("C:/ProjectFC/Server/plugins")) //C:\Users\user\Desktop\localhost\plugins
        rename("(.*)", "LOT.jar")
    }

    named("build") {
        finalizedBy("installPlugin")
    }
}
