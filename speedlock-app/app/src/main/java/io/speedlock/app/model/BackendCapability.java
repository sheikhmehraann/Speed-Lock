package io.speedlock.app.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the evaluated capabilities, prerequisites, and blockers
 * for an exploit backend implementation against a target device profile.
 */
public class BackendCapability {
    private final String backendId;
    private final String displayName;
    private final String cve;
    private BackendState state = BackendState.UNTESTED;
    private boolean prerequisitesMet = false;
    private final List<String> reasons = new ArrayList<>();
    private final List<String> criticalBlockers = new ArrayList<>();
    private final List<String> requiredFiles = new ArrayList<>();
    private final List<String> operationsSupported = new ArrayList<>();

    public BackendCapability(String backendId, String displayName, String cve) {
        this.backendId = backendId;
        this.displayName = displayName;
        this.cve = cve;
    }

    public String getBackendId() { return backendId; }
    public String getDisplayName() { return displayName; }
    public String getCve() { return cve; }

    public BackendState getState() { return state; }
    public void setState(BackendState state) { this.state = state; }

    public boolean isPrerequisitesMet() { return prerequisitesMet; }
    public void setPrerequisitesMet(boolean prerequisitesMet) { this.prerequisitesMet = prerequisitesMet; }

    public List<String> getReasons() { return reasons; }
    public List<String> getCriticalBlockers() { return criticalBlockers; }
    public List<String> getRequiredFiles() { return requiredFiles; }
    public List<String> getOperationsSupported() { return operationsSupported; }

    public boolean isActionable() {
        return state.isActionable();
    }
}
