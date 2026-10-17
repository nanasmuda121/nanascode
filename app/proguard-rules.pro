# NasCode Proguard / R8 Optimization Rules

# Keep NasCode Application and Activities
-keep class com.nascode.nanas.** { *; }
-keep class * extends android.app.Application
-keep class * extends android.app.Activity
-keep class * extends androidx.fragment.app.Fragment
-keep class * extends androidx.lifecycle.ViewModel

# Keep Hilt and Dagger Dependency Injection
-keep class * implements dagger.hilt.internal.GeneratedComponent { *; }
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }
-keep @dagger.hilt.android.HiltAndroidApp class * { *; }
-keep @javax.inject.Inject class * { *; }
-keep @javax.inject.Singleton class * { *; }
-dontwarn dagger.hilt.**

# Keep Room Database, DAOs, and Entities
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Dao interface * { *; }
-keep @androidx.room.Entity class * { *; }
-keep class com.nanascode.app.core.database.** { *; }
-dontwarn androidx.room.paging.**

# Keep Kotlinx Serialization & Core Domain Models
-keepattributes *Annotation*, InnerClasses, EnclosingMethod, Signature
-keepclassmembers class * {
    *** Companion;
}
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep @kotlinx.serialization.Serializable class * { *; }
-keepclassmembers @kotlinx.serialization.Serializable class * {
    *** Companion;
}
-keep class com.nanascode.app.core.model.** { *; }

# Keep CodeMirror, Terminal, and Plugin WebView JavaScript Interfaces
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
-keep class com.nanascode.app.feature.editor.EditorJsBridge { *; }
-keep class com.nanascode.app.core.terminal.** { *; }
-keep class com.nanascode.app.feature.editor.plugin.** { *; }
-keep class com.nanascode.app.feature.editor.terminal.** { *; }

# Eclipse JGit & SSH/HTTP Transports
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

# SLF4J Logger
-dontwarn org.slf4j.**
-keep class org.slf4j.** { *; }

# Redact debug logs in release builds to guarantee token privacy
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
}
