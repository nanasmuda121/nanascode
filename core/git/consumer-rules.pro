# JGit ProGuard Rules
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
