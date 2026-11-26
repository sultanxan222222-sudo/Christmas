# Ramadan Dream Launcher

Native Android launcher prototype for Android phones and Android TV.

## Current foundation
- Emerald night background with Arabic Allah calligraphy, falling snow, mosque silhouette, illuminated minaret, crescent and selectable kitten colour.
- Four selectable clock styles (visual variants will be expanded in the next iteration).
- Grozny prayer timetable integration using the Chechen Republic Spiritual Administration (DUM CHR) timetable API, timezone Europe/Moscow (UTC+3).
- CC0 Adhan audio packaged from Wikimedia Commons.
- Automatic adhan scheduling and manual test playback.
- Phone and TV APK flavours. No firmware, system partitions, or stock launcher are modified.

## Build
GitHub Actions builds both APK variants on pushes to `ramadan-islamic-launcher`. Download artifacts named `Ramadan-Dream-Launcher-phone` and `Ramadan-Dream-Launcher-TV`.

## Important validation
This is the first implementation build. Verify the API response and prayer schedule against the official DUM CHR timetable and local mosque. Android TV background process / alarm behavior varies by firmware and must be tested on the target TCL/Union TV. Alarm scheduling uses Android's inexact-while-idle API to avoid requiring the user to grant special exact-alarm access.


Latest integration checkpoint: the adhan playback service is declared as a foreground service and the alarm is restored after reboot. The phone and TV variants are built from the current branch head.

Build checkpoint: duplicate imports detected in CI have been removed from the current source.

Package identity: phone APK uses com.ramadan.dream.launcher; TV APK uses com.ramadan.dream.launcher.tv so it installs separately from the earlier Christmas launcher.
