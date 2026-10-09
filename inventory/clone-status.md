# Speed Lock — Repository Clone & Verification Status

**Workspace Root:** `C:\Users\Admin\Videos\Github\Speed Lock`  
**Audit Timestamp:** 2026-10-09 12:15:00 UTC  
**Total Curated Repositories:** 39  

---

## 1. Executive Summary

| Metric | Count | Details |
|---|---|---|
| **Total Curated Repositories** | 39 | All requested and newly discovered 5.10 repositories catalogued |
| **Successfully Cloned** | 39 | Cloned with 100% success rate from GitHub |
| **Failed / Inaccessible** | 0 | Zero connectivity or URL resolution failures |
| **Reused Without Disruption** | 32 (Session 1) / 39 (Session 2+) | Verified non-destructive workflow |
| **Repositories with Active Submodules** | 3 | `Root-My-Device`, `Root-My-Device-Payloads`, `Root-My-Pixel` |
| **Repositories with Git LFS** | 0 | Checked `.gitattributes`; no missing binary LFS pointers |
| **Working Tree Modifications (Dirty)** | 0 | All 39 working trees clean and untouched |

---

## 2. Category Breakdown

| Category Folder | Repo Count | Research Focus |
|---|---|---|
| `repositories/ghostlock-ionstack/` | 4 | Core IonStack primitives, framework app, base exploits, and Quest 3 5.10 port |
| `repositories/dirtyfrag/` | 9 | CVE-2026-43284 / CVE-2026-43500 reference PoC, ports, Android tools, and purple-team rules |
| `repositories/device-specific-projects/` | 20 | OEM-specific locked-bootloader jailbreaks (Samsung, Pixel, Nothing, Meizu, OnePlus, POCO, Quest) |
| `repositories/root-management-apps/` | 2 | Modern zero-memory-corruption exploit chains (LSPromise) & universal root apps (UniRoot) |
| `repositories/kernel-research/` | 2 | Upstream research foundations (CyberMeowfia) and target offset extractors (popsicle) |
| `repositories/reference-catalogues/` | 2 | Comprehensive historical and contemporary Android rooting catalogues |

---

## 3. Comprehensive Verification Table

| Repository Name | Category | Upstream Branch | Commit SHA | Last Commit Date | Submodules | Status |
|---|---|---|---|---|---|---|
| [ghostlock-app](https://github.com/YuKongA/ghostlock-app) | `ghostlock-ionstack` | `main` | `3d4306c` (`3d4306c1abcf7b17a6e89ca5bc75721f873125c5`) | 2026-10-07 | None | Cloned (Verified) |
| [ghostlock-a17](https://github.com/mobilehackinglab/ghostlock-a17) | `ghostlock-ionstack` | `main` | `eddefdd` (`eddefdda015cef713df4753d2665cb4a52f5b65d`) | 2026-08-21 | None | Cloned (Verified) |
| [GhostLock](https://github.com/R0rt1z2/GhostLock) | `ghostlock-ionstack` | `5.10` | `2752d2e` (`2752d2e5aef40c5007f23f36ce89ee29e1a6a4bc`) | 2026-08-21 | None | Cloned (Verified) |
| [DFRoot](https://github.com/diabl0w/DFRoot) | `dirtyfrag` | `master` | `209867f` (`209867fbb4059140cf474e4fa9b2ea57575a2ce1`) | 2026-10-08 | None | Cloned (Verified) |
| [DirtyFrag-mitschud](https://github.com/mitschud/DirtyFrag) | `dirtyfrag` | `master` | `0f4a4c0` (`0f4a4c0520f065d93d21a54a5c94b0fc8c620132`) | 2026-10-09 | None | Cloned (Verified) |
| [DFReroot](https://github.com/polygraphene/DFReroot) | `dirtyfrag` | `main` | `9edc769` (`9edc769518f721d4cd39ad16e638e322abd74c45`) | 2026-09-28 | None | Cloned (Verified) |
| [DirtyInit](https://github.com/combeng6th/DirtyInit) | `dirtyfrag` | `main` | `3409c35` (`3409c35e723c73c527f395548a653966148e8f88`) | 2026-09-19 | None | Cloned (Verified) |
| [dirtyfrag-V4bel](https://github.com/V4bel/dirtyfrag) | `dirtyfrag` | `master` | `aab16fc` (`aab16fcada27142dd8ce8704906cf6736cf213b8`) | 2026-05-11 | None | Cloned (Verified) |
| [dirtyfrag-rs](https://github.com/t0asts/dirtyfrag-rs) | `dirtyfrag` | `main` | `dc908c0` (`dc908c03b0545b736cd36f7b0968ef95d0c8c748`) | 2026-05-08 | None | Cloned (Verified) |
| [dirtyfrag-arm64](https://github.com/linnemanlabs/dirtyfrag-arm64) | `dirtyfrag` | `main` | `7f45d0b` (`7f45d0bccd3bdfa478c476bd58b202441f511fc9`) | 2026-05-13 | None | Cloned (Verified) |
| [DirtyFrag-Galaxy](https://github.com/coey0814/DirtyFrag-Galaxy) | `dirtyfrag` | `main` | `dab227d` (`dab227d863e5a321f20e23c684e9d9ed943c8804`) | 2026-10-08 | None | Cloned (Verified) |
| [Dirty-Frag-hunting](https://github.com/0xAllow/Dirty-Frag-hunting) | `dirtyfrag` | `main` | `e05b43a` (`e05b43a7e45e35c42d06e043cd6295a4931349cd`) | 2026-05-22 | None | Cloned (Verified) |
| [ghostlock-oneplus](https://github.com/JoinChang/ghostlock-oneplus) | `device-specific-projects` | `main` | `9b2a7ba` (`9b2a7ba61cc2afe0f5652af2f317b62eec68356a`) | 2026-09-17 | None | Cloned (Verified) |
| [GhostLock-Galaxy](https://github.com/wxxsfxyzm/GhostLock-Galaxy) | `device-specific-projects` | `main` | `f42c0cb` (`f42c0cb5a3a1a3fff8a5b9289dce62e6d5293ea7`) | 2026-10-05 | None | Cloned (Verified) |
| [GhostSam](https://github.com/snothin/GhostSam) | `device-specific-projects` | `main` | `818faef` (`818faef4a278353a22c3481a01a6eaedf8088a3d`) | 2026-10-06 | None | Cloned (Verified) |
| [ghostlock-emerald](https://github.com/datfooldive/ghostlock-emerald) | `device-specific-projects` | `main` | `ebb355d` (`ebb355d302629a034d0959e5e579496559e8f84e`) | 2026-07-31 | None | Cloned (Verified) |
| [oppo-ghostlock](https://github.com/pubglite55/oppo-ghostlock) | `device-specific-projects` | `main` | `6db06a4` (`6db06a4b73853062b4b97df905e79a963b78ffa2`) | 2026-09-08 | None | Cloned (Verified) |
| [meizu21-ghostlock-root](https://github.com/ymh001/meizu21-ghostlock-root) | `device-specific-projects` | `main` | `b02925c` (`b02925c93b4a3afe9242d7e7ce1a10af69a74178`) | 2026-09-14 | None | Cloned (Verified) |
| [iQOO-Z9_5G-vivo-T3_5G-Root-GhostLock](https://github.com/ankitrawatgit/iQOO-Z9_5G-vivo-T3_5G-Root-GhostLock) | `device-specific-projects` | `main` | `303e954` (`303e954b2b011d3b77c3d112b4301fb2e04dafff`) | 2026-09-07 | None | Cloned (Verified) |
| [Root-My-Galaxy](https://github.com/BuSung-dev/Root-My-Galaxy) | `device-specific-projects` | `main` | `23adf39` (`23adf39fcd4f1be4e73184ee960a6d5399baf147`) | 2026-09-03 | None | Cloned (Verified) |
| [Root-My-Galaxy-Payloads](https://github.com/BuSung-dev/Root-My-Galaxy-Payloads) | `device-specific-projects` | `main` | `6e3223e` (`6e3223e689688540060ddd97a0927e927bfed207`) | 2026-09-03 | None | Cloned (Verified) |
| [Root-My-Pixel](https://github.com/alex193a/Root-My-Pixel) | `device-specific-projects` | `main` | `03a6f76` (`03a6f7642bf0972e36c338829a02ba61d149105b`) | 2026-09-12 | 1 Submodule | Cloned (Verified) |
| [Root-My-Pixel-Payloads](https://github.com/alex193a/Root-My-Pixel-Payloads) | `device-specific-projects` | `main` | `1836bb1` (`1836bb16fba9b9e5680fc374ce10e5de124520bc`) | 2026-09-07 | None | Cloned (Verified) |
| [Root-My-Device](https://github.com/tqmane/Root-My-Device) | `device-specific-projects` | `main` | `0aac524` (`0aac524e0ee50647d6f924bb8ae38ed8098142d2`) | 2026-09-22 | 2 Submodules | Cloned (Verified) |
| [root-my-nothing](https://github.com/ang3lo-azevedo/root-my-nothing) | `device-specific-projects` | `spacewar` | `4bb51c4` (`4bb51c4cfca6b0f7df1e7815d3f405eac2fda438`) | 2026-08-07 | None | Cloned (Verified) |
| [pixel-ksu-root](https://github.com/JingMatrix/pixel-ksu-root) | `device-specific-projects` | `main` | `65762e7` (`65762e75248d99c07eaf4e518e2b928e279411bd`) | 2026-10-05 | None | Cloned (Verified) |
| [Root-My-Device-Payloads](https://github.com/WitAqua-tools/Root-My-Device-Payloads) | `device-specific-projects` | `main` | `6099967` (`6099967b2f75c120ffa14b42876c3945db57ad70`) | 2026-08-29 | 2 Submodules | Cloned (Verified) |
| [CVE-2026-43499-popsicle](https://github.com/x-spy/CVE-2026-43499-popsicle) | `kernel-research` | `main` | `2765ac6` (`2765ac6c5d9be686bc6cd07ad8c998d1c2e67012`) | 2026-07-16 | None | Cloned (Verified) |
| [CyberMeowfia](https://github.com/NebuSec/CyberMeowfia) | `kernel-research` | `main` | `94ae8ab` (`94ae8ab301b2ec9f36bdd66321b6e11209ae60d1`) | 2026-09-07 | None | Cloned (Verified) |
| [lspromise](https://github.com/lsposed/lspromise) | `root-management-apps` | `master` | `0258165` (`0258165d91aa72451dcd8406934ba3fc34f52c14`) | 2026-09-09 | None | Cloned (Verified) |
| [awesome-android-root-exploits](https://github.com/DuncanParSky/awesome-android-root-exploits) | `reference-catalogues` | `main` | `e0d9dd0` (`e0d9dd00b4c58ebe006253c1dce60e24930a084e`) | 2026-07-26 | None | Cloned (Verified) |
| [awesome-android-root](https://github.com/awesome-android-root/awesome-android-root) | `reference-catalogues` | `main` | `887470c` (`887470caa768f42b08fcbbbdc96a6622f7609ebe`) | 2026-10-08 | None | Cloned (Verified) |
| [IonStackQuest3](https://github.com/F-19-F/IonStackQuest3) | `ghostlock-ionstack` | `main` | `81faf79` (`81faf7942576fd826bb097fb2b4fae76e090a103`) | 2026-08-06 | None | Cloned (Verified) |
| [IonStack-S22U](https://github.com/sarabpal-dev/IonStack-S22U) | `device-specific-projects` | `main` | `fad1773` (`fad1773b3432b762fc84dd85e9306db932c5926e`) | 2026-10-04 | None | Cloned (Verified) |
| [smt878u-ionstack-poc](https://github.com/Wtrwx/smt878u-ionstack-poc) | `device-specific-projects` | `main` | `88f34c6` (`88f34c628ab8cec69aa944c9bf8f1cd8981186e3`) | 2026-10-07 | None | Cloned (Verified) |
| [QuestStack](https://github.com/starseed12345/QuestStack) | `device-specific-projects` | `main` | `7f943f8` (`7f943f867067e8ef575ec91b8554c4cb08fa83ac`) | 2026-08-28 | None | Cloned (Verified) |
| [Root-My-Galaxy-SM-S918B](https://github.com/soumarcelino/Root-My-Galaxy-SM-S918B) | `device-specific-projects` | `main` | `3624164` (`362416498212658e17f72f05e64259b85998d75b`) | 2026-09-30 | None | Cloned (Verified) |
| [Root-My-Device-techtornados](https://github.com/techtornados/Root-My-Device) | `device-specific-projects` | `main` | `4208d4d` (`4208d4dded885d4c9c63f92cd8b485587e5b50ba`) | 2026-09-04 | None | Cloned (Verified) |
| [UniRoot](https://github.com/kuuky29/UniRoot) | `root-management-apps` | `main` | `6d646d4` (`6d646d4dc916f666be170481afd640d9954136ce`) | 2026-09-30 | None | Cloned (Verified) |
