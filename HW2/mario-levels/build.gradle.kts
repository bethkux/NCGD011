import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm") version "2.3.20"
}

group = "cz.cuni.gamedev.nail123"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}
dependencies {
    implementation(kotlin("stdlib-jdk8"))

    implementation("org.apache.commons", "commons-csv", "1.14.1")
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_25)
    }
}

tasks.register<JavaExec>("singleRun") {
    group = "run"

    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("RunGameKt")
}

tasks.register<JavaExec>("generateCSV") {
    group = "run"

    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("GatherMetricsKt")
}
