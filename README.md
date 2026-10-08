# Helmet Heroes (Android WebView wrapper)

A minimal Android app that opens the official game URL (`https://www.helmet-heroes.com/`) full-screen in a WebView.
The URL lives in one place: `app/src/main/java/com/helmetheroes/mobile/AppConfig.kt`.

## Build the APK with GitHub (no Android Studio needed)

1. Create a new repository on github.com (e.g. `helmet-heroes-apk`).
2. Upload everything in this folder (including the hidden `.github` folder) to the repository.
3. Open the **Actions** tab. The **Build APK** workflow starts automatically (or click it, then **Run workflow**).
4. Wait about 5 minutes for a green check mark.
5. Open the finished run, scroll to **Artifacts**, and download `helmet-heroes-apk`.
6. Unzip it. You get `helmet-heroes.apk` and `SHA256SUMS.txt`.
7. Uninstall any older version first (each build is signed with a different debug key), then copy the APK to your phone, tap it, and allow "Install unknown apps" when asked.
8. Open **Helmet Heroes** from your app drawer.

## What it does
- Loads the game URL, keeps cookies and local storage so logins persist.
- Immersive full-screen, locked to landscape.
- On-screen joystick and buttons: ATK, B, N, M (skills), E (pickup), HEAL (comma key).
- Menu button (top-left): reload, hide/show controls, fit-game toggle, exit.
- Back button navigates back in the page, then exits.
- Retry / Exit screen on network errors, HTTP errors and WebView crashes.

## Known limitations / NOT VERIFIED
- **Not tested.** This project has not been built or run on a device yet.
- **Flash caveat.** The official site refers to Flash and an Adobe AIR desktop app. Flash does not run in Android WebView, so if the game is Flash-based it will not play here, and no wrapper can fix that. The site also lists an official Google Play app, which may be the better option.
- Touch controls send keyboard key presses to the game. Keys: joystick = W A S D, ATK = Space, B N M = skills, E = pickup, HEAL = comma. Change them in `AppConfig.kt`. Whether the game accepts them is NOT VERIFIED.
- The 'fit game to screen' option is best-effort and NOT VERIFIED against the real page. Turn it off from the menu button (top-left) if the screen goes black.
- No settings screen, layout editor or release signing yet. The APK is a debug build, signed with the standard debug key, which is fine for personal installs.

## Legal
This app only loads the public website. It bundles no game assets. Helmet Heroes belongs to its owners.
