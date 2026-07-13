# kotlinx.serialization — keep the generated serializers.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class agentic.triad.missioncontrol.** {
    *** Companion;
}
-keepclasseswithmembers class agentic.triad.missioncontrol.** {
    kotlinx.serialization.KSerializer serializer(...);
}
# ktor / okhttp
-dontwarn org.slf4j.**
-keep class io.ktor.** { *; }
