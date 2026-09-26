# Material 3 Expressive UI

The manager UI components follow WeKit and InstallerX-Revived. Device-information icon choices additionally follow KernelSU as requested. Business operations remain ShimmerPatch's. The copied components retain their upstream copyright and license headers; adaptations use the repository's GPL license.

Reference working copies used on 2026-09-12:

- WeKit: `650febaa3717eddd39f5f29a4dfa6dfd37875cb1`
- InstallerX-Revived: `f6ffcd8ea84e629bc14160414e7343f07a4d3d76`

Paths in the following table are relative to each reference project's `app/src/main/java`. Destination paths are under `manager/src/main/java/moe/shimmerfly/shimmerpatch/ui`.

| Source | Destination / adaptation |
| --- | --- |
| WeKit `dev/ujhhgtg/wekit/ui/content/m3/ExpressiveCollapsingTopAppBar.kt` | `component/ExpressiveCollapsingTopAppBar.kt`; complete title layout, measurement, drag, typography interpolation and heading semantics copied. |
| WeKit `ui/content/m3/ExpressiveBackButton.kt` | `component/ExpressiveBackButton.kt`; ShimmerPatch's existing Material arrow and localized back description. |
| WeKit `ui/content/m3/{SegmentedColumn,BaseWidget,BaseItemContainer,SwitchWidget,RadioButtonWidget,M3Shape}.kt` | `component/m3/`; complete grouped item shapes, animations and widgets. Ordinary actions do not expose selection; switches and radio rows have one accessible state. |
| WeKit `ui/content/m3/LazySegmentedItems.kt` | `component/m3/LazySegmentedItems.kt`; bounded language, DNS and installer lists share dynamic group corners and spacing. |
| WeKit `ui/content/m3/DropDownMenuWidget.kt`, InstallerX `ui/page/main/widget/menu/GroupedDropdownMenuPopup.kt` | `component/m3/`; shared expressive selection and action menus, group/item shapes and leading icons. |
| WeKit `ui/content/WeKitBasicDialog.kt` | `component/m3/SettingsDialog.kt`; surface, 24 dp spacing and scrollable content. Dialog windows handle their own dismissal and platform transition. |
| WeKit `ui/content/M3Blur.kt` | `component/M3Blur.kt`; complete M3 color/blend and backdrop helpers copied. The reference tint is composited on the regular canvas to keep the title legible when a capture frame is unavailable. |
| WeKit `ui/content/FloatingBottomBar.kt`, `ui/content/{animation,liquid}/*.kt`, `ui/content/DragGestureInspector.kt` | `component/`; complete floating navigation, gestures, highlights and blur implementation copied, including keyboard and accessibility actions. |
| WeKit `ui/navigation/M3NavEffects.kt`, `ui/utils/CornerRadiusUtil.kt` | `page/M3NavEffects.kt`, `util/CornerRadiusUtil.kt`; device corner clipping, dimming and surface colors. |
| WeKit `activity/settings/SettingsActivity.kt` | `activity/MainActivity.kt`; Miuix NavDisplay, persisted typed back stack and per-entry lifecycle/viewmodel ownership. |
| InstallerX `com/rosan/installer/ui/activity/SettingsActivity.kt` and `res/values*/themes.xml` | `activity/MainActivity.kt` and manager launch themes; keep the system splash until the themed UI is composed, using AndroidX SplashScreen for API 28+ compatibility. |
| WeKit `ui/utils/theme/{ModuleAppTheme,SeedResolver}.kt` | `theme/Theme.kt`; MaterialExpressiveTheme, expressive motion and MaterialKolor seed generation. |
| InstallerX `com/rosan/installer/ui/navigation/PagerState.kt` | `page/MainPagerState.kt`; complete cancelable navigation controller shared by Main and Manage. |
| InstallerX `ui/page/main/settings/home/HomePage.kt` | `page/HomeScreen.kt`; status panel, copied StatCard and grouped device information. |
| KernelSU `manager/app/src/main/java/me/weishu/kernelsu/ui/screen/home/HomeMaterial.kt` | `page/HomeScreen.kt`; Material leading-icon choices for the six device/framework information rows. |
| InstallerX `ui/page/main/settings/preferred/about/AboutPage.kt` | `page/AboutScreen.kt`; collapsing heading, grouped links and app information. |
| InstallerX `ui/theme/Shape.kt` | `component/m3/AppItemShapes.kt`; complete first/middle/last/single list shapes copied. |
| InstallerX `ui/page/main/widget/setting/NavigationItemWidget.kt` | `page/SettingsScreen.kt`; shared settings action / chevron layout. |
| WeKit `ui/agent/settings/PromptsScreen.kt` | `page/ManageScreen.kt`; PrimaryTabRow and one retained pager. |
| InstallerX `ui/page/main/settings/config/apply/{ApplyPage,ApplyItemWidget}.kt` | `component/{SearchBar,AppItem}.kt`; real text input and app selection rows. |
| InstallerX `ui/page/main/installer/dialog/inner/{InstallingDialog,PreparingDialog}.kt` / Material 3 pull-to-refresh | `page/newpatch/DoPatchBody.kt`, `component/{LoadingDialog,ShimmerPatchPullToRefresh}.kt`; wavy progress and expressive loading indicators. |

## Design-system boundary

All text, buttons, cards, selection controls, tabs, menus, preferences and dialogs are Material 3. Material 3 is explicitly pinned to `1.5.0-alpha28`, matching WeKit, because the expressive APIs are not supplied by the BOM's stable Material version.

Direct Miuix dependencies are restricted to `miuix-nav-android`, `miuix-blur-android` and the shader module needed by blur. No Miuix UI, preference, icon or theme component library is used. COUI, Haze and the former Kyant backdrop dependency are removed.

## State and layout rules

- Each destination draws an opaque base and its own optional wallpaper before content. Navigation transitions cannot expose another destination through the foreground surface.
- One controller owns each PagerState. Clicks, shortcuts and Back submit a target; settled swipes report their result. Main keeps all three page compositions alive.
- Refresh containers wrap the app-bar nested-scroll connection, so list overscroll first expands the title and only the remaining distance reaches pull-to-refresh.
- DataStore theme values are loaded once at the activity boundary. Preferences consume the loaded state, without rendering placeholder defaults on tab entry.
- Cold startup keeps the system splash until the first themed composition is ready. The platform owns its exit transition; no placeholder frame or custom launch animation is inserted.
- The splash drawable insets the complete launcher artwork by one sixth, so the mark and the ShimmerPatch wordmark stay inside the system icon mask.
- Scaffold measures the bottom navigation. Pages receive its actual height as scrollable end padding, keeping content behind blur while allowing the final item to scroll fully above navigation.
- Process recreation returns interrupted native patch/picker flows to a stable destination; configuration changes preserve their live state and pending result channels.
- Search uses one real input and one result tree. There is no fake input, IME-height focus reset, duplicate pager, or full-page visibility switch.
- InstallerX’s `adjustResize` activity behavior and patch-page IME padding keep inline editing from panning the entire destination.
- Dialog dismissal and transitions are owned by the native dialog window, with no custom fade, scale or predictive-back transform.
- Custom DNS and installer rows use separate edit and radio actions. Their shared editor validates before returning a value, reports errors on the text field, and discards cancelled edits; the parent dialog commits the selection.

## Launcher icon

The launcher icon is adaptive. A plain bitmap made launchers fall back to drawing their own white plate behind it, which is the white ring the previous icon showed.

- Colours are sampled from the supplied artwork: field `#86B752`, arc `#E7F2D7`, mark `#FFFFFF`, wordmark `#496025`.
- `mipmap-anydpi-v26/ic_launcher.xml` and `ic_launcher_round.xml` combine `ic_launcher_background` with `ic_launcher_foreground`, and add `ic_launcher_monochrome` for Android 13+ themed icons.
- The mark is centred in the canvas and scaled to 0.94 about its own centre, so its arm tips reach `(53.6 / 2) * sqrt(2) * 0.94 = 35.6` from the centre, inside the guaranteed-visible circle of radius 36: no mask can clip them.
- `mipmap-{m,h,xh,xxh,xxxh}dpi/ic_launcher.png` carry the complete artwork, wordmark included, for API levels without adaptive icons, generated from the source artwork at 48-192 px.
- The splash uses `ic_launcher_artwork.png`, a 512 px raster of the same artwork. The vector it replaced still spelled NPatch in its traced wordmark, and a raster reuses the supplier's lettering instead of retracing twelve glyphs as path data.
- `ic_notification.xml` draws the mark on its own 24 dp canvas. Notification small icons are rendered as a system-tinted silhouette, so the full artwork arrived as a single solid block.

## Regression checks

`manager/src/androidTest` contains focused tests for ordinary action semantics, single switch/radio state nodes, and native dialog dismissal. Run on an explicitly selected test emulator:

```sh
ANDROID_HOME=/path/to/android-sdk ./gradlew :manager:assembleDebug :manager:assembleDebugAndroidTest
adb -s emulator-5580 install -r manager/build/outputs/apk/debug/manager-debug.apk
adb -s emulator-5580 install -r manager/build/outputs/apk/androidTest/debug/manager-debug-androidTest.apk
adb -s emulator-5580 shell am instrument -w moe.shimmerfly.shimmerpatch.test/androidx.test.runner.AndroidJUnitRunner
```

Runtime review should cover first-run and review-mode welcome, all main tabs, search focus/IME dismissal, selection, menu actions, dialog buttons and predictive back, app shortcuts, custom backgrounds, light/dark themes, large fonts, and last-item clearance beneath both navigation modes.
