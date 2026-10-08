# Desktop scale verification

Expectation: on Linux/X11 without an explicit scale, Xft.dpi 192 selects scale 2 before Swing starts; 288 selects 3, while normal or unusable DPI preserves Java's automatic scale. DesktopScale owns that policy and the bounded xrdb query; Main delegates before constructing the decoder or accessing Swing. No tidy first was needed. Naming follows Intention Revealing Selector (#4).

Baseline: the user observed a small UI on a desktop with Xft.dpi 192 and confirmed that an explicit Java scale 2 corrected it. Main previously performed no desktop-scale setup. The first DesktopScale tests failed because the contract was absent; tests for overrides then failed because the resource source was incorrectly queried, and malformed DPI handling failed. Checked source failures initially failed compilation because the source contract did not support them. All scenarios passed after their corresponding changes.

Unit checks cover integer scales, normal DPI, fractional DPI, exact Xft.dpi keys, whitespace, missing or malformed values, NaN/infinity, DPI bounds, explicit property/environment overrides, platform/DISPLAY guards, source errors/timeouts and preserved interruption. The source is injectable; these tests create no AWT windows and do not run graphical integration.

Manual source check: `xrdb -query` returned `Xft.dpi: 192`; `dpkg-query -S /usr/bin/xrdb` identified x11-xserver-utils. Calling only `DesktopScale.configure()` in JShell with app/target/classes, without starting Main or AWT, printed `desktop scale=2`. The Debian workflow now declares and verifies that package dependency; no package was built locally.

Reproduce the permitted unit suite with `mvn -B -pl app -am -Dtest=DesktopScaleTest,RgbImageTest,RawDirectoryTest,PreviewRendererTest,StartupDirectoryTest,ProcessedBitmapTest -Dsurefire.failIfNoSpecifiedTests=false test`. Build the JAR without executing tests using `mvn -B -DskipTests package`. Version-policy tests use `ruby .github/release_version_test.rb`; the existing release helper calculates 0.2.1 from v0.2.0 for these fix commits.

Final result: 18 permitted unit tests passed in 2.034 seconds, including 8 DesktopScale tests. JAR packaging with skipped tests passed in 1.360 seconds. Ruby passed 5 tests and 7 assertions; diff whitespace and secret-name checks were clean.

Limits: no Robot, Xvfb, graphical tests or verify were run locally for this slice. No live monitor switching is implemented. Missing xrdb, failed queries and a 750 ms timeout retain automatic scaling; oversized resource output can also reach that timeout. The real subprocess failure/timeout paths were inspected and are represented by fake source failures in tests. The Debian artifact remains for CI verification. The startup source check did not open a window, so final rendered size remains for the user's manual check.
