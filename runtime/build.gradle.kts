import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
android {
	namespace = "io.github.ulysseszh.pwainstaller.runtime"
	compileSdk = 37
	buildToolsVersion = "36.0.0"
	defaultConfig {
		applicationId = "io.github.ulysseszh.pwainstaller.template"
		minSdk = 21
		targetSdk = 37
		versionCode = 1
		versionName = "1.0.0"
	}
	compileOptions { sourceCompatibility = JavaVersion.VERSION_11; targetCompatibility = JavaVersion.VERSION_11 }
	buildTypes { release {
		isMinifyEnabled = true
		proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
	} }
}
kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11) } }
dependencies { implementation(project(":browser")); implementation(project(":core")) }

/** Distinct resources for Android's density selection; APK generation replaces their payloads. */
abstract class GenerateIconDensities : DefaultTask() {
	@get:InputFile abstract val sourceIcon: RegularFileProperty
	@get:OutputDirectory abstract val outputDirectory: DirectoryProperty
	@TaskAction fun generate() {
		val original = ImageIO.read(sourceIcon.get().asFile)
		val root = outputDirectory.get().asFile.apply { deleteRecursively(); mkdirs() }
		mapOf("mdpi" to 48, "hdpi" to 72, "xhdpi" to 96, "xxhdpi" to 144, "xxxhdpi" to 192).forEach { (density, size) ->
			listOf("web_icon", "web_adaptive_icon", "web_monochrome_icon").forEachIndexed { index, name ->
				val image = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
				image.createGraphics().let { graphics ->
					graphics.drawImage(original, 0, 0, size, size, null); graphics.dispose()
				}
				// Avoid aapt merging resources that will receive different images later.
				image.setRGB(0, 0, index + 1)
				val file = File(root, "mipmap-$density/$name.png").apply { parentFile.mkdirs() }
				ImageIO.write(image, "png", file)
			}
		}
	}
}
val iconDensities = tasks.register<GenerateIconDensities>("generateIconDensities") {
	sourceIcon.set(layout.projectDirectory.file("src/main/res/drawable/web_icon.png"))
	outputDirectory.set(layout.buildDirectory.dir("generated/iconDensities"))
}
androidComponents.onVariants(androidComponents.selector().all()) { variant ->
	variant.sources.res?.addGeneratedSourceDirectory(iconDensities, GenerateIconDensities::outputDirectory)
}
