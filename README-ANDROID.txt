WAVE for Android  -  your phone's music, found automatically  (version 2)
==============================================================

NEW IN VERSION 10
- Cards (Recently played, playlists, artists) can no longer grow because of a long song title:
  every card and cover is the same size.
- Artists: "ala", "A.L.A", "ala - Topic" and "ALA Topic" are now ONE artist; "ktyb" and "ktybon" are one artist.
- Collaborations: a guest who only appears on that one song is not made an artist
  (samara, baya -> only samara if samara has other songs; if all artists only have that song, only the first one is kept).
  The song still shows all credits and is found by searching any of the names.

NEW IN VERSION 9
- Time slider: you can now press and DRAG it (before, the playing song kept pulling it back).
- Smart artists: names that differ only by case, accents or a swapped / missing letter become one artist
  (young rz = YONUG rz); songs by two artists ("ala , ktyb", "ala/ktyb", "A feat. B") appear under BOTH;
  a song with no artist but "ala - song name" in its title goes to artist "ala".
  Turn it off or rename an artist (artist page > Rename) if it ever guesses wrong.
- All covers are the same square size; a song with no cover borrows one from another song by the same artist.
- Up next shows just the next 2 songs in nice cards, "Show all" opens the rest.
- Dragging a song: a green slot and a number badge show exactly where it will land.
- Smoother: long lists load in chunks, lighter glass effects, sliding highlight on the bottom bar.

NEW IN VERSION 8
- FIX (real cause found): the app did not know the size of your songs, so it ignored "jump to this time"
  requests and sent the song from the start, which froze playback after you moved the slider.
  The app now reads the real file size, and the page also loads each song fully before playing.
- Edit mode: drag a song by its handle (the dots on the right) or press and hold a song, then drag it.
  The list makes room as you drag and scrolls by itself near the edges. Number tap and arrows still work.
- New playlist design: glass header card with pills, and the songs in one glass panel with thin separators.

NEW IN VERSION 7
- FIX: songs no longer freeze after you drag the time slider around. Each song is now loaded fully
  before it plays, the slider only jumps when you let go, and a frozen song restarts itself.
- Liquid glass (iPhone style) on buttons, bars, song rows and playlists, over one fixed background colour.
- Playlist rows show the song picture next to the number again.
- "Edit" button on playlists: tap a number to move a song (swap places or insert there),
  or use the arrows. Press Done when finished.
- Playlist pictures are changed only from "Edit name and picture" (the separate camera / change / remove buttons are gone).

NEW IN VERSION 6
- FIX: songs no longer stop right after they start (removed a conflicting audio-focus request
  and added a safety net that restarts a song if it is paused by accident in its first seconds).
- New home: avatar + your name (set it in Appearance), round search / heart buttons, filter chips,
  big curated cards (Wave Mix, New in your library...), Top playlists with play buttons.
- Floating pill navigation with a glowing circle on the active tab.
- Playing a song: player bar with the cover fading in and a white rounded pause button;
  playlist page in album style (cover + title, wide Play / Shuffle buttons, equalizer on the playing row).
- Full-screen player: glowing slider, glass buttons that press in, pulsing play button,
  cover that moves with the beat, and an "add to playlist" button.
- Add songs sheet: filter by length (under 2 min, 2-4 min, over 4 min) and by artist,
  then "Add all" for exactly what you see. Creating a playlist asks if you want to choose songs now.
- Artist, Wave Mix and Liked pages have a "save as playlist" button.
- Buttons ripple, rotate and bounce; stronger glass with bright edges.

NEW IN VERSION 5 - SMOOTHER AND GLASSIER
- Stronger glass theme: frosted bars, glowing colour blobs that slowly drift behind everything.
- Swipe down on the full-screen player and it tucks into the bottom bar. Swipe up on the bar to open it.
- Playlist page: the big cover shrinks and fades while you scroll, then a slim bar with the
  playlist name and a play button takes its place. Back button on every playlist/artist page.
- Animations everywhere: pages slide in, lists appear one by one, hearts pop, menus and sheets glide.
- Trending (Tunisia / worldwide) removed. Discover now only searches what you type.

NEW IN VERSION 4 - NEW LOOK
- Spotify-style design: black background, green accent, round green play buttons,
  white progress bars that turn green when touched, play button on cards.
- Glass theme (default): frosted-glass player, menus, tabs and cards over a colour glow
  that follows the cover of the song playing.
- Appearance settings (the sliders icon at the top): switch Glass / Classic dark and pick an
  accent colour (green, cyan, blue, purple, pink, orange, or "Song" to follow the cover).
- New green logo and app icon.

NEW IN VERSION 3 - DISCOVER (needs internet)
- New "Discover" tab: search any song by name, listen, like it, or add it to a playlist.
- Two sources: "Hits" (30-second previews of popular songs) and "Free full songs"
  (complete tracks by independent artists on Audius).
- Full commercial songs cannot be streamed for free outside the big music apps, so for those the
  menu has "Find full song on YouTube", which opens YouTube on that song.
- Online songs you add to a playlist stay in it after you restart the app.

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
