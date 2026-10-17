# Code optimization and shrinking rules
-allowaccessmodification

# Keep data models used for network serialization
-keep class com.codershubinc.aaxion_mob.core.model.** { *; }

# Keep fields annotated for serialization
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
    @com.google.gson.annotations.Expose <fields>;
}

# Keep reflection attributes for networking
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations

# Retrofit API interface members
-keepclassmembers,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# Suppress known harmless warnings for third-party libraries
-dontwarn okhttp3.**
-dontwarn retrofit2.**
-dontwarn kotlinx.coroutines.**
-dontwarn coil.**
