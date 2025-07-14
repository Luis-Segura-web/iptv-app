// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    // Definimos los plugins usando el alias del catálogo de versiones (libs)
    // 'apply false' significa que el plugin está disponible para los módulos, pero no se aplica aquí.
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.ksp) apply false
}
