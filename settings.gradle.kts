pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "HexaFleet"

include(":core:hexmath")
include(":domain:game")
include(":data:local")
include(":data:multiplayer")
include(":feature:board")
include(":feature:lobby")
include(":app")

