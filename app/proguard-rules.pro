# Domain models (Room entities)
-keep class com.onlyflows.domain.model.** { *; }

# ── WorkManager workers ───────────────────────────────────────────────────────
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.CoroutineWorker
-keepclassmembers class * extends androidx.work.Worker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# ── Room ──────────────────────────────────────────────────────────────────────
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.**

# ── Geofencing / Google Play Services ────────────────────────────────────────
-dontwarn com.google.android.gms.**
