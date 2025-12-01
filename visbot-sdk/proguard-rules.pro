# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Keep Master service related classes (for reflection)
-keep class com.ubtrobot.master.** { *; }
-keep class com.ubtrobot.servo.** { *; }
-keep class com.ubtrobot.locomotion.** { *; }
-keep class com.ubtrobot.transport.** { *; }

# Keep SDK public API classes
-keep class com.visbot.sdk.** { *; }

# Keep Gson classes
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

