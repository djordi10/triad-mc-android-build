# TRIAD Mission Control — Android (native)

A true-native Android app (Kotlin + Jetpack Compose) that is a client of `triad-mcp`, at parity
with the web dashboard **v5.11**. Design: `../docs/MISSION-CONTROL-ANDROID-DESIGN.md`.

**v5.11 facelift.** The app now reads as one system with the web dashboard: a **light paper body
with white cards** (was an all-dark cockpit), the **segmented nav** (OPERATE / ANALYSE / MODEL·LEARN
/ CONTROL) with the current 14-view set and numbering, a per-view **dark TRIAD brand strip** + stance
strip, and the shared component vocabulary (cards, stat tiles, tags, ribbons, laws, PEND boxes). All
fourteen views are built out with their DEMO content — the O/X/C/L/T/D/Q/I/S/C/T/G laws, matching the
wired web pages.

## Build

Requires **JDK 17+** and the **Android SDK (API 34)**. The Gradle wrapper is committed, so no
Gradle install is needed. Point Gradle at your SDK once via `android/local.properties`:

```properties
sdk.dir=/path/to/Android/sdk
```

(or export `ANDROID_HOME`). Then, from `android/`:

```bash
./gradlew :app:testDebugUnitTest    # JVM unit tests — the wall, fixtures, VALIDITY (no SDK build)
./gradlew :app:assembleDebug        # DEMO-mode APK, no network needed
./gradlew :app:installDebug         # onto a connected device/emulator
```

Or just open `android/` in Android Studio (Koala+) and Run.

The `debug` build boots in **DEMO** (BuildConfig.DEFAULT_MODE) with the v4.0 live-shaped
fixtures — fully usable before any deployment. Connect to a live window from the in-app
connection sheet: mode → LIVE, MCP endpoint URL, bearer (stored in the Android Keystore).

## The wall

Reads, replays, proposes. The MCP client exposes exactly `call()` (read-only, with a
mutating-verb denylist), `propose()`, and `recordCheckup()` — there is no code path from a tap
to a venue action. See `mcp/MutatingDenylist.kt` and the design doc §1.

## What is built vs. stubbed

Built: Gradle/manifest/theme, the 14-view adaptive navigation, the MCP client + ktor transport,
`MissionRepository` with a working `DemoRepository`, the **Overview** screen against fixtures,
the connection sheet, the propose drawer, and the adaptive launcher icon. The other twelve views
are placeholder screens following the Overview pattern (a screen + ViewModel per view).
