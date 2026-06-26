# ARUUU ProGuard rules

# ── Kotlin ────────────────────────────────────────────────────────────────
-keep class kotlin.** { *; }
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**

# ── Hilt / Dagger ─────────────────────────────────────────────────────────
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }
-keepclasseswithmembernames class * {
    @javax.inject.Inject <init>(...);
}

# ── Room ──────────────────────────────────────────────────────────────────
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *
-keepclassmembers class * extends androidx.room.RoomDatabase {
    abstract *;
}

# ── Gson / serialization ──────────────────────────────────────────────────
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# ── ARUUU domain models ──────────────────────────────────────────────────
-keep class com.aruuu.app.domain.model.** { *; }
-keep class com.aruuu.app.data.local.*Entity { *; }

# ── Biometric ─────────────────────────────────────────────────────────────
-keep class androidx.biometric.** { *; }

# ── CameraX ───────────────────────────────────────────────────────────────
-keep class androidx.camera.** { *; }

# ── Security crypto ───────────────────────────────────────────────────────
-keep class androidx.security.crypto.** { *; }

# ── Compose ───────────────────────────────────────────────────────────────
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# ── DataStore ─────────────────────────────────────────────────────────────
-keep class androidx.datastore.** { *; }

# ── General Android ───────────────────────────────────────────────────────
-keepattributes SourceFile,LineNumberTable
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.app.Application
-keep public class * extends android.accessibilityservice.AccessibilityService

# ── Fix R8 missing classes ────────────────────────────────────────────────
-dontwarn com.google.errorprone.annotations.**

# ── Remove logging in release ─────────────────────────────────────────────
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int d(...);
}
