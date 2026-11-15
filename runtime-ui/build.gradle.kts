plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
android {
    namespace = "io.github.ulysseszh.pwainstaller.runtime"
    compileSdk = 37
    buildToolsVersion = "36.0.0"
    defaultConfig {
        applicationId = "io.github.ulysseszh.pwainstaller.template"
        minSdk = 21; targetSdk = 37; versionCode = 1; versionName = "1.0.0"
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_11; targetCompatibility = JavaVersion.VERSION_11 }
    sourceSets.getByName("main").apply {
        java.srcDir("../runtime/src/main/java")
        res.srcDir("../runtime/src/main/res")
        res.srcDir(project(":runtime").layout.buildDirectory.dir("generated/iconDensities"))
        assets.srcDir("../runtime/src/main/assets")
        manifest.srcFile("../runtime/src/main/AndroidManifest.xml")
    }
    buildTypes { release {
        isMinifyEnabled = true
        proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "../runtime/proguard-rules.pro", "proguard-rules.pro")
    } }
}
kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11) } }
tasks.named("preBuild") { dependsOn(":runtime:generateIconDensities") }
dependencies {
    implementation(project(":browser")); implementation(project(":core"))
    implementation("com.google.android.material:material:1.12.0")
}
