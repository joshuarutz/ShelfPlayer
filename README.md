# ShelfPlayer

A small personal Android audiobook player for your own local audio files.

## Features

- Pick your own audiobook/audio files using Android's system file picker.
- Remembers playback position separately for every selected file.
- Tap a book later to continue where you stopped.
- Play/pause, skip back 15 seconds, skip forward 30 seconds, and seek.
- Keeps playing in the background through Android Media3's MediaSessionService.
- No broad storage permission: access is retained only for files you explicitly select.

# Easiest way to get the APK: GitHub cloud build

You do **not** need Android Studio, Java, the Android SDK, USB debugging, or a cable to your phone.

## One-time setup

1. Go to GitHub and create a free account if you do not already have one.
2. Create a new repository. A name such as `ShelfPlayer` is fine.
3. Leave the repository empty when creating it (do not add a README or .gitignore on GitHub).
4. On your computer, unzip this project ZIP.
5. Open the `ShelfPlayer` folder inside it.
6. On the empty GitHub repository page choose **uploading an existing file** / **Add file > Upload files**.
7. Drag the **contents** of the `ShelfPlayer` folder into the upload area. Make sure `.github`, `app`, `build.gradle`, `settings.gradle`, `.gitignore`, and `README.md` are included.
8. Commit/upload the files to the `main` branch.

GitHub Actions should start the cloud build automatically after the upload.

## Download the APK on your Xiaomi

1. Open your repository on GitHub.
2. Tap/click **Actions**.
3. Open the newest **Build ShelfPlayer APK** run.
4. Wait until the build shows a green check mark.
5. Under **Artifacts**, download **ShelfPlayer-APK**.
6. GitHub downloads the artifact as a small ZIP. Extract it on your phone.
7. Inside is **ShelfPlayer.apk**. Tap it.
8. If Android/HyperOS asks, allow your browser or Files app to **Install unknown apps**.
9. Tap **Install**.

After that ShelfPlayer appears like a normal app. You can delete the downloaded ZIP/APK after installation if you want.

## Build again after future changes

Any time project files are committed to `main`, GitHub builds a new APK automatically. You can also open **Actions > Build ShelfPlayer APK > Run workflow** to trigger a fresh build manually.

## If GitHub says Actions are disabled

Open the repository's **Actions** tab and enable workflows for the repository, then run **Build ShelfPlayer APK** again.

## Technical versions

- Android Gradle Plugin: 9.3.0
- Gradle: 9.5
- Java/JDK: 17
- compileSdk / targetSdk: 36
- minSdk: 26
- AndroidX Media3: 1.11.0

The included `.github/workflows/build-apk.yml` installs the required SDK components, builds `:app:assembleDebug`, renames the output to `ShelfPlayer.apk`, and uploads it as a GitHub Actions artifact.

## Xiaomi / HyperOS note

Android may ask for notification permission so playback controls can appear outside the app. If HyperOS stops playback after the screen has been off for a while, change ShelfPlayer's battery setting to allow background activity / no restrictions.

## Audio files

The picker accepts `audio/*`. Actual decoding support depends on Android/Media3 and the codecs on the device. MP3, M4A/M4B, AAC, FLAC, OGG, and WAV are common choices.
