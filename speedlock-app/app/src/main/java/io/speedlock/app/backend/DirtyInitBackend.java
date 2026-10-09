package io.speedlock.app.backend;

import io.speedlock.app.model.BackendCapability;
import io.speedlock.app.model.BackendState;
import io.speedlock.app.model.DeviceProfile;
import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 * Backend adapter for DirtyInit (Init Namespace Escalation via CVE-2026-43284).
 * Overwrites shared library page cache to intercept init LogMessage handling,
 * establishing a local socket stream into u:r:init:s0.
 */
public class DirtyInitBackend implements IRootBackend {

    @Override
    public String getId() { return "dirtyinit"; }

    @Override
    public String getDisplayName() { return "DirtyInit (Init Socket Relay)"; }

    @Override
    public String getCve() { return "CVE-2026-43284"; }

    @Override
    public List<String> getSupportedArchitectures() {
        return Arrays.asList("arm64");
    }

    @Override
    public List<String> getRequiredComponents() {
        return Arrays.asList(
            "dirtyinit.c (native orchestrator)",
            "dfi_exploit.c (ESP writer)",
            "/system/lib64/libbase.so (target log library)"
        );
    }

    @Override
    public List<String> getSupportedOperations() {
        return Arrays.asList(
            "Init Process Function Pointer Hooking",
            "Init Domain Stream Socket Relay (u:r:init:s0)",
            "SELinux execute_no_trans Domain Transition Evasion"
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

        // Check if device is GKI 5.10
        if (profile.isGki510()) {
            cap.getReasons().add("Kernel is GKI 5.10; IPsec transform architecture matches target prerequisites");
            cap.setPrerequisitesMet(true);
            cap.setState(BackendState.METADATA_COMPATIBLE);
            cap.getCriticalBlockers().add(
                "Requires extraction of exact libbase.so LogMessage function offsets for X6871 build " +
                profile.getBuildDisplayId() + "; untrusted socket relay unverified on live Transsion userspace."
            );
        } else {
            cap.setState(BackendState.UNTESTED);
            cap.getCriticalBlockers().add("Non-GKI 5.10 kernel; requires manual memory layout audit");
        }

        return cap;
    }

    @Override
    public boolean verifyLocalArtifacts(File workspaceRoot) {
        File dirtyinitDir = new File(workspaceRoot, "repositories/dirtyfrag/DirtyInit");
        File dInitC = new File(dirtyinitDir, "native/dirtyinit.c");
        File dfiExpC = new File(dirtyinitDir, "native/dfi_exploit.c");
        return dirtyinitDir.isDirectory() && dInitC.exists() && dfiExpC.exists();
    }
}
