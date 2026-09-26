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

The launcher icon is adaptive and carries the complete artwork, wordmark included. A plain bitmap made launchers fall back to drawing their own white plate behind it, which is the white ring the previous icon showed.

- Colours are sampled from the supplied artwork: field `#86B752`, arc `#E7F2D7`, mark `#FFFFFF`, wordmark `#496025`.
- An adaptive mask hides roughly a third of the canvas, and the artwork puts its wordmark at y 89.7-98.4 of 108, well outside that visible circle. The whole lockup is therefore scaled to 0.62 about the artwork's content centre `(54.43, 58.31)` and moved onto the canvas centre, which puts the wordmark's outer corners 34.6 units from the middle, inside the mask's 36-unit radius: nothing is cropped.
- At 48 dp the lettering is about 2.4 dp tall, so it reads as a lockup rather than as text. A twelve-letter wordmark cannot be legible inside a 48 dp circular icon; raising the scale to 0.72 makes it larger but starts cropping the outer letters under a circular mask.
- `mipmap-anydpi-v26/ic_launcher.xml` and `ic_launcher_round.xml` combine `ic_launcher_background` with `ic_launcher_foreground`, and reuse the same foreground for `monochrome`, the Android 13+ themed layer: only its alpha is read, so one asset keeps the two layers identical.
- `ic_launcher_background.xml` is the green field plus the arc, the arc transformed exactly like the lockup so the wordmark still lands on it. After the transform the arc is a circle centred `(53.61, 138.24)` with radius 71.03, cresting at y 67.2.
- `drawable-{m,h,xh,xxh,xxxh}dpi/ic_launcher_foreground.png` are the mark and the wordmark keyed out of the artwork with per-pixel alpha, one 108 dp canvas each. The arc's antialiased edge is deliberately left out: the vector background draws it.
- `mipmap-{m,h,xh,xxh,xxxh}dpi/ic_launcher.png` carry the complete artwork for API levels without adaptive icons, generated from the source artwork at 48-192 px.
- The splash uses `ic_launcher_artwork.png`, a 512 px raster of the same artwork. The vector it replaced still spelled NPatch in its traced wordmark, and a raster reuses the supplier's lettering instead of retracing twelve glyphs as path data.
- `ic_notification.xml` draws the mark on its own 24 dp canvas. Notification small icons are rendered as a system-tinted silhouette, so the full artwork arrived as a single solid block.

## Patched-app page

Tapping a row in Manage -> Apps opens `ui/page/AppDetailScreen.kt` instead of the module picker, because which patcher produced a bundle decides which actions apply. It is assembled from the repository's own widgets (`SegmentedColumn`, `BaseWidget`, `ExpressiveActionDropdown`) rather than modeled on another manager's layout.

- The header keeps the icon and app name on one row and puts the chips on their own row underneath, each with a leading icon, so a wide icon cannot squeeze them and every chip starts at the same left edge as the sections below.
- Those chips name the patcher and, when the bundle is ours, the mode and the loader version. The one naming the patcher takes `patcherTone`, and the two holding the bundle's details take `patcherDetailTone`, one step below it: our own bundles keep the primary family for both, another patcher's name keeps the primary container pair while its details drop to the secondary one.
- The module list is read from the archive under `assets/{shimmerpatch,npatch,lspatch}/modules/`, so a bundle another patcher produced still lists its modules; each entry is resolved against the installed apps for a label and icon.
- Loader and scope rows only appear for our own bundles, and a foreign one gets a note naming its patcher instead.
- Export writes the installed APK set, base plus splits, into a folder the user picks through the system document tree.

## Manage tabs

Manage -> Apps and Manage -> Modules are one pager. Its tabs lead with an icon and carry the section's count on that icon's top corner, the arrangement KernelSU uses for its destinations, rather than widening the label with a number.

- `Icons.Widgets` and `Icons.Extension`: the module icon is the one the module sections already use, and the app icon had to be changed twice - `Icons.Apps` and then `Icons.GridView` both draw the same shape in their filled and outlined variants at 24 dp, so the selected tab looked unselected. Each tab draws the filled variant while it is the current page and the outlined one otherwise, the same contract the bottom bar's destinations already use. They are 24 dp beside a 14 sp label, and take the tab's own content colour, which leaves the selected one primary and the other onSurfaceVariant.
- The app count excludes patcher managers: they appear on the same page in their own group, because they produced patched apps rather than being patched themselves. Each of those rows leads with a badge naming the manager and follows it with the package, both in the same tone rule as the chips, because which tool produced a bundle is what a reader looks for first.
- Both tabs read the view models their bodies already read, so the page still performs one package scan.
- Counts are decoration, so Settings -> Appearance and theme carries a switch for them; the icons stay either way, and a count of zero draws no badge at all.

## Chips and row height

- There are two tiers, and a chip is in one of them. The name of a patcher - the chip on an app row or the badge leading a manager row - takes `patcherTone`: the bright primary pair on this project and the primary container pair on another patcher. Everything the row says about that bundle or tool - its mode, its loader, the manager's package - takes `patcherDetailTone` one step down: the primary container pair on ours and the secondary one on another's. Both decisions live in one file, so a row cannot name itself in one family and carry its details in another.
- `AppItem` keeps its blocks four dp apart and eight dp from the row's edges. Rows are the densest thing the page shows and each one already carries a name, a package and a line of chips, so the padding is what decides whether a list of them reads as a list.

## Module badges

A module row carries two badges, and both used to be painted from the module's kind, so a modern module showed two identical green labels.

- A modern module splits the primary family between its badges: the API version takes the light `primary` tone and the pipeline the darker `primaryContainer` under it. A legacy module keeps the pair it had - the version in `primaryContainer`, the pipeline in `secondaryContainer` - because there the pipeline is the constant and the version is what moves.
- Both badges sit in the row's top corner, level with the module name, the way LSPatch shows them. `AppItem`'s `topRightContent` slot is what places them there; it had been invoked inside the left-aligned block, so the slot's name and its position disagreed. The version and the description keep the left column to themselves, unchanged.
- The name and the package under it are one block with no gap of their own: the label's line box already carries more room below its glyphs than the smaller package line does, so the two read as separated without one.
- The version rides with the package as `package · version` rather than on a line of its own: a module row is about one package, and the two are read together.
- The pipeline badge names the API family the module was written against - `libxposed` or `legacy` - rather than the abstract `Modern`/`Legacy`: the tone already carries the state, and the label says which side of the API split the module sits on.

## Regression checks

`manager/src/androidTest` contains focused tests for ordinary action semantics, single switch/radio state nodes, and native dialog dismissal. Run on an explicitly selected test emulator:

```sh
ANDROID_HOME=/path/to/android-sdk ./gradlew :manager:assembleDebug :manager:assembleDebugAndroidTest
adb -s emulator-5580 install -r manager/build/outputs/apk/debug/manager-debug.apk
adb -s emulator-5580 install -r manager/build/outputs/apk/androidTest/debug/manager-debug-androidTest.apk
adb -s emulator-5580 shell am instrument -w moe.shimmerfly.shimmerpatch.test/androidx.test.runner.AndroidJUnitRunner
```

Runtime review should cover first-run and review-mode welcome, all main tabs, search focus/IME dismissal, selection, menu actions, dialog buttons and predictive back, app shortcuts, custom backgrounds, light/dark themes, large fonts, and last-item clearance beneath both navigation modes.
