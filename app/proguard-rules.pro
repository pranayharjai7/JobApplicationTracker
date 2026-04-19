# Suppression rules for missing optional dependencies
-dontwarn org.ietf.jgss.**
-dontwarn org.apache.http.**
-dontwarn javax.annotation.**
-dontwarn com.google.j2objc.annotations.**
-dontwarn java.lang.instrument.**
-dontwarn com.google.common.flogger.**
-dontwarn javax.lang.model.**

# Ignore all warnings if needed (for libraries with missing optional deps)
-ignorewarnings

# Hilt / Dagger rules
-keepattributes *Annotation*
-keepattributes Signature
-keep class dagger.hilt.internal.GeneratedComponentManager { *; }
-keep @dagger.hilt.android.HiltAndroidApp class *
-keep @dagger.hilt.android.scopes.* class *
-keep class * implements dagger.hilt.internal.GeneratedComponent { *; }
-keep class * extends com.google.dagger.hilt.android.internal.managers.HiltWrapper_ { *; }

# Room rules
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep class * { @androidx.room.PrimaryKey *; }

# Gson / Google API Model preservation
-keep class com.google.api.services.gmail.model.** { *; }
-keep class com.google.api.client.** { *; }
-keep class com.google.api.services.gmail.** { *; }
-keep class com.google.api.client.googleapis.** { *; }
-keep class com.google.api.client.json.gson.** { *; }
-keep class com.pranay.jobtracker.data.** { *; }
-keepclassmembers class * {
  @com.google.gson.annotations.SerializedName <fields>;
}

# AI Provider Rules (Moshi/Gson reflections)
-keep class com.pranay.jobtracker.domain.ai.** { *; }

# Retain Coroutine debug info (optional)
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.coroutines.android.HandlerContext {
    long offset;
}
