# ShelfPlayer

A small personal Android audiobook player.

## What it does

- Pick your own local audiobook/audio files using Android's system file picker.
- Remembers the playback position separately for every selected file.
- Tap a book later to continue where you stopped.
- Play / pause, skip back 15 seconds, skip forward 30 seconds, and seek.
- Keeps playing in the background through Android Media3's MediaSessionService.
- Does not require broad storage access; it retains access only to files you explicitly select.

## Easiest build path

1. Install the current stable Android Studio.
2. Open this `ShelfPlayer` folder as a project.
3. Let Android Studio install/sync the requested Android SDK components if prompted.
4. Connect your Xiaomi phone by USB with USB debugging enabled.
5. Click **Run** to install directly on the phone.

To make a shareable APK instead, use **Build > Generate App Bundles or APKs > Generate APKs** (wording can vary slightly by Android Studio version). For personal use, the debug APK is enough.

## Build versions

- Android Gradle Plugin: 9.3.0
- Gradle required by AGP 9.3: 9.5.0
- Java/JDK: 17
- compileSdk / targetSdk: 36
- minSdk: 26
- AndroidX Media3: 1.11.0

## Notes for Xiaomi / HyperOS

Android may ask for notification permission so playback controls can appear outside the app. If HyperOS aggressively stops playback after the screen has been off for a while, set ShelfPlayer's battery setting to allow background activity / no restrictions.

## Files supported

The picker accepts `audio/*`; actual decoding support depends on Android/Media3 and the codecs on the device. Common audiobook formats such as MP3, M4A/M4B, AAC, FLAC, OGG and WAV are typical choices.
