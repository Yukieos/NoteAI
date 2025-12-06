import java.util.Properties
import kotlin.apply

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
}
val localProps = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use { load(it) }
    }
}
val openAiKey: String = localProps.getProperty("OPENAI_API_KEY") ?: ""
android {
    namespace = "com.example.noteai"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.example.noteai"
        minSdk = 24
        targetSdk = 36
        buildConfigField("String", "OPENAI_API_KEY", "\"$openAiKey\"")
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildFeatures {
        buildConfig = true   // 打开 BuildConfig 生成
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    // Markwon 核心 (必须)
    implementation("io.noties.markwon:core:4.6.2")
    // 代码高亮插件 (优化 CodeBlock)
   // implementation("io.noties.markwon:syntax-highlight:4.6.0")
  //  implementation("io.noties.prism4j:prism4j:2.0.0")
    // 表格插件 (可选，优化 AI 生成的表格)
    implementation("io.noties.markwon:ext-tables:4.6.2")
}
