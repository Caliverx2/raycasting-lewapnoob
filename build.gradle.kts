plugins {
    kotlin("jvm") version "2.1.0"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("java")
}

group = "org.lewapnoob"
version = "2"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    testImplementation(kotlin("test"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.1")
    implementation("javazoom:jlayer:1.0.1")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}

tasks {
    shadowJar {
        archiveBaseName.set("rolada_z_gowna")
        archiveClassifier.set("")
        archiveVersion.set("")
    }
}

tasks.withType<Jar> {
    manifest {
        attributes["Main-Class"] = "org.lewapnoob.raycast.MainKt"
    }
}

tasks.jar {
    manifest {
        attributes(
            "Main-Class" to "org.lewapnoob.raycast.MainKt",
            "Class-Path" to "org.lewapnoob.raycast.MainKt",
            "JVM-Options" to "-Xmx2048m -Xms1024m"
        )
    }
}