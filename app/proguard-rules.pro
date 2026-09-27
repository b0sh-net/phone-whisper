# --- Kotlin & Compose (already in proguard-android-optimize.txt, kept here for clarity) ---
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod
-keep class kotlin.** { *; }
-keep class kotlinx.** { *; }
-keep class androidx.compose.** { *; }

# --- sherpa-onnx (JNI + AAR) ---
# Keep all JNI-native methods and their declaring classes
-keepclassmembers class * {
    native <methods>;
}
-keep class org.kengigahei.sherpaonnx.** { *; }
-keep class com.k2fsa.sherpa.onnx.** { *; }
-keep class ai.onnxruntime.** { *; }

# Keep classes referenced via reflection by sherpa-onnx runtime
-dontwarn com.k2fsa.**
-dontwarn org.kengigahei.**
-dontwarn ai.onnxruntime.**

# --- OkHttp / OkHttpClient ---
-keepattributes Signature
-keepattributes Annotations
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
}
-dontwarn okio.**
-dontwarn okhttp3.**

# --- Apache Commons Compress ---
-dontwarn org.apache.commons.compress.**
-keep class org.apache.commons.compress.** { *; }

# --- MediaCodec / Audio decoding ---
-dontwarn android.media.MediaCodecList

# --- Remaining warnings (suppress non-critical ones) ---
-dontwarn javax.annotation.**
-dontwarn sun.misc.**
