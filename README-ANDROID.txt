WAVE for Android  -  your phone's music, found automatically  (version 2)
==============================================================

NEW IN VERSION 2
- Only real songs: voice messages, WhatsApp audio, call/voice recordings, ringtones,
  alarms, podcasts and audiobooks are left out (songs under 40 seconds are skipped too).
- Spotify-style home: quick tiles, Recently played, Made for you (daily mix), your playlists, your artists.
- New logo and app icon.
- Playlists: big cover page, play button, your own picture (tap the camera on the cover),
  a new create/edit sheet with picture + name, and it opens "Add songs" right after you create one.
- Library tabs (Songs / Playlists / Artists) and new sort chips (Recently added, Title, Artist, Oldest).
- Controls outside the app: previous / play-pause / next in the notification shade and on the lock screen,
  headset and Bluetooth buttons, and music pauses automatically for phone calls.

HOW TO UPDATE (you already have the repository)
1. Unzip this folder. On github.com open your "wave" repository > Add file > Upload files.
2. Drag everything INSIDE the unzipped WaveMusicAndroid folder (Ctrl+A) into the page.
   Files with the same name are replaced; new ones are added.
3. Commit changes, open the Actions tab, wait for the green check, download Wave-APK again.
4. Install the new app-debug.apk over the old one (your playlists are kept).

What it does
- On first launch it asks permission to read music, then lists every song on the phone.
  No importing, no file picker. Songs you add later appear the next time you open the app.
- Plays in the background and with the screen off (a small "Wave" notification shows while playing).
- Playlists, likes and recently played are kept on the phone.
- Album art and colours come from the songs themselves.
- Phone back button: closes Now playing / menus / goes back to Home, then leaves the app.
- Requires Android 8.0 or newer.

HOW TO GET THE APK (pick ONE way)
--------------------------------------------------------------
A) Easiest - no software to install (free GitHub account)
   1. Go to github.com, sign up/in, click "New repository", name it "wave", Create.
   2. Click "uploading an existing file", then drag ALL the files and folders from
      inside this WaveMusicAndroid folder into the page (including the hidden ".github" folder).
      If ".github" does not upload: click "Add file > Create new file", type the name
      .github/workflows/build-apk.yml and paste the text from build-apk.yml.txt.
   3. Click "Commit changes".
   4. Open the "Actions" tab. A job "Build Wave APK" starts; wait ~5 minutes until it is green.
   5. Click the finished run, scroll to "Artifacts", download "Wave-APK" (a zip), unzip it.
   6. Send app-debug.apk to your phone and open it.

B) Android Studio (on a computer)
   1. Install Android Studio, choose File > Open, select this folder, wait for Gradle sync.
   2. Plug in your phone (USB debugging on) and press Run, or use
      Build > Build APK(s) and copy app/build/outputs/apk/debug/app-debug.apk to your phone.

Installing on the phone
- Open app-debug.apk. If Android asks, allow "Install unknown apps" for the app you opened it from.
- Open Wave, tap Allow when asked for access to music and notifications.
- If you pressed Deny: tap "Allow access" inside Wave; it will open the app settings
  where you can switch on Music and audio.

Notes
- The interface is the same index.html as the website version; it lives in
  app/src/main/assets/index.html if you want to edit it.
- Only songs Android has indexed as music are listed (the same list other music apps see).
- This is a debug-signed APK for personal use (not for the Play Store).
