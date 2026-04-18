# Domain models (Room entities + serialized payloads)
-keep class com.onlyflows.domain.model.** { *; }
-keep class com.onlyflows.data.sync.** { *; }

# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class ** {
    @kotlinx.serialization.Serializable <methods>;
}
-keep,includedescriptorclasses class com.onlyflows.**$$serializer { *; }
-keepclassmembers class com.onlyflows.** {
    *** Companion;
}
-keepclasseswithmembers class com.onlyflows.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Ktor — ships its own consumer rules but ensure reflection targets survive
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**
# SLF4J impl is optional; Ktor uses the API but the static binder is absent at runtime
-dontwarn org.slf4j.impl.StaticLoggerBinder

# Glance app widgets — system instantiates receivers by name via reflection
-keep class com.onlyflows.ui.widgets.** { *; }

# WorkManager workers
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.CoroutineWorker
-keepclassmembers class * extends androidx.work.Worker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# Room — ships consumer rules; this guards against edge cases
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.**

# Geofencing / Google Play Services
-dontwarn com.google.android.gms.**
