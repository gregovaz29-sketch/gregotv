# jcifs-ng uses bouncycastle/reflection; keep to be safe (release only)
-dontwarn org.bouncycastle.**
-dontwarn org.slf4j.**
-keep class jcifs.** { *; }
-keep class org.jetbrains.kotlinx.serialization.** { *; }
