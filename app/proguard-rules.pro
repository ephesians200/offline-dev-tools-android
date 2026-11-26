# BouncyCastle: the app only calls its lightweight crypto API directly (digests, generators, params),
# which R8 can trace statically, so the blanket "-keep class org.bouncycastle.** { *; }" is gone.
# That keeps the APK small. If a release build ever fails inside Argon2 / scrypt / bcrypt / SHA-3 /
# BLAKE2, re-add a narrow keep for just that package, e.g.:
#   -keep class org.bouncycastle.crypto.generators.** { *; }
-dontwarn org.bouncycastle.**
-dontwarn javax.naming.**
