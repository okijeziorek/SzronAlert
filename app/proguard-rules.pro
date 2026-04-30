# ====== FROSTALERT SECURITY RULES ======

# Keep minimal metadata for crash reports but obfuscate source
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Obfuscate everything except specific keeps
-keepattributes Signature,InnerClasses,EnclosingMethod

# ====== BILLING PROTECTION ======
# Keep public interface but obfuscate internals
-keep interface pl.oki.frostalert.billing.BillingManagerInterface { *; }

# CRITICAL: Obfuscate billing implementation while keeping functionality
-keep,allowobfuscation class pl.oki.frostalert.billing.BillingClientWrapper {
    public <methods>;
}

# ====== SENSITIVE CLASSES ======
# Obfuscate settings and data stores
-keep,allowobfuscation class pl.oki.frostalert.data.local.SettingsDataStore
-keep,allowobfuscation class pl.oki.frostalert.data.local.FrostDatabase

# Obfuscate API clients
-keep,allowobfuscation class pl.oki.frostalert.data.remote.OpenMeteoApi
-keep,allowobfuscation class pl.oki.frostalert.data.repository.SmartHomeRepository

# ====== HIDE DEBUG FUNCTIONALITY ======
# Remove debug screen in release builds
-assumenosideeffects class pl.oki.frostalert.ui.screens.DebugScreenKt {
    *;
}
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# ====== SERIALIZATION & REFLECTION ======
-keepattributes *Annotation*
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
}

# Keep serializable classes structure
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# ====== WORKMANAGER + HILT ======
-keep class * extends androidx.work.ListenableWorker { *; }
-keep class androidx.hilt.work.HiltWorkerFactory { *; }
-keep @androidx.hilt.work.HiltWorker class * { *; }

# ====== COMPOSE ======
# Only keep Kotlin metadata needed by Compose; do not blanket-keep all of androidx.compose.**
-keep class kotlin.Metadata { *; }

# ====== ROOM DATABASE ======
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# ====== GLANCE WIDGET ======
-keep class * extends androidx.glance.appwidget.GlanceAppWidget { *; }
-keep class * extends androidx.glance.appwidget.GlanceAppWidgetReceiver { *; }

# ====== KTOR ======
# Ktor and coroutines ship their own consumer ProGuard rules; no blanket keeps needed here.
# Only suppress warnings for internal classes that may be stripped.
-dontwarn io.ktor.**
-dontwarn kotlinx.coroutines.**

# ====== REMOVE METADATA ======
-keepattributes !SourceDebugExtension
