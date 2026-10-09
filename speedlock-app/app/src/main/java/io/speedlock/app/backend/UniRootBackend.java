package io.speedlock.app.backend;

import io.speedlock.app.model.BackendCapability;
import io.speedlock.app.model.BackendState;
import io.speedlock.app.model.DeviceProfile;
import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 * Backend adapter for UniRoot (Universal Multi-Chain Root Manager).
 * Combines fast unprivileged DirtyFrag engine, GhostLock profiles,
 * and in-app ksud patching for KernelSU/KernelSU-Next.
 */
public class UniRootBackend implements IRootBackend {

    @Override
    public String getId() { return "uniroot"; }

    @Override
    public String getDisplayName() { return "UniRoot (Universal Manager)"; }

    @Override
    public String getCve() { return "CVE-2026-43284 / CVE-2026-43499"; }

    @Override
    public List<String> getSupportedArchitectures() {
        return Arrays.asList("arm64");
    }

    @Override
    public List<String> getRequiredComponents() {
        return Arrays.asList(
            "UniRoot app bundle (Kotlin/Gradle)",
            "ksud binary (KernelSU daemon)",
            "Vendor-patched .ko modules"
        );
    }

    @Override
    public List<String> getSupportedOperations() {
        return Arrays.asList(
            "Dual-Engine Selection (DirtyFrag Fast / GhostLock Profile)",
            "On-Device ksud Patching and Module Delivery",
            "Foreground Boot Service Auto-Root"
        );
    }

    @Override
    public BackendCapability evaluateCompatibility(DeviceProfile profile) {
        BackendCapability cap = new BackendCapability(getId(), getDisplayName(), getCve());
        cap.getRequiredFiles().addAll(getRequiredComponents());
        cap.getOperationsSupported().addAll(getSupportedOperations());

        if (!getSupportedArchitectures().contains(profile.getArchitecture())) {
            cap.setState(BackendState.UNSUPPORTED);
            cap.getCriticalBlockers().add("Architecture '" + profile.getArchitecture() + "' unsupported");
            return cap;
        }

        if (profile.isGki510()) {
            cap.getReasons().add("Universal ARM64 manager architecture compatible with GKI 5.10 targets");
            cap.setPrerequisitesMet(true);
            cap.setState(BackendState.METADATA_COMPATIBLE);
            cap.getCriticalBlockers().add(
                "Upstream UniRoot is tuned for Samsung Knox/DEFEX bypass; " +
                "MediaTek Dimensity 8200 X6871 requires Transsion-specific SEPolicy and symbol CRC modules."
            );
        } else {
            cap.setState(BackendState.UNTESTED);
        }

        return cap;
    }

    @Override
    public boolean verifyLocalArtifacts(File workspaceRoot) {
        File unirootDir = new File(workspaceRoot, "repositories/root-management-apps/UniRoot");
        File buildFile = new File(unirootDir, "app/build.gradle.kts");
        return unirootDir.isDirectory() && buildFile.exists();
    }
}
