# NanasCode Proguard Rules

# Keep Hilt and Dagger
-keep class * extends android.app.Application
-keep class * extends android.app.Activity
-keep class * extends androidx.fragment.app.Fragment

# Keep Room
-keep class * extends androidx.room.RoomDatabase

# Keep Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses, EnclosingMethod
-keepclassmembers class * {
    *** Companion;
}
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}

# Redact all Log.d and Log.v in release builds to guarantee token privacy
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
}

# Eclipse JGit
-dontwarn org.eclipse.jgit.**
-keep class org.eclipse.jgit.** { *; }

# Java SE runtime classes referenced by JGit not present in Android SDK
-dontwarn java.lang.ProcessHandle
-dontwarn java.lang.management.**
-dontwarn javax.management.**
-dontwarn org.ietf.jgss.**
-dontwarn java.awt.**
-dontwarn javax.naming.**
-dontwarn javax.security.auth.**

# SLF4J
-dontwarn org.slf4j.**
