# Lint cleanup

Runtime hook targets, API dispatch, authentication, file modes and module preference paths retain their original implementations. The attempted `PackageManagerHooks` / package-manager compatibility / preferences-path helpers were removed. Hidden/private platform API checks use suppression declarations, following WeKit's approach, rather than replacing the framework integration.

The remaining cleanup consists of resource translations and formatting, equivalent Kotlin/Java syntax and type declarations, unused dependencies/resources, and Gradle metadata. Existing exported components retain their exported status. Loader resource shrinking and asset-producer ordering are declared in Gradle; language splitting is disabled so the existing in-app locale selection has every translation offline.

## Localization

- 44 locale directories cover all 241 translatable resources, including regional/default-language inheritance.
- Missing translations were generated from English; existing translations were retained, with targeted corrections where their meaning or technical names were outdated.
- Simplified/traditional Chinese, German, French and Japanese received additional review. Plural forms and DEX terminology were checked separately, including Slavic, Lithuanian, Arabic and Hebrew forms.
- Package names, platform API identifiers, URLs and format arguments are checked by `python3 scripts/check_translations.py`.
- There is no MissingTranslation baseline or category-wide translation suppression.

## Reproduction

```sh
python3 scripts/check_translations.py
./gradlew lint :manager:lintRelease :meta-loader:lintRelease \
  :patch-loader:lintRelease :remote-api:lintRelease :share:android:lintRelease
./gradlew :manager:assembleDebug :manager:assembleDebugAndroidTest :apkzlib:test
```

The standalone Gradle settings and wrapper were updated in the `remote-api` submodule; the parent repository records the corresponding submodule commit.

Gradle/AGP deprecation notices are separate from Android Lint findings.
