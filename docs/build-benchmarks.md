# APK build concurrency benchmark

Measured on two local x86_64 Android emulators, each configured with two virtual CPUs. These measurements establish the benefit on the tested emulators; timings on physical devices will vary.

The `benchmarkSequentialAndParallelApkBuilds` instrumentation test builds four separate apps with 1,500 network filter rules each. It warms class loading, key generation and filesystem caches, then measures three sequential and three two-worker batches in alternating order. Each parallel result is opened and checked for the correct app metadata. Installation is excluded.

| Emulator | Sequential batches (ms) | Two-worker batches (ms) | Median improvement |
| --- | --- | --- | --- |
| Android 15 | 1927, 1906, 1847 | 1062, 1041, 1004 | 45.4% |
| Android 5 | 1595, 1531, 1608 | 1021, 936, 940 | 41.1% |

Run the test on one idle device at a time, without concurrent Gradle builds:

```sh
adb -s emulator-5554 shell am instrument -w \
  -e class io.github.ulysseszh.pwainstaller.DeviceIntegrationTest#benchmarkSequentialAndParallelApkBuilds \
  io.github.ulysseszh.pwainstaller.test/androidx.test.runner.AndroidJUnitRunner
adb -s emulator-5554 logcat -d -s PwaBuildBenchmark:I
```

The installer therefore uses two APK build workers when at least two processors are available, and one otherwise. The bounded pool limits memory pressure and leaves the metadata/UI worker independent. Reinstall-all prepares the next app while the current one builds or awaits installation; package-installer confirmations are submitted one at a time.
