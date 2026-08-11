SHELFPLAYER — SIMPLE GITHUB BUILD

IMPORTANT:
Do NOT upload this ZIP itself to GitHub. Unzip it first.
Do NOT upload WORKFLOW-PASTE-INTO-GITHUB.yml with the app files.

1. Create a blank GitHub repository.
2. Unzip this package on your computer.
3. In the repository choose Add file > Upload files.
4. Upload these THREE items:
      app
      build.gradle
      settings.gradle
   Commit the upload.

5. Open the repository's Actions tab.
6. Choose "set up a workflow yourself" (not Android CLI).
7. Delete everything GitHub puts in the editor.
8. Open WORKFLOW-PASTE-INTO-GITHUB.yml from this package and paste its entire contents.
9. Leave the filename as something ending in .yml, for example build-apk.yml.
10. Click Commit changes.

The build should start automatically.

11. Actions > Build ShelfPlayer APK > newest run.
12. At the bottom under Artifacts, download ShelfPlayer-APK.
13. Unzip it on your Xiaomi and install ShelfPlayer.apk.
