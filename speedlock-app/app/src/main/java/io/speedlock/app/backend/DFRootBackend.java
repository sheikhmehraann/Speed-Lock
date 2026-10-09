package io.speedlock.app.backend;

import io.speedlock.app.model.BackendCapability;
import io.speedlock.app.model.BackendState;
import io.speedlock.app.model.DeviceProfile;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Backend adapter for DFRoot (CVE-2026-43284, DirtyFrag).
 * Evaluates kernel config prerequisites, vendor library candidates,
 * and Loadable Kernel Module (LKM) ABI compatibility.
 */
public class DFRootBackend implements IRootBackend {

    @Override
    public String getId() { return "dfroot"; }

    @Override
    public String getDisplayName() { return "DFRoot (CVE-2026-43284)"; }

    @Override
    public String getCve() { return "CVE-2026-43284"; }

    @Override
    public List<String> getSupportedArchitectures() {
        return Arrays.asList("arm64");
    }

    @Override
    public List<String> getRequiredComponents() {
        return Arrays.asList(
            "exp.c (JNI native)",
            "splicehelper.c (splice pipe helper)",
            "libcxx.S (custom runtime shim)",
            "dfroot.ko (target-matched kernel module)"
        );
    }

    @Override
    public List<String> getSupportedOperations() {
        return Arrays.asList(
            "Unprivileged ESP Page Cache Overwrite",
            "Target Shared Library In-Place Patching",
            "Kernel Module Late-Load without Unlocked Bootloader"
        );
    }

    @Override
    public BackendCapability evaluateCompatibility(DeviceProfile profile) {
        BackendCapability cap = new BackendCapability(getId(), getDisplayName(), getCve());
        cap.getRequiredFiles().addAll(getRequiredComponents());
        cap.getOperationsSupported().addAll(getSupportedOperations());

        // 1. Architecture check
        if (!getSupportedArchitectures().contains(profile.getArchitecture())) {
            cap.setState(BackendState.UNSUPPORTED);
            cap.getCriticalBlockers().add("Architecture '" + profile.getArchitecture() + "' unsupported (requires arm64)");
            return cap;
        }

        // 2. Kernel configuration checks
        Map<String, String> cfg = profile.getConfigFlags();
        boolean hasXfrm = "y".equals(cfg.get("CONFIG_XFRM"));
        boolean hasEsp = "y".equals(cfg.get("CONFIG_INET_ESP"));
        boolean hasModules = "y".equals(cfg.get("CONFIG_MODULES"));
        boolean hasKprobes = "y".equals(cfg.get("CONFIG_KPROBES"));

        if (!hasXfrm || !hasEsp || !hasModules || !hasKprobes) {
            cap.setState(BackendState.UNSUPPORTED);
            if (!hasXfrm) cap.getCriticalBlockers().add("CONFIG_XFRM=y missing: required for IPsec transform allocation");
            if (!hasEsp) cap.getCriticalBlockers().add("CONFIG_INET_ESP=y missing: required for ESP packet decryption primitive");
            if (!hasModules) cap.getCriticalBlockers().add("CONFIG_MODULES=y missing: required for loadable kernel modules");
            if (!hasKprobes) cap.getCriticalBlockers().add("CONFIG_KPROBES=y missing: required for dynamic symbol resolution");
            return cap;
        }

        cap.getReasons().add("Kernel configuration satisfies all required DirtyFrag subsystems (XFRM, ESP, MODULES, KPROBES)");

        // 3. Candidate library
        if (profile.getCandidateLibraries().isEmpty()) {
            cap.setState(BackendState.UNSUPPORTED);
            cap.getCriticalBlockers().add("No target vendor library candidate (/vendor/lib64/libbinderdebug.so) found in firmware");
            return cap;
        }
        cap.getReasons().add("Candidate library present: " + profile.getCandidateLibraries().get(0));

        // 4. Insmod symlink
        if (!profile.hasInsmodSymlink()) {
            cap.setState(BackendState.UNSUPPORTED);
            cap.getCriticalBlockers().add("Executable /vendor/bin/insmod missing from vendor filesystem");
            return cap;
        }
        cap.getReasons().add("Executable /vendor/bin/insmod is present as a valid symlink to toybox_vendor");

        // 5. Binary LKM Blocker Check
        cap.setPrerequisitesMet(true);
        cap.setState(BackendState.METADATA_COMPATIBLE);
        cap.getCriticalBlockers().add(
            "Prebuilt dfroot-android12-5.10.ko carries mismatched vermagic '5.10.252-dirty' " +
            "and empty __versions section; target kernel strictly enforces CONFIG_MODVERSIONS=y " +
            "requiring authentic module compilation against ACK 5.10 commit f82f7360927e."
        );

        return cap;
    }

    @Override
    public boolean verifyLocalArtifacts(File workspaceRoot) {
        File dfrootDir = new File(workspaceRoot, "repositories/dirtyfrag/DFRoot");
        File expC = new File(dfrootDir, "app/src/main/jni/exp.c");
        File lkmC = new File(dfrootDir, "lkm/dfroot.c");
        return dfrootDir.isDirectory() && expC.exists() && lkmC.exists();
    }
}
