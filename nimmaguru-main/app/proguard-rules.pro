# Nimma Guru ProGuard Rules

# Hilt generated classes (P-BUILD-04)
-keep class dagger.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$ViewWithFragmentComponentBuilderEntryPoint { *; }
-keepclassmembers class * {
    @dagger.hilt.* <methods>;
    @javax.inject.* <fields>;
}

# Kotlin serialization annotations & generated companions used by @Serializable
# routes (Routes.kt) and any future JSON DTOs.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keep,includedescriptorclasses class com.nimmaguru.app.**$$serializer { *; }
-keepclassmembers class com.nimmaguru.app.** {
    *** Companion;
}
-keepclasseswithmembers class com.nimmaguru.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Firebase
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }

# Firestore POJO deserialization needs default-arg constructors and field names
-keep class com.nimmaguru.app.core.model.** { *; }
-keepclassmembers class com.nimmaguru.app.core.model.** {
    <init>();
    *;
}

# Coroutines reflection (for Flow, etc.) — narrow rule
-keepclassmembernames class kotlinx.** { volatile <fields>; }
