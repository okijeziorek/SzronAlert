# Closed Test Build Configuration

## Overview

This project now includes a special `closedTest` build variant designed for closed testing on Google Play. This variant complies with AdMob policies by using official Google test ad unit IDs instead of production ads.

## What's Different in the Closed Test Build?

### 1. Test Ad Units
The `closedTest` variant uses Google's official test AdMob IDs:
- **App ID**: `ca-app-pub-3940256099942544~3347511713`
- **Banner IDs**: `ca-app-pub-3940256099942544/6300978111`
- **Native Ad ID**: `ca-app-pub-3940256099942544/2247696110`

These test IDs ensure:
- ✅ No risk of violating AdMob's policy against clicking your own ads
- ✅ Safe testing with real ad SDK integration
- ✅ No invalid traffic reported to your AdMob account
- ✅ Testers can interact with ads without consequences

### 2. Unique Package Name
The closed test build has a different application ID:
- **Production**: `pl.oki.frostalert`
- **Closed Test**: `pl.oki.frostalert.closedtest`

This allows both versions to be installed simultaneously on the same device.

### 3. Version Suffix
The version name includes `-closedtest` suffix (e.g., `1.3-closedtest`) to easily identify test builds.

### 4. Release-like Configuration
The closed test build uses the same optimizations as release:
- Minification enabled (R8)
- Resource shrinking enabled
- ProGuard rules applied
- Same signing configuration as release

## Building the Closed Test Variant

### Using Gradle

```bash
# Build the APK
./gradlew assembleClosedTest

# Build the App Bundle for Play Store
./gradlew bundleClosedTest

# Install directly to a connected device
./gradlew installClosedTest
```

### Using Android Studio

1. Open **Build Variants** panel (View → Tool Windows → Build Variants)
2. Select **closedTest** from the dropdown
3. Build → Build Bundle(s) / APK(s) → Build APK or Build Bundle

## Output Locations

- **APK**: `app/build/outputs/apk/closedTest/app-closedTest.apk`
- **Bundle**: `app/build/outputs/bundle/closedTest/app-closedTest.aab`

## Uploading to Google Play Console

1. Build the bundle:
   ```bash
   ./gradlew bundleClosedTest
   ```

2. Go to [Google Play Console](https://play.google.com/console)

3. Navigate to: Testing → Closed testing

4. Create or select a closed test track

5. Upload the bundle: `app/build/outputs/bundle/closedTest/app-closedTest.aab`

6. Add your testers via email or create a testing list

## Important Notes

⚠️ **Do NOT use the closedTest build for production release!**

The closedTest variant should only be used for:
- Internal testing
- Closed beta testing on Play Store
- Pre-release validation

For production releases, always use the `release` build variant which contains your real AdMob production IDs.

## Source Code Structure

```
app/src/
├── closedTest/
│   └── res/
│       └── values/
│           └── admob.xml          # Test AdMob IDs
├── debug/
│   └── res/
│       └── values/
│           └── admob.xml          # Test AdMob IDs (same as closedTest)
└── release/
    └── res/
        └── values/
            └── admob.xml          # Production AdMob IDs
```

## Testing Ads in the Closed Test Build

When testing:
1. The ads displayed will be marked as "Test ads"
2. Clicking on test ads is safe and won't affect your AdMob account
3. Test ads may show generic content or Google sample ads
4. Impressions and clicks won't be counted in AdMob reporting

## Switching Back to Production

To build for production:
```bash
# Production release build
./gradlew assembleRelease
./gradlew bundleRelease
```

## Troubleshooting

### "Duplicate package" error when installing
This means you already have another variant installed. Uninstall it first:
```bash
adb uninstall pl.oki.frostalert.closedtest
# or
adb uninstall pl.oki.frostalert
```

### Ads not showing
- Check internet connection
- Verify the test IDs are correctly configured
- Check logcat for AdMob initialization messages
- Test ads may take a moment to load

### Build fails
- Clean the project: `./gradlew clean`
- Sync Gradle files
- Check that all AdMob dependencies are up to date

## AdMob Policy Compliance

This configuration ensures compliance with [AdMob's publisher policies](https://support.google.com/admob/answer/6128543):

> Publishers may not click their own ads or use any means to inflate impressions and/or clicks artificially, including manual methods.

By using test ad units during closed testing, you avoid any risk of:
- Invalid traffic
- Account suspension
- Policy violations
