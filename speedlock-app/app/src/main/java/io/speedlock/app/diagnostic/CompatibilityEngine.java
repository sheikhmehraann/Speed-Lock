package io.speedlock.app.diagnostic;

import io.speedlock.app.backend.BackendRegistry;
import io.speedlock.app.model.BackendCapability;
import io.speedlock.app.model.BackendState;
import io.speedlock.app.model.DeviceProfile;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Evaluates holistic device compatibility across hardware, kernel,
 * vendor filesystem, and registered exploit backends.
 */
public class CompatibilityEngine {

    public static class CompatibilitySummary {
        public final DeviceProfile profile;
        public final Map<String, BackendCapability> backendCapabilities;
        public final List<String> verifiedFacts = new ArrayList<>();
        public final List<String> criticalBlockers = new ArrayList<>();
        public final boolean hasActionableBackend;

        public CompatibilitySummary(DeviceProfile profile, Map<String, BackendCapability> caps) {
            this.profile = profile;
            this.backendCapabilities = caps;
            boolean actionable = false;
            for (BackendCapability cap : caps.values()) {
                if (cap.isActionable()) {
                    actionable = true;
                    break;
                }
            }
            this.hasActionableBackend = actionable;
        }
    }

    private final BackendRegistry registry;

    public CompatibilityEngine(BackendRegistry registry) {
        this.registry = registry;
    }

    public CompatibilitySummary evaluate(DeviceProfile profile) {
        Map<String, BackendCapability> caps = registry.evaluateAll(profile);
        CompatibilitySummary summary = new CompatibilitySummary(profile, caps);

        // Hardware & Architecture evaluation
        if ("arm64".equalsIgnoreCase(profile.getArchitecture())) {
            summary.verifiedFacts.add("64-bit ARM architecture (AArch64) supported by all modern backends");
        } else {
            summary.criticalBlockers.add("Unsupported architecture: " + profile.getArchitecture());
        }

        // Kernel evaluation
        if (profile.isGki510()) {
            summary.verifiedFacts.add("Linux 5.10 GKI 2.0 kernel family detected (" + profile.getKernelRelease() + ")");
        } else if (profile.getKernelRelease() != null && !profile.getKernelRelease().isEmpty()) {
            summary.verifiedFacts.add("Non-GKI 5.10 kernel detected: " + profile.getKernelRelease());
        }

        // Page size and VA bits
        if (profile.getPageSizeBytes() == 4096) {
            summary.verifiedFacts.add("4096-byte memory page size verified");
        } else {
            summary.criticalBlockers.add("Non-standard page size: " + profile.getPageSizeBytes() + " bytes");
        }

        // Subsystems
        Map<String, String> cfg = profile.getConfigFlags();
        if ("y".equals(cfg.get("CONFIG_XFRM")) && "y".equals(cfg.get("CONFIG_INET_ESP"))) {
            summary.verifiedFacts.add("IPsec transform (XFRM) & ESP subsystems enabled for DirtyFrag");
        }
        if ("y".equals(cfg.get("CONFIG_FUTEX"))) {
            summary.verifiedFacts.add("Futex PI subsystem enabled for GhostLock");
        }
        if ("is not set".equals(cfg.get("CONFIG_MODULE_SIG"))) {
            summary.verifiedFacts.add("Kernel module signing check disabled (# CONFIG_MODULE_SIG is not set)");
        }

        // Candidate libraries and symlinks
        if (!profile.getCandidateLibraries().isEmpty()) {
            summary.verifiedFacts.add("Target library candidate verified: " + profile.getCandidateLibraries().get(0));
        }
        if (profile.hasInsmodSymlink()) {
            summary.verifiedFacts.add("Vendor insmod binary symlink verified (/vendor/bin/insmod)");
        }

        // Aggregate blockers from backends
        for (BackendCapability cap : caps.values()) {
            for (String blocker : cap.getCriticalBlockers()) {
                if (!summary.criticalBlockers.contains(blocker)) {
                    summary.criticalBlockers.add("[" + cap.getDisplayName() + "] " + blocker);
                }
            }
        }

        return summary;
    }
}
