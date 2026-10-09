package io.speedlock.app.backend;

import io.speedlock.app.model.BackendCapability;
import io.speedlock.app.model.BackendState;
import io.speedlock.app.model.DeviceProfile;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Backend adapter for GhostLock / IonStack (CVE-2026-43499).
 * Evaluates Futex subsystem prerequisites, virtual address geometry,
 * and MediaTek physical load address requirements.
 */
public class GhostLockBackend implements IRootBackend {

    @Override
    public String getId() { return "ghostlock"; }

    @Override
    public String getDisplayName() { return "GhostLock / IonStack (CVE-2026-43499)"; }

    @Override
    public String getCve() { return "CVE-2026-43499"; }

    @Override
    public List<String> getSupportedArchitectures() {
        return Arrays.asList("arm64");
    }

    @Override
    public List<String> getRequiredComponents() {
        return Arrays.asList(
            "rootchain.c (native payload)",
            "target.h / ionstack.conf (struct offsets)",
            "kernel_phys_load & kernel_phys_offset (MediaTek physical addresses)"
        );
    }

    @Override
    public List<String> getSupportedOperations() {
        return Arrays.asList(
            "Futex PI Subsystem Kernel Memory Manipulation",
            "Arbitrary Kernel Write (W1/W2/W3 stages)",
            "Usermode Helper Execution (u:r:kernel:s0)"
        );
    }

    @Override
    public BackendCapability evaluateCompatibility(DeviceProfile profile) {
        BackendCapability cap = new BackendCapability(getId(), getDisplayName(), getCve());
        cap.getRequiredFiles().addAll(getRequiredComponents());
        cap.getOperationsSupported().addAll(getSupportedOperations());

        if (!getSupportedArchitectures().contains(profile.getArchitecture())) {
            cap.setState(BackendState.UNSUPPORTED);
            cap.getCriticalBlockers().add("Architecture '" + profile.getArchitecture() + "' unsupported (requires arm64)");
            return cap;
        }

        Map<String, String> cfg = profile.getConfigFlags();
        if (!"y".equals(cfg.get("CONFIG_FUTEX"))) {
            cap.setState(BackendState.UNSUPPORTED);
            cap.getCriticalBlockers().add("CONFIG_FUTEX=y missing from kernel configuration");
            return cap;
        }
        cap.getReasons().add("Futex PI subsystem is compiled into kernel (CONFIG_FUTEX=y)");

        if (profile.getPageSizeBytes() == 4096) {
            cap.getReasons().add("4KB page size compatible with standard ARM64 IonStack memory spray");
        } else {
            cap.setState(BackendState.UNSUPPORTED);
            cap.getCriticalBlockers().add("Non-standard page size: " + profile.getPageSizeBytes() + " bytes");
            return cap;
        }

        if (profile.getVaBits() == 39) {
            cap.getReasons().add("39-bit Virtual Address space matches GKI 5.10 memory layout");
        }

        cap.setPrerequisitesMet(true);
        cap.setState(BackendState.METADATA_COMPATIBLE);

        // MediaTek Specific Blocker
        cap.getCriticalBlockers().add(
            "Missing MediaTek physical load addresses (kernel_phys_load / kernel_phys_offset); " +
            "MediaTek firmware lacks embedded BTF, requiring mtk-phys extractor execution on physical device."
        );
        cap.getCriticalBlockers().add(
            "Missing compiled target profile header (target.h / ionstack.conf) containing exact task_struct, " +
            "cred, and call_usermodehelper_exec_work offsets for build ab14119954."
        );

        return cap;
    }

    @Override
    public boolean verifyLocalArtifacts(File workspaceRoot) {
        File ghostlockDir = new File(workspaceRoot, "repositories/ghostlock-ionstack/GhostLock");
        File appDir = new File(workspaceRoot, "repositories/ghostlock-ionstack/ghostlock-app");
        File rootchainC = new File(ghostlockDir, "src/rootchain.c");
        return ghostlockDir.isDirectory() && appDir.isDirectory() && rootchainC.exists();
    }
}
