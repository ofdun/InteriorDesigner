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
val lombokVersion = "1.18.42"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(24)
    }
    modularity.inferModulePath.set(true)
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

application {
    mainClass.set("com.ofdun.interiordesigner.MyApplication")
}

javafx {
    version = "24"
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

    compileOnly("org.projectlombok:lombok:$lombokVersion")
    annotationProcessor("org.projectlombok:lombok:$lombokVersion")
    testCompileOnly("org.projectlombok:lombok:$lombokVersion")
    testAnnotationProcessor("org.projectlombok:lombok:$lombokVersion")

    testImplementation("org.junit.jupiter:junit-jupiter-api:${junitVersion}")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:${junitVersion}")
}

tasks.withType<Test> {
    useJUnitPlatform()
    jvmArgs = listOf(
        "--enable-preview",
        "--add-modules", "javafx.controls,javafx.fxml",
        "--add-opens", "java.base/java.lang=ALL-UNNAMED",
        "--add-opens", "java.desktop/java.awt=ALL-UNNAMED",
        "--add-opens", "javafx.graphics/javafx.scene=ALL-UNNAMED"
    )
}

tasks.withType<JavaExec> {
    jvmArgs = listOf(
        "--enable-preview",
        "--module-path", classpath.asPath,
        "--add-modules", "javafx.controls,javafx.fxml",
        "--add-opens", "java.base/java.lang=ALL-UNNAMED",
        "--add-opens", "java.desktop/java.awt=ALL-UNNAMED",
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

task("benchmark", JavaExec::class) {
    group = "benchmark"
    description = "Запускает бенчмарк производительности рендеринга с различным количеством потоков"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("com.ofdun.interiordesigner.benchmark.RenderingBenchmark")
    jvmArgs = listOf(
        "--module-path", classpath.asPath,
        "--add-modules", "javafx.controls,javafx.fxml",
        "--add-opens", "javafx.graphics/javafx.scene=ALL-UNNAMED",
        "--add-opens", "java.base/java.lang=ALL-UNNAMED",
        "--add-opens", "java.desktop/java.awt=ALL-UNNAMED",
        "-Xms2g",
        "-Xmx4g"
    )
}
