# Directory mosaic verification

Expectation: startup selects the remembered existing folder or prompts; folder browsing displays bounded RAW previews off the EDT, preserves individual failures, opens full photographs, and ignores superseded results without interrupting native decoding.

Baseline: the existing 16 tests passed with `env -u WAYLAND_DISPLAY -u XDG_SESSION_TYPE xvfb-run -a mvn -B verify`; the application displayed only a single photograph.

Each feature scenario first ran its new test before implementation. RawDirectory, PreviewRenderer, StartupDirectory and the directory CLI tests failed compilation because their new contracts were absent. DirectoryMosaicTest initially failed because openDirectory was absent. Relevant tests passed after each implementation.

MosaicLifecycleTest exposed a real failure: completion of an obsolete directory announced that the new directory had finished while its decoder remained blocked. DirectoryMosaicTest's early-return scenario timed out because remaining thumbnails never completed. A close scenario first required waiting for tiles to appear, then failed because the disposed frame retained tiles. Each failure was fixed and rerun with the original viewer regressions.

Release expectation: feat increments minor, fix/perf increments patch, breaking headers or footers increment major, and non-product changes do not release. `.github/release_version_test.rb` failed first because the helper was absent; its five tests and seven assertions pass. The current history yields 0.2.0 from the existing v0.1.2 tag. The minimal Ruby helper is needed to classify multiline Conventional Commit messages and calculate the maximum bump without fragile shell parsing; the existing workflow consumes its outputs.

Reproduce: `ruby .github/release_version_test.rb`, then `env -u WAYLAND_DISPLAY -u XDG_SESSION_TYPE xvfb-run -a mvn -B verify`. The suite includes a real LibRaw preview, real CLI photograph rendering, file chooser interaction, image aspect/pixel checks, isolated Preferences, stale work and close during decode.

Final local result: Maven verify passed in 5.403 seconds with 27 tests (2 domain, 24 app, 1 packaged launcher). Ruby version policy passed all 5 tests and 7 assertions. Diff whitespace checks passed; the secret-name scan found only the existing GitHub Actions token reference.

Limits: preview decoding uses full-resolution LibRaw decoding before shrinking; only the resulting bounded BufferedImage is kept per tile. Returning before directory loading completes restarts that directory. Memory and latency with large real-world folders were not benchmarked. GitHub Actions and the Debian release artifact require CI verification after publication; no release or tag was created locally.

Review correction: DirectoryMemoryTest first failed because opening a photo before preview completion left no saved directory, so restart prompted again. Directory acceptance now saves on the EDT after a successful, current listing. The two memory tests pass, including preserving the remembered folder after a missing-folder listing failure. Lifecycle assertions now allow persistence of a directory accepted before it was superseded or closed, while requiring the final remembered directory to match the current selection.

Review navigation correction: DirectoryMosaicTest now shows its frame and asserts the full photo is visible after opening, its tile is hidden, and those visibility states reverse after returning. Removing the photo card transition manually produced a failure at the photo visibility assertion; after restoring it, removing the mosaic return transition produced a failure at the tile visibility assertion. Both transitions were restored explicitly. Final verify passed in 5.538 seconds with 29 Java tests, and the 5 Ruby tests / 7 assertions passed. No personal photo files or application Preferences were used by these review checks.
