import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

kotlin {
    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    compilerOptions.freeCompilerArgs.add("-Xexpect-actual-classes")
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    jvm("desktop")

    sourceSets {
        val desktopMain by getting

        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.koin.android)
            implementation(libs.logback.android)
            implementation(libs.jmx)
            // preview
            implementation(libs.androidx.compose.ui.tooling.preview)
            implementation(libs.androidx.documentfile)
        }
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            // serialization
            implementation(libs.kotlin.serialization.core)
            implementation(libs.kotlin.serialization.json)
            // ktor
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.ktor.client.websocket)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            // settings
            implementation(libs.settings)
            // room
            implementation(libs.room.runtime)
            implementation(libs.room.ktx)
            // navigation
            implementation(libs.nav)
            // m3
            implementation(libs.m3)
            implementation(libs.m3.icon)
            // koin
            implementation(libs.koin.core)
            // slf4j
            implementation(libs.slf4j.api)
            // sqlite
            implementation(libs.sqlite.bundled)
            // ssh
            implementation(libs.sshd.core)
            // cron-utils
            implementation(libs.cron.utils)
        }
        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.swing)
            implementation(libs.logback.classic)
            runtimeOnly(libs.kotlinx.coroutines.swing)
        }
    }
}

room {
    schemaDirectory(provider { projectDir.resolve("schemas").path })
}

android {
    namespace = "top.e404.mywol"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "top.e404.mywol"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = libs.versions.mywol.get()
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/{DEPENDENCIES,LICENSE,NOTICE}"
        }
    }
    signingConfigs {
        register("release") {
            storeFile = file(getProperty("storeFile"))
            storePassword = getProperty("storePassword")
            keyAlias = getProperty("keyAlias")
            keyPassword = getProperty("keyPassword")
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                file("src/androidMain/proguard-rules.pro")
            )
        }
        getByName("debug") {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    androidComponents {
        onVariants { variant ->
            variant.outputs.forEach { output ->
                if (output is com.android.build.api.variant.impl.VariantOutputImpl) {
                    output.outputFileName = "MyWol-${variant.name}-${output.versionName.get()}.apk"
                }
            }
        }
    }
}

dependencies {
    add("kspAndroid", libs.room.compiler)
    add("kspDesktop", libs.room.compiler)
    debugImplementation(compose.uiTooling)
}

compose.desktop {
    application {
        mainClass = "top.e404.mywol.DesktopMain"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "top.e404.mywol"
            packageVersion = libs.versions.mywol.get()
        }
    }
}

fun Project.getProperty(name: String) =
    getPropertyOrNull(name) ?: error("Property $name not found")

val Project.localPropertiesFile: File get() = project.rootProject.file("local.properties")

fun Project.getLocalProperty(key: String): String? {
    return if (localPropertiesFile.exists()) {
        val properties = Properties()
        localPropertiesFile.inputStream().buffered().use { input ->
            properties.load(input)
        }
        properties.getProperty(key)
    } else {
        localPropertiesFile.createNewFile()
        null
    }
}

fun Project.getPropertyOrNull(name: String) =
    getLocalProperty(name)
        ?: System.getProperty(name)
        ?: System.getenv(name)
        ?: findProperty(name)?.toString()
        ?: properties[name]?.toString()
        ?: extensions.extraProperties.runCatching { get(name).toString() }.getOrNull()

//configurations.all {
//    resolutionStrategy {
//        // 强制使用 28.0.2 版本的 support 包
//        force("com.android.support:support-v4:28.0.2")
//        // 强制使用 4.9.0 版本的 glide 库
//        force("com.github.bumptech.glide:glide:4.9.0")
//
//        failOnVersionConflict()
//    }
//}