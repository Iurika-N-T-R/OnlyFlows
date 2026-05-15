# Domain models (Room entities)
-keep class com.onlyflows.domain.model.** { *; }

# ── Glance app widgets ────────────────────────────────────────────────────────
-keep class com.onlyflows.ui.widget.** { *; }
-keep class * implements androidx.glance.appwidget.action.ActionCallback { *; }

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
