import com.github.spotbugs.snom.Confidence
import com.github.spotbugs.snom.Effort
import com.github.spotbugs.snom.SpotBugsTask
import org.cyclonedx.Version
import org.cyclonedx.model.Component

plugins {
    java
    jacoco
    id("com.gradleup.shadow") version "9.4.2"
    id("xyz.jpenilla.run-paper") version "3.0.2"
    id("com.diffplug.spotless") version "8.7.0"
    id("com.github.spotbugs") version "6.5.6"
    id("org.owasp.dependencycheck") version "12.2.2"
    id("info.solidsoft.pitest") version "1.19.0"
    id("org.cyclonedx.bom") version "3.2.4"
}

group = "io.github.shaches"
version = "0.0.4"
description = "EventBlock - Event driven OneBlock minigame plugin"

val javaRuntimeVersion = 25
val javaBytecodeVersion = 25
val paperApiVersion = "26.1.2.build.70-stable"
val strictVerification = providers.gradleProperty("strictVerification").map { it.toBoolean() }.orElse(false)
val testcontainersEnabled =
    providers.gradleProperty("testcontainers.enabled")
        .orElse(providers.systemProperty("testcontainers.enabled"))
        .orElse("false")
val paperRuntimeTestIncludes =
    listOf(
        "oneblock/ChestItemsDualModeTest.class",
        "oneblock/ChestItemsEdgeCaseTest.class",
        "oneblock/gui/dialog/DialogMenuManagerTest.class"
    )

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(javaRuntimeVersion))
    }
    withSourcesJar()
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/") { name = "papermc" }
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/") { name = "spigot" }
    maven("https://repo.phoenix616.dev/") { name = "phoenix616" }
    maven("https://repo.helpch.at/releases/") { name = "helpch" }
    maven("https://repo.oraxen.com/releases") { name = "oraxen" }
    maven("https://repo.nexomc.com/releases") { name = "nexo" }
    maven("https://maven.devs.beer/") { name = "devsbeer" }
    maven("https://repo.alessiodp.com/releases/") { name = "alessiodp" }
    maven("https://jitpack.io") { name = "jitpack" }
}

configurations.configureEach {
    resolutionStrategy {
        force("dev.triumphteam:triumph-gui:3.1.13")
    }
}

listOf(
    configurations.compileClasspath,
    configurations.testCompileClasspath,
    configurations.testRuntimeClasspath
).forEach { cfg ->
    cfg {
        attributes {
            attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, javaRuntimeVersion)
        }
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:$paperApiVersion")
    compileOnly("me.clip:placeholderapi:2.12.2")
    compileOnly("dev.lone:api-itemsadder:4.0.10")
    compileOnly("com.nexomc:nexo:1.10.0") {
        exclude(group = "com.nexomc", module = "protectionlib")
    }
    compileOnly("io.th0rgal:oraxen:1.213.0") {
        exclude(group = "me.gabytm.util", module = "actions-spigot")
        exclude(group = "org.jetbrains", module = "annotations")
        exclude(group = "com.ticxo", module = "PlayerAnimator")
        exclude(group = "com.github.stefvanschie.inventoryframework", module = "IF")
        exclude(group = "io.th0rgal", module = "protectionlib")
        exclude(group = "dev.triumphteam", module = "triumph-gui")
        exclude(group = "org.bstats", module = "bstats-bukkit")
        exclude(group = "com.jeff-media", module = "custom-block-data")
        exclude(group = "com.jeff-media", module = "persistent-data-serializer")
        exclude(group = "com.jeff_media", module = "MorePersistentDataTypes")
        exclude(group = "gs.mclo", module = "java")
    }
    compileOnly("net.byteflux:libby-bukkit:1.3.1")
    compileOnly("com.github.spotbugs:spotbugs-annotations:4.10.2")

    implementation("org.bstats:bstats-bukkit:3.2.1")
    implementation("io.github.almighty-satan:XSeries:13.6.0+26.1")
    implementation("com.google.code.gson:gson:2.11.0")
    implementation("com.zaxxer:HikariCP:7.0.2")

    runtimeOnly("com.mysql:mysql-connector-j:9.5.0")
    runtimeOnly("com.h2database:h2:2.4.240")

    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.11.4")
    testImplementation("org.assertj:assertj-core:3.26.3")
    testImplementation("org.mockito:mockito-core:5.16.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.16.0")
    testImplementation("net.bytebuddy:byte-buddy:1.17.0")
    testImplementation("net.bytebuddy:byte-buddy-agent:1.17.0")
    testImplementation("com.github.seeseemelk:MockBukkit-v1.21:3.107.0")
    testImplementation("org.testcontainers:testcontainers:1.20.4")
    testImplementation("org.testcontainers:junit-jupiter:1.20.4")
    testImplementation("org.testcontainers:mysql:1.20.4")
    testCompileOnly("io.papermc.paper:paper-api:$paperApiVersion")
    testRuntimeOnly("io.papermc.paper:paper-api:$paperApiVersion")
    testCompileOnly("me.clip:placeholderapi:2.12.2")
    testRuntimeOnly("me.clip:placeholderapi:2.12.2")

    spotbugsPlugins("com.h3xstream.findsecbugs:findsecbugs-plugin:1.13.0")
}

spotless {
    java {
        target("src/main/java/**/*.java", "src/test/java/**/*.java")
        googleJavaFormat("1.25.2")
            .style("GOOGLE")
            .reflowLongStrings()
        removeUnusedImports()
        trimTrailingWhitespace()
        endWithNewline()
    }
}

jacoco {
    toolVersion = "0.8.13"
}

spotbugs {
    toolVersion.set("4.10.2")
    effort.set(Effort.MAX)
    reportLevel.set(Confidence.MEDIUM)
    ignoreFailures.set(strictVerification.map { !it })
}

dependencyCheck {
    failBuildOnCVSS = 7.0f
    suppressionFile = file("dependency-check-suppressions.xml").absolutePath
    analyzers.ossIndex.enabled = false
}

pitest {
    pitestVersion.set("1.16.1")
    junit5PluginVersion.set("1.2.1")
    targetClasses.set(setOf("oneblock.*"))
    targetTests.set(setOf("oneblock.*"))
    mutationThreshold.set(70)
    coverageThreshold.set(75)
    outputFormats.set(setOf("XML", "HTML"))
    timestampedReports.set(false)
}

tasks.cyclonedxDirectBom {
    includeConfigs = listOf("compileClasspath", "runtimeClasspath")
    skipConfigs = listOf(".*test.*", ".*Test.*")
    projectType.set(Component.Type.LIBRARY)
    schemaVersion.set(Version.VERSION_15)
    includeBomSerialNumber = true
    includeLicenseText = false
    includeMetadataResolution = true
    includeBuildEnvironment = false
}

tasks.cyclonedxBom {
    projectType.set(Component.Type.LIBRARY)
    schemaVersion.set(Version.VERSION_15)
    includeBomSerialNumber = true
    includeLicenseText = false
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(javaBytecodeVersion)
}

tasks.withType<Jar>().configureEach {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    systemProperty("testcontainers.enabled", testcontainersEnabled.get())
    testLogging {
        events("failed", "skipped")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}

tasks.test {
    exclude(paperRuntimeTestIncludes)
    finalizedBy(tasks.jacocoTestReport)
}

tasks.register<Test>("paperRuntimeTest") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Runs tests that currently need a fuller Paper runtime than MockBukkit provides."
    include(paperRuntimeTestIncludes)
    shouldRunAfter(tasks.test)
    ignoreFailures = true
}

tasks.register<Test>("serverTest") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Runs Docker/Testcontainers integration tests."
    include("**/*IT.class")
    shouldRunAfter(tasks.test)
    systemProperty("testcontainers.enabled", "true")
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

tasks.jacocoTestCoverageVerification {
    dependsOn(tasks.test)
    violationRules {
        rule {
            limit {
                counter = "INSTRUCTION"
                value = "COVEREDRATIO"
                minimum = "0.35".toBigDecimal()
            }
            limit {
                counter = "BRANCH"
                value = "COVEREDRATIO"
                minimum = "0.30".toBigDecimal()
            }
        }
    }
}

tasks.named<SpotBugsTask>("spotbugsTest") {
    enabled = false
}

tasks.processResources {
    inputs.property("version", project.version)
    filesMatching("plugin.yml") {
        expand(mapOf("project" to mapOf("version" to project.version)))
    }
}

tasks.jar {
    enabled = false
}

tasks.shadowJar {
    archiveClassifier.set("")
    mergeServiceFiles()
    relocate("org.bstats", "oneblock.utils.stats")
    relocate("com.cryptomorin.xseries", "oneblock.utils.lib")
    listOf(
        "com/cryptomorin/xseries/*/*/*/**",
        "com/cryptomorin/xseries/base/XM*",
        "com/cryptomorin/xseries/base/*y*",
        "com/cryptomorin/xseries/*r*/*/**",
        "com/cryptomorin/xseries/m*/*/**",
        "com/cryptomorin/xseries/*bstr*",
        "com/cryptomorin/xseries/N*",
        "com/cryptomorin/xseries/XA*",
        "com/cryptomorin/xseries/XS*",
        "com/cryptomorin/xseries/XE*",
        "com/cryptomorin/xseries/XG*",
        "com/cryptomorin/xseries/XI*",
        "com/cryptomorin/xseries/XP*",
        "com/cryptomorin/xseries/XT*",
        "com/cryptomorin/xseries/XW*",
        "com/cryptomorin/xseries/XBi*"
    ).forEach(::exclude)
}

tasks.assemble {
    dependsOn(tasks.shadowJar)
}

tasks.build {
    dependsOn(tasks.shadowJar, tasks.cyclonedxBom)
}

tasks.check {
    dependsOn(tasks.spotlessCheck, tasks.jacocoTestReport)
    if (strictVerification.get()) {
        dependsOn(tasks.jacocoTestCoverageVerification)
    }
}

tasks.register("ci") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Runs the default CI build and produces the shaded plugin jar."
    dependsOn(tasks.check, tasks.shadowJar, tasks.cyclonedxBom)
}

tasks.register("format") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Formats Java sources with Spotless."
    dependsOn(tasks.spotlessApply)
}

tasks.register("securityScan") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Runs OWASP dependency-check with the project suppressions."
    dependsOn(tasks.dependencyCheckAnalyze)
}

tasks.register("mutationTest") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Runs PIT mutation testing."
    dependsOn(tasks.pitest)
}

tasks.register("printVersion") {
    group = HelpTasksPlugin.HELP_GROUP
    description = "Prints the project version for release automation."
    doLast {
        println(project.version)
    }
}
