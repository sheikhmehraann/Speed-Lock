package io.speedlock.app.model;

/**
 * Explicit, honest lifecycle states for exploit backends and device support.
 * Prevents conflating metadata matching with verified device support.
 */
public enum BackendState {
    /** Backend source files, modules, or dependencies are missing from the installation. */
    UNAVAILABLE("Unavailable"),

    /** Device hardware, architecture, or kernel configuration is fundamentally incompatible. */
    UNSUPPORTED("Unsupported"),

    /** Device or kernel artifacts have not been audited or verified. */
    UNTESTED("Untested"),

    /** High-level kernel config, subsystems, and libraries match, but binary ABI is unverified. */
    METADATA_COMPATIBLE("Metadata Compatible"),

    /** Kernel module / native binary has passed deep binary ABI verification (matching vermagic & CRCs). */
    BUILD_VERIFIED("Build Verified"),

    /** Authorized physical execution on target hardware has confirmed operational functionality. */
    DEVICE_VERIFIED("Device Verified");

    private final String displayName;

    BackendState(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isActionable() {
        return this == BUILD_VERIFIED || this == DEVICE_VERIFIED;
    }
}
