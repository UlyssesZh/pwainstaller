import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }

android {
    namespace = "io.github.ulysseszh.pwainstaller"
    compileSdk = 37
    buildToolsVersion = "36.0.0"
    defaultConfig {
        applicationId = "io.github.ulysseszh.pwainstaller"
        minSdk = 21
        targetSdk = 37
        versionCode = 1
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        isCoreLibraryDesugaringEnabled = true
    }
    // Share the exact fallback PNG with the APK template and JVM metadata tests.
    sourceSets.getByName("main").resources.apply {
        srcDir("../runtime/src/main/res/drawable")
        include("web_icon.png")
    }
    buildTypes { release { isMinifyEnabled = false } }
    packaging { resources.excludes += listOf("META-INF/versions/**", "META-INF/*.kotlin_module", "META-INF/DEPENDENCIES") }
    lint { disable += "QueryAllPackagesPermission" }
}
kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11) } }

abstract class StageRuntimeTemplate : DefaultTask() {
    @get:InputFile abstract val templateApk: RegularFileProperty
    @get:InputFile abstract val browserTemplateApk: RegularFileProperty
    @get:OutputDirectory abstract val outputDirectory: DirectoryProperty
    @TaskAction fun stage() {
        val destination = outputDirectory.file("runtime.apk").get().asFile
        destination.parentFile.mkdirs()
        templateApk.get().asFile.copyTo(destination, overwrite = true)
        browserTemplateApk.get().asFile.copyTo(outputDirectory.file("runtime-ui.apk").get().asFile, overwrite = true)
    }
}
val template = tasks.register<StageRuntimeTemplate>("copyRuntimeTemplate") {
    dependsOn(":runtime:assembleRelease", ":runtime-ui:assembleRelease")
    templateApk.set(project(":runtime").layout.buildDirectory.file("outputs/apk/release/runtime-release-unsigned.apk"))
    browserTemplateApk.set(project(":runtime-ui").layout.buildDirectory.file("outputs/apk/release/runtime-ui-release-unsigned.apk"))
    outputDirectory.set(layout.buildDirectory.dir("generated/templateAssets"))
}
androidComponents.onVariants(androidComponents.selector().all()) { variant ->
    variant.sources.assets?.addGeneratedSourceDirectory(template, StageRuntimeTemplate::outputDirectory)
}

/**
 * apksig's ASN.1 annotations are not @Inherited. getAnnotation has the same behavior here,
 * and unlike Class.getDeclaredAnnotation it exists on Android API 21. Rebuild the constant
 * pool with stable indices so the published signer can run on the requested minimum API.
 * Signature algorithms and certificate checks are unchanged; JVM/device tests verify output.
 */
abstract class AndroidApkSigner : DefaultTask() {
    @get:Classpath abstract val sourceJars: ConfigurableFileCollection
    @get:OutputFile abstract val outputJar: RegularFileProperty
    @TaskAction fun adapt() {
        val destination = outputJar.get().asFile
        destination.parentFile.mkdirs()
        ZipInputStream(sourceJars.singleFile.inputStream()).use { input ->
            ZipOutputStream(destination.outputStream()).use { output ->
                while (true) {
                    val entry = input.nextEntry ?: break
                    if (entry.isDirectory || Regex("META-INF/.*\\.(SF|RSA|DSA|EC)").matches(entry.name)) continue
                    val bytes = input.readBytes()
                    output.putNextEntry(ZipEntry(entry.name).apply { time = 0 })
                    output.write(if (entry.name.endsWith(".class")) adaptClass(bytes) else bytes)
                    output.closeEntry()
                }
            }
        }
    }
    private fun adaptClass(bytes: ByteArray): ByteArray {
        val input = DataInputStream(ByteArrayInputStream(bytes))
        val buffer = ByteArrayOutputStream()
        val output = DataOutputStream(buffer)
        val magic = input.readInt()
        require(magic == 0xcafebabe.toInt())
        output.writeInt(magic)
        output.writeShort(input.readUnsignedShort())
        output.writeShort(input.readUnsignedShort())
        val count = input.readUnsignedShort()
        output.writeShort(count)
        var index = 1
        while (index < count) {
            val tag = input.readUnsignedByte()
            output.writeByte(tag)
            when (tag) {
                1 -> output.writeUTF(input.readUTF().let { if (it == "getDeclaredAnnotation") "getAnnotation" else it })
                3, 4 -> output.writeInt(input.readInt())
                5, 6 -> { output.writeLong(input.readLong()); index++ }
                7, 8, 16, 19, 20 -> output.writeShort(input.readUnsignedShort())
                9, 10, 11, 12, 17, 18 -> output.writeInt(input.readInt())
                15 -> { output.writeByte(input.readUnsignedByte()); output.writeShort(input.readUnsignedShort()) }
                else -> error("Unknown class constant-pool tag $tag")
            }
            index++
        }
        input.copyTo(output)
        return buffer.toByteArray()
    }
}
val apksigSource by configurations.creating { isTransitive = false; isCanBeConsumed = false }
val androidSigner = tasks.register<AndroidApkSigner>("prepareAndroidApkSigner") {
    sourceJars.from(apksigSource)
    outputJar.set(layout.buildDirectory.file("generated/signing/apksig-api21.jar"))
}

dependencies {
    implementation(project(":core"))
    implementation(project(":browser"))
    implementation("androidx.work:work-runtime:2.9.1")
    implementation("dev.rikka.shizuku:api:13.1.5")
    implementation("dev.rikka.shizuku:provider:13.1.5")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.core:core:1.13.1")
    implementation("androidx.webkit:webkit:1.12.1")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("com.google.android.material:material:1.12.0")
    apksigSource("com.android.tools.build:apksig:8.13.2")
    implementation(files(androidSigner.flatMap { it.outputJar }))
    implementation("org.jsoup:jsoup:1.18.3")
    implementation("com.caverock:androidsvg-aar:1.4")
    implementation("org.bouncycastle:bcprov-jdk15to18:1.80")
    implementation("org.bouncycastle:bcpkix-jdk15to18:1.80")
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
}
tasks.withType<Test>().configureEach { dependsOn(template) }
