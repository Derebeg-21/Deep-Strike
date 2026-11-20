# ===================================================================
# HexaFleet R8 / ProGuard Production Rules
# ===================================================================

# -------------------------------------------------------------------
# Kotlinx Serialization Rules
# -------------------------------------------------------------------
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.SerializationKt

# Keep class members annotated with @SerialName
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
}

# Keep the companion object serializer instance
-keepclassmembers class **$$serializer {
    public static final ** INSTANCE;
}

# Keep classes annotated with @Serializable
-keepclasseswithmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}
-keepclasseswithmembers class * {
    @kotlinx.serialization.Serializable class *;
}

# Keep custom serializers
-keepclassmembers class * extends kotlinx.serialization.KSerializer {
    <init>(...);
}

# -------------------------------------------------------------------
# AndroidX Room SQLite & DAOs
# -------------------------------------------------------------------
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**
-keep class com.hexafleet.data.local.HexaFleetDatabase_Impl { *; }
-keepclassmembers class * extends androidx.room.RoomDatabase {
    <init>();
}
-keepclassmembers class * {
    @androidx.room.Dao *;
}
-keepclassmembers class * {
    @androidx.room.Entity *;
}

# -------------------------------------------------------------------
# HexaFleet Domain, Engine & Multiplayer Models
# -------------------------------------------------------------------
-keep class com.hexafleet.core.hexmath.** { *; }
-keep class com.hexafleet.domain.game.model.** { *; }
-keep class com.hexafleet.domain.game.multiplayer.** { *; }
-keep class com.hexafleet.data.local.entity.** { *; }
-keep class com.hexafleet.data.multiplayer.model.** { *; }

# -------------------------------------------------------------------
# Kotlin Coroutines
# -------------------------------------------------------------------
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-dontwarn kotlinx.coroutines.**

# -------------------------------------------------------------------
# Jetpack Compose & Vector Graphics
# -------------------------------------------------------------------
-keep class androidx.compose.ui.** { *; }
-dontwarn androidx.compose.**

# -------------------------------------------------------------------
# HexaFleet AI & Anti-Cheat Heuristic Engine (Obfuscation & Optimization)
# -------------------------------------------------------------------
# Yapay zeka taktiksel H-PDF matrisleri ve olasılık hesaplayıcıları karartılır (obfuscated)
-repackageclasses 'com.hexafleet.internal'
-allowaccessmodification
-dontwarn com.hexafleet.domain.game.ai.**
