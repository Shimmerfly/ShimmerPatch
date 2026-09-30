# Zip and signing engines

The patcher writes APKs through one of two engines, and the manager carries both:

- **apkzlib**, the in-tree module at `/apkzlib`, which patching has always used;
- **NeoApk**, whose sources live in the `NeoApk/` submodule, which upstream patching moved to.

Which one runs is a setting (`Patch engine` under the patch group of the settings screen) rather
than a build choice, because patching runs inside the manager process. `Patcher` reads the
preference and calls the matching patcher class; nothing else in the tree knows the difference.

## Why the sources are built here

Upstream consumes NeoApk as `top.nkbe:NeoApk:1.0.1` from JitPack, and substitutes a sibling
`../NeoApk` checkout through `includeBuild` when one is present. Neither works here:

- that coordinate has never been published, so a dependency on it cannot resolve;
- building NeoApk's own Gradle build needs a Kotlin plugin version this checkout cannot download,
  and its published repository belongs to the upstream author rather than to this project.

So `:zipengines:neoapk` compiles the submodule's sources as part of this build, through a plain
`kotlin.jvm` plugin applied by id (AGP already puts Kotlin on the classpath, and asking for a
second, versioned copy of it is refused).

## Updating NeoApk

The submodule points at this repository's `neoapk` branch, which mirrors `HSSkyBoy/NeoApk`, in the
same way `core` and `remote-api` mirror their upstreams. To move it forward:

```sh
git -C NeoApk fetch origin main         # or the fork the sources come from
git -C NeoApk push <this repo> origin/main:neoapk
git add NeoApk                          # record the new commit
```
