# Ramadan Dream Launcher

Native Android launcher for Android phones and Android TV.

Current development version: **1.3.0**  
Package IDs: `com.ramadan.dream.launcher` (phone) and `com.ramadan.dream.launcher.tv` (TV).

## Main Ramadan scene

The home screen is being rebuilt around a large interactive mosque, rather than a row of settings and every installed app.

- A central emerald mosque with two tall minarets and a large dome.
- Mosque windows and minaret lantern rooms remain dark outside the prayer-light window. At the beginning of each scheduled prayer, the interior windows and minarets illuminate with a warm golden glow. The animation uses the Grozny prayer timetable; the decorative glow remains active for 20 minutes after the scheduled start.
- Green hanging Ramadan pennants with gold lettering sway gently in the wind.
- A crescent and star sit above the minaret.
- Two brass floor lanterns have animated flames.
- A small kitten rests under a wide, low-branched tree and moves subtly: breathing, blinking, and swaying its tail.
- The tree artwork is an original vector interpretation inspired by the broad crown and spreading trunk of Jordan's Al-Buqayawiyya / Blessed Tree. The site's association with the Prophet's childhood journey is a remembered historical tradition; the launcher does not claim that the surviving tree's identity is independently proven. See the [Royal Hashemite Court announcement](https://rhc.jo/en/news/king-inaugurates-visitor-centre-site-blessed-tree-safawi?language=en) and [Jordan News Agency report](https://www.petra.gov.jo/ar/index.php/en/news/awqaf-minister-highlights-historical-significance-of-al-buqayawiyya%C2%A0tree-1).
- Pressing the mosque opens the prayer timetable.

## Home-screen controls

- The circular gear in the header opens Android TV system settings.
- The three-dot control beside the kitten opens hidden launcher options: prayer and audio settings, Hijri calendar, clock style, kitten choice, and media/app sections.
- The home screen displays only app shortcuts selected by the user.
- Add or remove shortcuts through the tile manager. Removing a tile only removes its shortcut from this launcher; it never uninstalls the app.
- Other installed apps remain available through the hidden Apps section.

## Prayer and audio

- Grozny prayer timetable integration using the DUM CHR timetable endpoint, with the `Europe/Moscow` time zone (UTC+3).
- CC0 Adhan audio packaged from Wikimedia Commons.
- Automatic Adhan scheduling, manual test playback, and playback restoration after reboot.
- Optional low-volume background nasheed with an ON/OFF control.

## Build

GitHub Actions builds both APK variants on pushes to `ramadan-islamic-launcher`. Download artifacts named `Ramadan-Dream-Launcher-phone` and `Ramadan-Dream-Launcher-TV`.

## Important validation

This branch is source code and build configuration; an APK from the latest visual redesign still needs a successful CI build before it should be treated as a finished release. Prayer times should be compared with the official DUM CHR timetable and the user's local mosque. Android TV background alarms and video/audio output vary by firmware and must be checked on the target TCL/Union TV. The prayer-light animation runs while the launcher screen is active; it does not wake the television.
