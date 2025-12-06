pluginManagement {
    repositories {
        // 1. 优先使用阿里云的 Gradle 插件镜像
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
        // 2. 阿里云 Google 镜像
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        // 3. 阿里云 Maven Central 镜像 (Kotlin 插件的核心来源)
        maven { url = uri("https://maven.aliyun.com/repository/public") }

        // 4. 保底的原生仓库（如果阿里云没有）
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // 同样在依赖管理中使用阿里云镜像
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        mavenCentral()
        google()

        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "NoteAI"
include(":app")

