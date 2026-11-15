pluginManagement { repositories { google(); mavenCentral(); gradlePluginPortal() } }
dependencyResolutionManagement {
	repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
	repositories { google(); mavenCentral() }
}
rootProject.name = "PWA Installer"
include(":app", ":runtime", ":runtime-ui", ":browser", ":core")
