# UI rewrite review

The rewrite replaces the complete manager UI with Material 3 Expressive. Reused source files and adaptations are documented in [UI_SOURCES.md](UI_SOURCES.md).

Validation runs on a temporary Pixel 7 emulator (Android API 37, x86_64, 16 KB pages). Twelve metadata-only module fixtures exercise long lists and search. The connected physical device is not used.

## Known-issue coverage

| Original issues | Implementation and evidence |
| --- | --- |
| 1, 15: transparent destinations / welcome predictive back | Each destination has an opaque surface. Review-mode welcome uses the same Miuix navigation transition as secondary pages. Mid-gesture inspection shows an opaque foreground. |
| 2: About title and back button disappear | Copied WeKit's single-title collapsing app bar. Collapsed About keeps both controls. |
| 3, 6: tab animation races / Back exits from non-home tabs | One InstallerX pager controller per pager; Back returns to Home. Rapid Home/Settings switching and Settings Back checked in the emulator. |
| 4, 5, 17: mixed menus, tabs and component systems | Standard M3 DropdownMenuItem, PrimaryTabRow and MaterialExpressiveTheme. COUI removed; no Miuix component/theme dependency. |
| 7, 8: search focus and duplicated result trees | One actual OutlinedTextField and one retained result tree; focus and filtering checked on the device and in instrumentation. |
| 9: settings rebuild and default-value flash | Retained main pages, saveable scroll state and activity-owned loaded theme. Returning from welcome keeps the settings scroll position. |
| 10: dialog exit / predictive-back completion | One retained Material dialog window, dedicated window dispatcher and preserved exit state/data. Theme, installer and DNS dialogs close by Back while retaining Settings. |
| 11, 12: settings title alignment and typography | Background image and opacity use the same copied BaseWidget as adjacent rows. |
| 13, 14: installer selector and input-dialog spacing | Shared radio rows; 24 dp dialog insets; scrolling and input validation. |
| 16: accessibility | Native controls expose input/checked/selected state. Ordinary actions have no Selected property; switch/radio state is exposed once. The core patch action has a tested label, button role, click action and keyboard focus. The M3 icon-plus-text FAB contract requires its label on the icon; both patch and selection-confirm actions provide it. |
| 18: last module obscured by bottom bar | Lists consume measured bottom navigation height. Search lists additionally account for the IME; horizontal display cutouts are handled in lists and search controls. |

The app picker retains its search query through landscape/portrait recreation and returns to patch configuration. Integrated-mode module selection returns both selected names. Override-version/SDK controls, dark theme, 1.5× fonts and final-item clearance were checked on the device.

The patch page uses InstallerX’s `adjustResize` window behavior and IME padding. Its action is hidden while editing to keep the entire field clear, and returns when the keyboard closes.

Additional lifecycle corrections retain app-picker requests, extraction/dialog state, navigator results and snackbar state across activity recreation. After process death, interrupted native patch/picker flows return to a stable destination instead of restoring callbacks that no longer exist.

## Build and checks

- Debug app and instrumentation APK build successfully with the repository's pinned native submodules and tools.
- All 10 instrumentation tests pass (13.248 seconds). Coverage includes rapid pager navigation, ordinary action/menu semantics, one switch/radio state node, app checkbox selection, persistent search focus, retained dialog exit and window Back isolation and the actual patch action’s accessible label, button role and keyboard focus.
- `git diff --check` passes.
- Full Debug/Release Lint is now clean across all Android modules. See [Lint cleanup and localization checks](LINT_REVIEW.md).
