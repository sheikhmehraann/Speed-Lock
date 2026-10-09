# Speed Lock — Unified Android Root Management & Diagnostic Application

**Package:** `io.speedlock.app`  
**Location:** `speedlock-app/`  
**Classification:** Defensive Android Application, Hardware Diagnostic Suite & Decoupled Backend Engine  
**Target Hardware:** Infinix GT 20 Pro (`X6871`), MediaTek Dimensity 8200 Ultimate (`MT6896` / `MT6895`)  
**Target Kernel:** Linux 5.10.237 GKI 2.0 (`5.10.237-android12-9-00014-gf82f7360927e-ab14119954`)  
**Automated Test Status:** 48/48 Passing (100% Success Rate across 30 Python + 18 Java tests)  

---

## 1. Project Overview & Architecture

`speedlock-app` is the unified Android project for the Speed Lock research workspace. It unifies independent root research backends (`DFRoot`, `GhostLock`, `DirtyInit`, `UniRoot`) behind an honest, decoupled diagnostic abstraction without merging or modifying upstream exploit source trees.

```text
speedlock-app/
├── README.md                          # Application handbook and build reference
├── build.gradle.kts                   # Root Gradle build script
├── settings.gradle.kts                # Project module settings
├── gradle.properties                  # JVM parameters and AndroidX flags
├── gradlew / gradlew.bat              # Gradle wrapper executables (Gradle 8.8)
├── gradle/wrapper/                    # Gradle wrapper configuration & JAR
└── app/
    ├── build.gradle.kts               # App module build script (API 31-34, signingConfigs)
    └── src/
        ├── main/
        │   ├── AndroidManifest.xml    # Android Application Manifest
        │   ├── res/                   # Layouts, colors, themes, strings
        │   └── java/io/speedlock/app/
        │       ├── SpeedLockApp.java          # Global application context singleton
        │       ├── model/                     # Core data contracts
        │       │   ├── BackendState.java      # Honest lifecycle state enum
        │       │   ├── DeviceProfile.java     # Hardware, OS, and kernel specifications
        │       │   └── BackendCapability.java # Evaluated backend capabilities
        │       ├── detector/                  # Hardware & OS detection
        │       │   ├── DeviceDetector.java    # Live /proc, getprop, and firmware reader
        │       │   └── SocClassifier.java     # MT6895 BSP vs MT6896 commercial resolver
        │       ├── backend/                   # Decoupled backend integrations
        │       │   ├── IRootBackend.java      # Abstract interface contract
        │       │   ├── BackendRegistry.java   # Central registry & discovery
        │       │   ├── DFRootBackend.java     # CVE-2024-50302 DirtyFrag adapter
        │       │   ├── GhostLockBackend.java  # CVE-2024-49854 IonStack adapter
        │       │   ├── DirtyInitBackend.java  # Init socket stream relay adapter
        │       │   └── UniRootBackend.java    # Universal manager adapter
        │       ├── diagnostic/                # Compatibility & logging
        │       │   ├── CompatibilityEngine.java # Multi-rule compatibility validator
        │       │   ├── DiagnosticLogger.java  # Thread-safe event logging
        │       │   └── ReportExporter.java    # JSON & Markdown report exporter
        │       ├── ui/                        # Presentation controllers & renderers
        │       │   ├── MainActivity.java      # 6-screen navigation controller
        │       │   ├── DeviceInfoView.java    # Device information screen
        │       │   ├── BackendCatalogueView.java # Backend catalogue screen
        │       │   ├── CompatibilityView.java # 5-tier compatibility centre screen
        │       │   ├── DiagnosticLogsView.java # Filterable diagnostic logs screen
        │       │   └── SettingsView.java      # Theme, motion, and privacy preferences
        │       └── cli/
        │           └── SpeedLockCli.java      # Standalone CLI entrypoint
        └── test/
            └── java/io/speedlock/app/         # 18-test standalone unit testbench
```

---

## 2. Six Core Functional Screens

Speed Lock implements six cohesive screens inspired by Windows 11 Fluent Design (layered depth, rounded acrylic surfaces), iOS typography and translucency, and Material 3 / KernelSU structure:

1. **Home Dashboard ([`MainActivity.java`](file:///c:/Users/Admin/Videos/Github/Speed%20Lock/speedlock-app/app/src/main/java/io/speedlock/app/ui/MainActivity.java)):**
   - Hero header with real-time hardware status: Infinix GT 20 Pro (`X6871`), MediaTek Dimensity 8200 Ultimate, Linux Kernel 5.10.237 GKI.
   - Evaluated compatibility badge (`METADATA_COMPATIBLE`).
   - One-touch routing to all five secondary diagnostic surfaces.

2. **Device Information Screen ([`DeviceInfoView.java`](file:///c:/Users/Admin/Videos/Github/Speed%20Lock/speedlock-app/app/src/main/java/io/speedlock/app/ui/DeviceInfoView.java)):**
   - Displays Brand (`Infinix`), Model (`X6871`), Marketing Name (`Infinix GT 20 Pro`).
   - Resolves and explains the **MT6895 BSP Platform vs MT6896 Commercial SoC** distinction.
   - Displays Kernel Release (`5.10.237-android12-9-00014-gf82f7360927e-ab14119954`), Architecture (`arm64-v8a`), Page Size (`4096 bytes`), and Security Patch (`2026-07-01`).

3. **Backend Catalogue Screen ([`BackendCatalogueView.java`](file:///c:/Users/Admin/Videos/Github/Speed%20Lock/speedlock-app/app/src/main/java/io/speedlock/app/ui/BackendCatalogueView.java)):**
   - Catalogues all integrated backends (`DFRoot`, `GhostLock`, `DirtyInit`, `UniRoot`).
   - Details target CVEs, operations supported, required components, and current operational states.

4. **Compatibility Centre Screen ([`CompatibilityView.java`](file:///c:/Users/Admin/Videos/Github/Speed%20Lock/speedlock-app/app/src/main/java/io/speedlock/app/ui/CompatibilityView.java)):**
   - Enforces a transparent **Five-Tier Verification Architecture**:
     - **Tier 1 (Source Audit):** Audited repository trees, Kbuild scripts, and open-source licenses.
     - **Tier 2 (Metadata Compatibility):** Validates kernel version, config flags (`CONFIG_XFRM=y`, `CONFIG_INET_ESP=y`, `CONFIG_FUTEX=y`, `CONFIG_MODULE_SIG is not set`), and candidate vendor libraries (`/vendor/lib64/libbinderdebug.so`).
     - **Tier 3 (ABI Verification):** Validates ARM64-v8a ELF structure, symbol CRCs, and struct layouts.
     - **Tier 4 (Build Verification):** Validates toolchain readiness and Android DDK / NDK compilation pipelines.
     - **Tier 5 (Device Hardware Validation):** Evaluates physical device execution status; explicitly marked **UNVERIFIED** without physical connected hardware.

5. **Structured Diagnostic Logs Screen ([`DiagnosticLogsView.java`](file:///c:/Users/Admin/Videos/Github/Speed%20Lock/speedlock-app/app/src/main/java/io/speedlock/app/ui/DiagnosticLogsView.java)):**
   - Chronological, thread-safe diagnostic event trace.
   - Interactive log level filtering (`ALL`, `INFO`, `WARN`, `ERROR`, `DEBUG`).
   - Case-insensitive keyword query search.
   - Export confirmation display with target path, entry count, and cryptographic SHA-256 verification hash.

6. **Settings & Preferences Screen ([`SettingsView.java`](file:///c:/Users/Admin/Videos/Github/Speed%20Lock/speedlock-app/app/src/main/java/io/speedlock/app/ui/SettingsView.java)):**
   - **Theme Mode:** Follow System, Fluent Light Acrylic, or Mica Dark (Default).
   - **Dynamic Color:** Toggle Material You / Monet extraction.
   - **Reduced Motion:** Accessibility toggle for static rendering vs fluid animations.
   - **Strict Privacy Mode:** 100% Local execution with zero telemetry and protected app-private sandbox cache.

---

## 3. MediaTek MT6895 vs MT6896 Silicon Resolution

A key finding in the X6871 ROM forensics resolved the dual designation in the firmware:
- **Base Silicon Platform (BSP):** MediaTek **MT6895** is the internal silicon platform family code used across MediaTek's BSP (shared across the Dimensity 8100/8200 series, e.g. kernel drivers `pinctrl-mt6895.ko`, `clk-dbg-mt6895.ko`, `mdp_drv_mt6895.ko`, and `MT6895_Android_scatter.xml`).
- **Commercial Marketing Part:** MediaTek **MT6896** is the commercial SKU for the enhanced 4nm Dimensity 8200 / Dimensity 8200 Ultimate used in the Infinix GT 20 Pro.
- The application's [`SocClassifier.java`](file:///c:/Users/Admin/Videos/Github/Speed%20Lock/speedlock-app/app/src/main/java/io/speedlock/app/detector/SocClassifier.java) transparently presents both designations to prevent diagnostic confusion.

---

## 4. Honest Capability Lifecycle States

The application enforces six explicit states:
- **`UNAVAILABLE`:** Backend source files or modules are missing from the installation.
- **`UNSUPPORTED`:** Hardware, SoC architecture, or kernel configuration is fundamentally incompatible.
- **`UNTESTED`:** Device or kernel artifacts have not been verified.
- **`METADATA_COMPATIBLE`:** Kernel configuration, candidate libraries, and partition maps match.
- **`BUILD_VERIFIED`:** Module binary passes deep ELF ABI verification (matching vermagic & symbol CRCs).
- **`DEVICE_VERIFIED`:** Authorized physical device execution has succeeded on hardware.

---

## 5. Build and Execution Instructions

### A. Standalone Java Build & Testbench (Zero External Dependencies)
Compile all application sources and execute the 18-test testbench using Microsoft OpenJDK 17:

```powershell
# 1. Compile
New-Item -ItemType Directory -Force -Path "build\classes"
$sources = (Get-ChildItem -Path "app\src\main\java", "app\src\test\java" -Filter "*.java" -Recurse).FullName
javac -d "build\classes" $sources

# 2. Run Testbench (18 Tests)
java -cp "build\classes" io.speedlock.app.SpeedLockTestRunner

# 3. Run Standalone CLI on X6871 Firmware
java -cp "build\classes" io.speedlock.app.cli.SpeedLockCli --firmware "..\research\firmware\X6871" --export-json "build\x6871_report.json" --export-md "build\x6871_report.md"
```

### B. Python Integration Testbench (30 Tests)
Execute the complete cross-platform Python regression suite:

```powershell
python ..\integration\x6871\tests\run_all_tests.py
```

### C. Local Android APK Assembly
If Android SDK Platform 34 and Build-Tools 34.0.0 are installed locally:
1. Set `ANDROID_HOME=C:\Users\<user>\AppData\Local\Android\Sdk` (or your SDK path).
2. Run:
   ```bash
   ./gradlew assembleDebug
   ```
   or for release:
   ```bash
   ./gradlew assembleRelease
   ```
   Artifacts are output to `app/build/outputs/apk/debug/app-debug.apk` and `app/build/outputs/apk/release/app-release.apk`.

> **Notice on Local Toolchain:** The local Windows host environment has Microsoft OpenJDK 17 installed and ready, but lacks Android SDK platforms and build tools. To eliminate manual SDK setup requirements and guarantee clean, reproducible builds, the project includes fully automated GitHub Actions CI/CD workflows that assemble, verify, and package the APK on clean cloud runners.

---

## 6. GitHub Actions CI/CD Pipeline

The project includes two reproducible GitHub Actions workflows located in `.github/workflows/`:

### A. Continuous Integration: `android-ci.yml`
- **Triggers:** Push to `main`, `master`, or `develop`; Pull Requests; manual `workflow_dispatch`.
- **Pipeline Steps:**
  1. Checks out repository code.
  2. Sets up Python 3.11 and runs the 30-test regression suite (`integration/x6871/tests/run_all_tests.py`).
  3. Sets up JDK 17 (Temurin) and runs the 18-test Java testbench (`SpeedLockTestRunner`).
  4. Sets up Android SDK (`android-actions/setup-android@v3` with API 34).
  5. Caches Gradle dependencies with `gradle/actions/setup-gradle@v4`.
  6. Assembles the debug APK (`./gradlew assembleDebug`).
  7. Verifies artifact existence and computes SHA-256 checksum.
  8. Publishes GitHub Step Summary and uploads artifacts (`app-debug.apk` and `.sha256`).

### B. Release Automation: `android-release.yml`
- **Triggers:** Git tag push matching `v*` (e.g. `v1.0.0`) or manual `workflow_dispatch`.
- **Pipeline Steps:**
  1. Runs all 48 automated regression tests (Python + Java).
  2. Configures keystore signing if GitHub Secrets are provided.
  3. Assembles release APK (`./gradlew assembleRelease`).
  4. Generates standard release artifact: `SpeedLock-<version>-x6871.apk`.
  5. Calculates SHA-256 checksum.
  6. Automatically formats detailed release notes documenting device profile, SoC classification, and backend audit status.
  7. Creates a published GitHub Release using `softprops/action-gh-release@v2` with the APK and checksum attached.

---

## 7. Keystore Configuration & Release Signing Secrets

To produce cryptographically signed release APKs in GitHub Actions, configure the following repository secrets under **Settings > Secrets and variables > Actions**:

| Secret Name | Description | Example / Format |
|---|---|---|
| `ANDROID_KEYSTORE_BASE64` | Base64-encoded Java Keystore (`.jks`) | Base64 string |
| `KEYSTORE_PASSWORD` | Password for the keystore file | Secret password |
| `KEY_ALIAS` | Alias of the signing key entry | `speedlock` |
| `KEY_PASSWORD` | Password for the private key | Secret password |

### Keystore Generation Guide
To generate a release signing key locally:

```bash
# 1. Generate Keystore
keytool -genkeypair -v \
  -keystore speedlock-release.jks \
  -alias speedlock \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -storepass YOUR_KEYSTORE_PASSWORD \
  -keypass YOUR_KEY_PASSWORD \
  -dname "CN=SpeedLock, OU=Security, O=Research, L=Global, ST=None, C=US"

# 2. Base64 Encode for GitHub Secrets
# On Windows (PowerShell):
[Convert]::ToBase64String([IO.File]::ReadAllBytes("speedlock-release.jks")) | Set-Clipboard

# On Linux / macOS:
base64 -w 0 speedlock-release.jks
```

> **Fallback Behavior:** If no signing secrets are configured in GitHub Secrets, the release workflow automatically falls back to an unsigned release artifact (`SpeedLock-<tag>-x6871.apk`), ensuring the pipeline never breaks or fails unexpectedly.

---

## 8. Release Procedure

To publish a new official Speed Lock release:

1. Ensure all local tests pass:
   ```powershell
   java -cp "build\classes" io.speedlock.app.SpeedLockTestRunner
   python ..\integration\x6871\tests\run_all_tests.py
   ```
2. Commit all staged updates to the repository:
   ```bash
   git add .
   git commit -m "chore: prepare release v1.0.0"
   ```
3. Tag the release commit and push to remote:
   ```bash
   git tag v1.0.0
   git push origin v1.0.0
   ```
4. GitHub Actions will automatically trigger `android-release.yml`, execute all 48 test verification steps, build the APK, compute SHA-256 checksums, and publish the GitHub Release with binary assets attached.
