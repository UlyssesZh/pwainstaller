# Cached filter selection benchmark

Measured with the complete AdGuard Android lists `2_optimized.txt`, `18.txt`, and `20.txt`, totaling 5,173,124 bytes. Reddit selects 33,606 rules totaling 797,828 bytes. The instrumentation benchmark compares the complete selected text against the snapshot generated before optimization, and checks that no HTTP requests occur during selection.

Local x86_64 emulator timings (milliseconds; physical-device timings will vary):

| Device | Previous repeated selections | Optimized first selection | Subsequent selections |
| --- | --- | --- | --- |
| Android 15 | 7075, 6795, 5847 | 1324 | 3, 2 |
| Android 5 | 4177, 3054, 3014 | 855 | 1, 0 |

Selection now recognizes cosmetic markers with string comparisons, avoids IDN conversion for ASCII domain patterns, reuses domain matches within a selection, and does not compile ordinary network URL matchers. Raw regular-expression rules are still checked for validity. The same parser validates network options for selection and runtime filtering, so unsupported transformations remain excluded.

The installer retains one host’s selected snapshot. Cache keys include the configured source files, their sizes, and modification times; successful global downloads also invalidate the result. Missing/new source files, changed source settings, and a different host invalidate it. Existing web-app snapshots remain independent.

Run `DeviceIntegrationTest#benchmarkCachedDefaultRulesFromCompleteLists` with `-e benchmarkRuleLists true`. Place the three complete list files and the expected `reddit.txt` snapshot in the installer’s external files directory before running it. Results appear under the `PwaRulesBenchmark` logcat tag and in `rule-benchmark.txt` in the same directory. Lists are local test inputs, not bundled with the app.

## Compiling selected rules

The same 797,828-byte Reddit snapshot produces 1,475,556 bytes of filtering JavaScript and license assets on Android. `benchmarkCompilingCompleteRedditRules` alternates one- and two-worker compilation and verifies every output byte is identical. It also recreates the disk-cache wrapper between requests and checks that cache hits do not read compiler assets. No HTTP requests are needed.

| Emulator | One worker (ms) | Two workers (ms) | First cache population, then cache hits (ms) |
| --- | --- | --- | --- |
| Android 15 | 1138, 781, 785, 620 | 730, 685, 650, 625 | 636, 5, 5, 4 |
| Android 5 | 291, 315, 361, 317 | 261, 291, 308, 291 | 282, 7, 6, 6 |

The first pair includes class loading and JIT warm-up. Two-worker median improvements across these runs were approximately 15% on Android 15 and 8% on Android 5. Measurements are from local x86_64 emulators, not the user’s phone; the Android 5 run overlapped the final part of host-side lint. Cache-population measurements above used the sequential compiler; production now uses two workers for at least 1,024 cosmetic rules when multiple processors are available. Smaller snapshots compile sequentially.

Compilation now avoids constructing matchers for ordinary blocking rules when only document exceptions are needed, indexes cosmetic exceptions by operator and selector, and resolves scriptlet names once. Percentage updates cover parsing, helper preparation, cosmetic compilation, exception processing, and asset generation. The persistent cache uses SHA-256 of the exact input, compiler version, and installer update identity; it checks entry lengths and CRCs and publishes files by atomic rename. Concurrent builds of the same snapshot share one compilation. The cache retains at most 64 entries within 64 MiB.

Run `DeviceIntegrationTest#benchmarkCompilingCompleteRedditRules` with `-e benchmarkRuleLists true` and the same `reddit.txt` input. Results are written to `compilation-benchmark.json` in the installer’s external files directory and the `PwaCompilationBenchmark` logcat tag.
