# Proguard rules for ClearOut
# Add project specific ProGuard rules here.
# You can control the set of applied obfuscations in build.gradle.kts using isMinifyEnabled.

# Hilt Specific Proguard rules
-keep class * extends class dagger.hilt.internal.GeneratedComponent { *; }
-keep class * extends class dagger.hilt.internal.GeneratedComponentManager { *; }
-keep class * extends class dagger.hilt.internal.ComponentEntryPoint { *; }
-keep class * extends class dagger.hilt.internal.UnsafeCasts { *; }
