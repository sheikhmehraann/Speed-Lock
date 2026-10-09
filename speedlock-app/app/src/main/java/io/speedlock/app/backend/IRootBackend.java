package io.speedlock.app.backend;

import io.speedlock.app.model.BackendCapability;
import io.speedlock.app.model.DeviceProfile;
import java.io.File;
import java.util.List;

/**
 * Common, decoupled interface contract for all root management and exploit backends.
 * Allows diverse implementations to be integrated without indiscriminate merging or source corruption.
 */
public interface IRootBackend {
    /** Unique alphanumeric identifier, e.g., 'dfroot'. */
    String getId();

    /** Human-readable display name. */
    String getDisplayName();

    /** Canonical CVE or vulnerability tracking identifier. */
    String getCve();

    /** List of supported processor architectures (e.g., 'arm64'). */
    List<String> getSupportedArchitectures();

    /** List of required source or compiled components for this backend. */
    List<String> getRequiredComponents();

    /** High-level operations supported by this backend (e.g., 'PageCacheWrite', 'KsuLateLoad'). */
    List<String> getSupportedOperations();

    /**
     * Evaluates compatibility against the given device profile without executing exploit payloads.
     */
    BackendCapability evaluateCompatibility(DeviceProfile profile);

    /**
     * Checks whether required repository artifacts exist on disk in the local workspace.
     */
    boolean verifyLocalArtifacts(File workspaceRoot);
}
