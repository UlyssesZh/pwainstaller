plugins { id("com.android.library"); id("org.jetbrains.kotlin.android") }
android {
	namespace = "io.github.ulysseszh.pwainstaller.browser"
	compileSdk = 37
	buildToolsVersion = "36.0.0"
	defaultConfig { minSdk = 21 }
	compileOptions { sourceCompatibility = JavaVersion.VERSION_11; targetCompatibility = JavaVersion.VERSION_11 }
}
kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11) } }

dependencies {
	implementation(project(":core"))
	implementation("androidx.core:core:1.13.1")
	implementation("androidx.webkit:webkit:1.12.1")
}
