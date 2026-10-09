package io.speedlock.app.ui;

import io.speedlock.app.model.BackendCapability;
import java.util.Map;

/**
 * Renders the Backend Catalogue screen, showing available implementations,
 * their target CVEs, operations supported, and required files.
 */
public class BackendCatalogueView {

    public static String render(Map<String, BackendCapability> capabilities) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== BACKEND CATALOGUE ===\n");
        for (BackendCapability cap : capabilities.values()) {
            sb.append("\n--------------------------------------------------\n");
            sb.append(cap.getDisplayName()).append(" [").append(cap.getCve()).append("]\n");
            sb.append("State: ").append(cap.getState().getDisplayName()).append("\n");
            sb.append("Operations Supported:\n");
            for (String op : cap.getOperationsSupported()) {
                sb.append("  • ").append(op).append("\n");
            }
            sb.append("Required Components:\n");
            for (String f : cap.getRequiredFiles()) {
                sb.append("  - ").append(f).append("\n");
            }
        }
        return sb.toString();
    }
}
