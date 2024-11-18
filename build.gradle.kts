import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.utils.addToStdlib.UnsafeCastFunction
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinSerialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.room) apply false
}

subprojects {
    afterEvaluate {
        val optInAnnotations = arrayOf(
            "kotlin.contracts.ExperimentalContracts",
            "kotlin.experimental.ExperimentalTypeInference",
            "kotlinx.serialization.ExperimentalSerializationApi",
            "kotlinx.coroutines.ExperimentalCoroutinesApi",
            "kotlinx.coroutines.FlowPreview",
            "androidx.compose.foundation.layout.ExperimentalLayoutApi",
            "androidx.compose.foundation.ExperimentalFoundationApi",
            "androidx.compose.material3.ExperimentalMaterial3Api",
            "androidx.compose.ui.ExperimentalComposeUiApi",
            "org.jetbrains.compose.resources.ExperimentalResourceApi",
            "kotlin.ExperimentalStdlibApi",
            "androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi",
            "androidx.compose.animation.ExperimentalSharedTransitionApi",
            "androidx.paging.ExperimentalPagingApi",
        )


        @OptIn(UnsafeCastFunction::class)
        val sourceSets =
            extensions.findByName("kotlin").safeAs<KotlinProjectExtension>()?.sourceSets
        sourceSets?.all {
            languageSettings.progressiveMode = true
            optInAnnotations.forEach { a ->
                languageSettings.optIn(a)
            }
        }
    }
}


