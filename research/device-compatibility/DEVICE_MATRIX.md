# Speed Lock — Device & Platform Compatibility Matrix

This matrix compiles verified and documented target platforms across the repositories archived in Speed Lock.

---

## 1. Master Compatibility Table

| Manufacturer | Device / Family | SoC Architecture | Android / OS Version | Kernel Baseline | Compatible Repositories | Exploitation Mechanism |
|---|---|---|---|---|---|---|
| **Samsung** | Galaxy A17 (SM-A175F) | MediaTek Helio G99 | Android 16 (OneUI) | GKI 6.12 | `ghostlock-a17` | GhostLock UMH root daemon (`g4d`) |
| **Samsung** | Galaxy S24 / S23 / A55 | Snapdragon 8 Gen 3 / Exynos 2400 | Android 14 / 15 (OneUI 6.1 / 7) | GKI 6.1, 6.6 | `GhostLock-Galaxy`, `GhostSam` | GhostLock kernel offset injection |
| **Samsung** | Galaxy S26 (m1q) | Snapdragon 8 Gen 4 / Exynos 2500 | Android 16 (OneUI 8.5) | GKI 6.6, 6.12 | `DFReroot`, `DirtyFrag-Galaxy` | DirtyFrag second-stage persistence |
| **Samsung** | Galaxy Note 20 Ultra to S26 Ultra | Exynos & Snapdragon (Universal) | Android 11 - 16 (OneUI 3 - 8) | Linux 4.19 - 6.6 | `DirtyInit` | `IpSecManager` + `libbase.so` init hijack |
| **Samsung** | Galaxy S21 through S25 | Exynos & Snapdragon | Android 12 - 15 | 5.10, 5.15, 6.1 | `Root-My-Galaxy`, `Root-My-Galaxy-Payloads` | Automated AP/boot patching |
| **Samsung** | Galaxy A52s | Snapdragon 778G | Android 14 | 5.10 | `pixel-ksu-root` | GhostLock runtime KSU injection |
| **Google** | Pixel 10 | Google Tensor G5 | Android 17 (Initial official) | GKI 6.6, 6.12 | `lspromise` | Telecom logic 0-day + Kernel 1-day |
| **Google** | Pixel 6 through Pixel 9 Pro | Google Tensor G1, G2, G3, G4 | Android 13 - 15 | 5.10, 5.15, 6.1 | `Root-My-Pixel`, `Root-My-Pixel-Payloads` | APatch / Magisk boot image integration |
| **Google** | Pixel 7 / 8 / 9 | Google Tensor G2, G3, G4 | Android 14, 15 | 5.15, 6.1 | `pixel-ksu-root` | Stock locked bootloader KSU loader |
| **OnePlus** | OnePlus 11, 12, Open, Ace 3/Pro | Snapdragon 8 Gen 2 / 8 Gen 3 | ColorOS / OxygenOS 14 - 16 | GKI 6.1, 6.6 | `ghostlock-oneplus` | UMH / ksud injection |
| **Xiaomi** | Xiaomi 17 Pro Max (`popsicle`) | Snapdragon 8 Gen 4 / 8 Elite | HyperOS 2.0 (Android 16) | GKI 6.12.23-android16 | `CVE-2026-43499-popsicle` | Disassembly target offset generator |
| **Xiaomi** | POCO M6 Pro (`emerald`) | MediaTek Helio G99 Ultra | HyperOS (Android 14) | 5.10, 5.15 | `ghostlock-emerald` | GhostLock kernel offset profile |
| **Nothing** | Nothing Phone (1) [`spacewar`] | Snapdragon 778G+ | Nothing OS 2.5 (Android 14) | Linux 5.4 / 5.10 | `root-my-nothing` | GhostLock per-boot runtime root |
| **Nothing** | Nothing Phone (2), (2a), (3a) | Snapdragon 8+ Gen 1, Dimensity | Nothing OS 2.6 - 3.0 (Android 15-17) | 5.15, 6.1, 6.6 | `Root-My-Device` | KernelSU submodule injection |
| **Meizu** | Meizu 21, Meizu 21 Pro | Snapdragon 8 Gen 3 | Flyme 10.5 (Android 14) | GKI 6.1 | `meizu21-ghostlock-root` | Flyme bootstrap script + offsets |
| **vivo / iQOO** | iQOO Z9 5G, vivo T3 5G | MediaTek Dimensity 7200 | Funtouch OS 14 (Android 14) | Linux 5.15 | `iQOO-Z9_5G-vivo-T3_5G-Root-GhostLock` | Shizuku APK launcher |
| **OPPO** | Find X7, Reno 11 / 12 | Dimensity 9300 / 8200 | ColorOS 14 (Android 14) | 5.15, 6.1 | `oppo-ghostlock` | GhostLock profile matching |
| **Amazon** | Fire Max 11 (`sunstone`) | MediaTek MT8188J | Fire OS 8 (8.3.1.9 - 8.3.3.8) | Linux 5.10 | `GhostLock`, `DFRoot` | GhostLock 5.10 / DFRoot late-load |
| **Amazon** | Fire TV Stick 4K Max 2nd Gen (`karat`) | MediaTek MT7921 | Fire OS 8 | Linux 5.10 | `GhostLock`, `DFRoot` | GhostLock 5.10 |
| **Meta** | Quest 3 | Snapdragon XR2 Gen 2 | Android 12 base | Linux 5.10 | `Root-My-Device-Payloads` | KernelSU module injection |
| **Generic Linux** | x86_64 / AArch64 Servers | Intel / AMD / AWS Graviton | Ubuntu, Fedora, OpenSUSE | Linux 5.10 - 6.17 | `dirtyfrag-rs`, `dirtyfrag-arm64` | Arbitrary page cache overwrite |

---

## 2. Bootloader Requirements by Technique

1. **Locked Bootloader (Jailbreak / Runtime):**
   - GhostLock / IonStack family (`ghostlock-app`, `ghostlock-a17`, `GhostLock-Galaxy`, etc.)
   - DirtyFrag family (`DFRoot`, `DirtyInit`, `DirtyFrag-Galaxy`)
   - LSPromise (`lspromise`)
   - All achieve runtime execution without wiping device user data or unlocking cryptographic boot trust chains.
2. **Unlocked Bootloader Optional / Persistent:**
   - `Root-My-Pixel` and `Root-My-Galaxy` support flashing patched `boot.img` / `init_boot.img` partitions via Fastboot / Odin when permanent bootloader unlocks are available.
