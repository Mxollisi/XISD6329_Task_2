# Debug vs release builds
* **Debug APK** (`assembleDebug`): debug-signed, shows Developer tools. For testing/demos.
* **Release**: create a keystore locally (`keytool -genkeypair -v -keystore giant-release.jks -alias giant -keyalg RSA -keysize 2048 -validity 10000`). **Never commit it.**
  Provide `storeFile/storePassword/keyAlias/keyPassword` through a git-ignored `keystore.properties` or CI secrets, add a `signingConfigs.release` block that reads them, then `./gradlew assembleRelease` (APK) or `bundleRelease` (AAB).
  Register the release SHA-1/SHA-256 in Firebase for Google sign-in. Release builds hide Developer tools and never seed private fictional data.
