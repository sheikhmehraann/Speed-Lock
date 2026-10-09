package io.speedlock.app.ui;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import io.speedlock.app.R;
import io.speedlock.app.SpeedLockApp;
import io.speedlock.app.detector.DeviceDetector;
import io.speedlock.app.detector.SocClassifier;
import io.speedlock.app.diagnostic.CompatibilityEngine;
import io.speedlock.app.diagnostic.DiagnosticLogger;
import io.speedlock.app.diagnostic.ReportExporter;
import io.speedlock.app.model.BackendCapability;
import io.speedlock.app.model.DeviceProfile;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Controller and entrypoint Activity for the Speed Lock dashboard.
 * Coordinates live device detection, backend discovery, 6-screen navigation,
 * and report generation with Fluent/Material 3 responsive controls.
 */
public class MainActivity extends Activity {
    private SpeedLockApp app;
    private DeviceProfile currentProfile;
    private CompatibilityEngine.CompatibilitySummary currentSummary;
    private SettingsView.SettingsState settingsState;

    // View references
    private TextView tvDeviceModel;
    private TextView tvSocInfo;
    private TextView tvKernelInfo;
    private TextView tvCompatStatus;
    private TextView tvBlockersSummary;
    private TextView tvBackendList;
    private Button btnRefresh;
    private Button btnExport;

    public MainActivity() {
        // Default constructor
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.app = SpeedLockApp.getInstance();
        this.settingsState = new SettingsView.SettingsState();

        bindViews();
        initializeDashboard();
        updateUi();
    }

    private void bindViews() {
        tvDeviceModel = findViewById(R.id.tv_device_model);
        tvSocInfo = findViewById(R.id.tv_soc_info);
        tvKernelInfo = findViewById(R.id.tv_kernel_info);
        tvCompatStatus = findViewById(R.id.tv_compat_status);
        tvBlockersSummary = findViewById(R.id.tv_blockers_summary);
        tvBackendList = findViewById(R.id.tv_backend_list);
        btnRefresh = findViewById(R.id.btn_refresh);
        btnExport = findViewById(R.id.btn_export);

        if (btnRefresh != null) {
            btnRefresh.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    initializeDashboard();
                    updateUi();
                    Toast.makeText(MainActivity.this, "Diagnostics refreshed.", Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (btnExport != null) {
            btnExport.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    exportReportToStorage();
                }
            });
        }
    }

    /**
     * Initializes the dashboard by querying live device identity
     * and evaluating registered exploit backends.
     */
    public void initializeDashboard() {
        if (app == null) {
            app = SpeedLockApp.getInstance();
        }
        if (settingsState == null) {
            settingsState = new SettingsView.SettingsState();
        }
        app.getLogger().info("MainActivity", "Initializing dashboard and querying device profile...");
        this.currentProfile = DeviceDetector.detectLiveDevice();
        this.currentSummary = app.getCompatibilityEngine().evaluate(currentProfile);
        app.getLogger().info("MainActivity", "Device profile evaluated: " + currentProfile.getModel() +
            " [" + currentProfile.getBspPlatform() + "]");
    }

    private void updateUi() {
        if (currentProfile == null || currentSummary == null) {
            return;
        }

        SocClassifier.SocClassification soc = SocClassifier.classify(currentProfile);

        if (tvDeviceModel != null) {
            tvDeviceModel.setText("Model: " + currentProfile.getMarketingName() + " (" + currentProfile.getModel() + ")");
        }
        if (tvSocInfo != null) {
            tvSocInfo.setText("SoC: " + soc.commercialName + " [BSP Platform: " + soc.bspPlatform + "]");
        }
        if (tvKernelInfo != null) {
            tvKernelInfo.setText("Kernel: " + currentProfile.getKernelRelease() + " (ABI: " + currentProfile.getArchitecture() + ")");
        }
        if (tvCompatStatus != null) {
            tvCompatStatus.setText("Status: [" + (currentSummary.hasActionableBackend ? "METADATA_COMPATIBLE" : "LIMITED_SUPPORT") + "]");
        }
        if (tvBlockersSummary != null) {
            if (currentSummary.criticalBlockers.isEmpty()) {
                tvBlockersSummary.setText("Blockers: None detected for metadata verification.");
            } else {
                tvBlockersSummary.setText("Blockers: " + currentSummary.criticalBlockers.size() + " prerequisite(s) require physical testing.");
            }
        }
        if (tvBackendList != null) {
            tvBackendList.setText(formatBackendSummary());
        }
    }

    private void exportReportToStorage() {
        try {
            String reportJson = generateExportReport(false);
            File exportDir = getExternalFilesDir(null);
            if (exportDir == null) {
                exportDir = getFilesDir();
            }
            File outFile = new File(exportDir, "speedlock_diagnostic_report.json");
            try (FileOutputStream fos = new FileOutputStream(outFile)) {
                fos.write(reportJson.getBytes(StandardCharsets.UTF_8));
            }
            Toast.makeText(this, "Report saved to: " + outFile.getName(), Toast.LENGTH_LONG).show();
            if (app != null) {
                app.getLogger().info("MainActivity", "Exported diagnostic report to " + outFile.getAbsolutePath());
            }
        } catch (Exception e) {
            Toast.makeText(this, "Export failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    public DeviceProfile getCurrentProfile() {
        if (currentProfile == null) {
            initializeDashboard();
        }
        return currentProfile;
    }

    public CompatibilityEngine.CompatibilitySummary getCurrentSummary() {
        if (currentSummary == null) {
            initializeDashboard();
        }
        return currentSummary;
    }

    public SettingsView.SettingsState getSettingsState() {
        if (settingsState == null) {
            settingsState = new SettingsView.SettingsState();
        }
        return settingsState;
    }

    // --- Screen Renderers ---

    public String renderHomeScreen() {
        ensureInitialized();
        StringBuilder sb = new StringBuilder();
        sb.append("==================================================\n");
        sb.append(" SPEED LOCK — X6871 ROOT DIAGNOSTICS DASHBOARD   \n");
        sb.append("==================================================\n\n");
        sb.append("DEVICE:   ").append(currentProfile.getMarketingName()).append(" (").append(currentProfile.getModel()).append(")\n");
        sb.append("PLATFORM: ").append(currentProfile.getBspPlatform()).append(" | KERNEL: ").append(currentProfile.getKernelRelease()).append("\n");
        sb.append("STATUS:   [").append(currentSummary.hasActionableBackend ? "METADATA_COMPATIBLE" : "LIMITED_SUPPORT").append("]\n\n");
        sb.append("QUICK NAVIGATION:\n");
        sb.append("  [1] Device Information   — MT6895 BSP vs MT6896 commercial SoC analysis\n");
        sb.append("  [2] Backend Catalogue    — DFRoot, GhostLock, DirtyInit, UniRoot specifications\n");
        sb.append("  [3] Compatibility Centre — 5-tier audit, metadata, ABI, build & hardware check\n");
        sb.append("  [4] Diagnostic Logs      — Filterable, searchable structured diagnostic trace\n");
        sb.append("  [5] Settings             — Fluent/Material 3 theme, reduced motion & privacy\n");
        return sb.toString();
    }

    public String renderDeviceInfoScreen() {
        ensureInitialized();
        return DeviceInfoView.render(currentProfile);
    }

    public String renderBackendCatalogueScreen() {
        ensureInitialized();
        return BackendCatalogueView.render(currentSummary.backendCapabilities);
    }

    public String renderCompatibilityCentreScreen() {
        ensureInitialized();
        return CompatibilityView.render(currentSummary);
    }

    public String renderDiagnosticLogsScreen(DiagnosticLogger.Level filter, String query) {
        if (app == null) {
            app = SpeedLockApp.getInstance();
        }
        return DiagnosticLogsView.renderFiltered(app.getLogger(), filter, query);
    }

    public String renderSettingsScreen() {
        if (settingsState == null) {
            settingsState = new SettingsView.SettingsState();
        }
        return SettingsView.render(settingsState);
    }

    public String generateExportReport(boolean asMarkdown) {
        ensureInitialized();
        if (asMarkdown) {
            return ReportExporter.exportToMarkdown(currentSummary);
        } else {
            return ReportExporter.exportToJson(currentSummary);
        }
    }

    public String formatBackendSummary() {
        ensureInitialized();
        StringBuilder sb = new StringBuilder();
        for (BackendCapability cap : currentSummary.backendCapabilities.values()) {
            sb.append(String.format("• %s (%s): %s%n",
                cap.getDisplayName(), cap.getCve(), cap.getState().getDisplayName()));
            for (String r : cap.getReasons()) {
                sb.append("   - ").append(r).append("\n");
            }
            for (String b : cap.getCriticalBlockers()) {
                sb.append("   ! BLOCKER: ").append(b).append("\n");
            }
        }
        return sb.toString();
    }

    private void ensureInitialized() {
        if (currentSummary == null || currentProfile == null) {
            initializeDashboard();
        }
    }
}
