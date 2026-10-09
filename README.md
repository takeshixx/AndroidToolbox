# Android Toolbox

A diagnostic and ADB-enablement toolbox app for Android devices: inspecting what blocks USB ADB,
toggling the relevant settings, and exporting a report.

<img src="images/androidtoolbox-v2.png" width="300">

## Building

Needs JDK 17 and Android SDK platform 33 (licenses accepted, e.g. via Android Studio).

If your default `java` isn't 17 (check `java -version`), point Gradle at one explicitly rather than
relying on `JAVA_HOME` already being set system-wide — on macOS with Homebrew's `openjdk@17`:

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 17) ./gradlew assembleSystemDebug assemblePlainDebug
```

This produces `app/build/outputs/apk/{system,plain}/debug/app-*-debug.apk`, Gradle-debug-signed —
not yet installable as a system/priv-app, see signing below.

### Signing for `system`/priv-app install

The `system` flavor (`sharedUserId=android.uid.system`) needs the AOSP `platform` test key, not the
debug key Gradle applies by default. Get the key pair and sign with `apksigner` from the SDK
build-tools:

```bash
# Key pair: the public AOSP test key, from the official AOSP source (not secret, not
# device-specific — this is the well-known engineering/test-keys signing key).
git clone -n --depth 1 --filter=blob:none https://github.com/aosp-mirror/platform_build.git /tmp/aosp-security
git -C /tmp/aosp-security sparse-checkout set target/product/security
git -C /tmp/aosp-security checkout
cp /tmp/aosp-security/target/product/security/platform.{pk8,x509.pem} .

APKSIGNER=$(find ~/Library/Android/sdk/build-tools -maxdepth 2 -name apksigner | sort -V | tail -1)
"$APKSIGNER" sign --key platform.pk8 --cert platform.x509.pem \
  --out out/AndroidToolbox-system-platform.apk \
  app/build/outputs/apk/system/debug/app-system-debug.apk

# Verify you actually got the platform key before shipping it to a device:
"$APKSIGNER" verify --print-certs out/AndroidToolbox-system-platform.apk
# expect: SHA-256 digest c8a2e9bccf597c2fb6dc66bee293fc13f2fc47ec77bc6b2b0d52c11f51192ab8

rm platform.pk8 platform.x509.pem  # don't leave key material lying around in the repo
```
