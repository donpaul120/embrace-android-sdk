plugins {
    id("embrace-prod-android-conventions")
}

description = "Embrace Android SDK: View Instrumentation"

android {
    namespace = "io.embrace.android.embracesdk.instrumentation.view"
}

dependencies {
    implementation(project(":embrace-android-instrumentation-api"))
    compileOnly(project(":embrace-android-api"))
    compileOnly("androidx.fragment:fragment-ktx:1.8.8")

    testImplementation(project(":embrace-android-instrumentation-api-fakes"))
    testImplementation(libs.robolectric)
}
