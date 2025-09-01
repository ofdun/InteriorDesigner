plugins {
    java
    application
    id("org.openjfx.javafxplugin") version "0.0.13"
}

group = "com.ofdun"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

val junitVersion = "5.10.2"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
    modularity.inferModulePath.set(false)
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

application {
    mainClass.set("com.ofdun.interiordesigner.MyApplication")
}

javafx {
    version = "21"
    modules = listOf("javafx.controls", "javafx.fxml")
}

dependencies {
    implementation("org.controlsfx:controlsfx:11.2.1")
    implementation("com.dlsc.formsfx:formsfx-core:11.6.0")
    implementation("net.synedra:validatorfx:0.5.0")

    implementation("org.ejml:ejml-simple:0.43")

    implementation("org.slf4j:slf4j-api:2.0.16")
    implementation("ch.qos.logback:logback-classic:1.5.16")

    implementation("io.micronaut:micronaut-inject:4.3.4")
    annotationProcessor("io.micronaut:micronaut-inject-java:4.3.4")
    compileOnly("io.micronaut:micronaut-inject-java:4.3.4")
    implementation("jakarta.inject:jakarta.inject-api:2.0.1")

    testImplementation("org.junit.jupiter:junit-jupiter-api:${junitVersion}")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:${junitVersion}")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.withType<JavaExec> {
    jvmArgs = listOf(
        "--module-path", classpath.asPath,
        "--add-modules", "javafx.controls,javafx.fxml",
        "--add-opens", "javafx.graphics/javafx.scene=ALL-UNNAMED"
    )
}

task("runApp", JavaExec::class) {
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("com.ofdun.interiordesigner.MyApplication")
    jvmArgs = listOf(
        "--module-path", classpath.asPath,
        "--add-modules", "javafx.controls,javafx.fxml",
        "--add-opens", "javafx.graphics/javafx.scene=ALL-UNNAMED"
    )
}