import io.papermc.paperweight.userdev.PaperweightUserDependenciesExtension

plugins {
    id("buildlogic.adapter")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

tasks.withType<JavaCompile>().configureEach {
    sourceCompatibility = "25"
    targetCompatibility = "25"
}

tasks.named("reobfJar") {
    enabled = false
}

tasks.named("assemble") {
    setDependsOn(dependsOn.filterNot { it == tasks.named("reobfJar").get() })
}

dependencies {
    // https://repo.papermc.io/service/rest/repository/browse/maven-public/io/papermc/paper/dev-bundle/
    the<PaperweightUserDependenciesExtension>().paperDevBundle("26.1.2.build.8-alpha")
    compileOnly(libs.paperLib)
}
