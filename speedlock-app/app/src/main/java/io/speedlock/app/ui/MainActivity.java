package io.speedlock.app.ui;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;

import io.speedlock.app.R;
import io.speedlock.app.SpeedLockApp;
import io.speedlock.app.backend.IRootBackend;
import io.speedlock.app.detector.DeviceDetector;
import io.speedlock.app.detector.SocClassifier;
import io.speedlock.app.diagnostic.CompatibilityEngine;
import io.speedlock.app.diagnostic.DiagnosticLogger;
import io.speedlock.app.diagnostic.ReportExporter;
import io.speedlock.app.model.BackendCapability;
import io.speedlock.app.model.BackendState;
import io.speedlock.app.model.DeviceProfile;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Map;

/**
 * Controller and main entrypoint Activity for the Speed Lock dashboard.
 * Implements a Fluent Design + GhostLock inspired UI with 6 interactive screens,
 * bottom navigation, searchable diagnostic logs, clipboard export, and theme controls.
 */
public class MainActivity extends AppCompatActivity {

    private SpeedLockApp app;
    private DeviceProfile currentProfile;
    private CompatibilityEngine.CompatibilitySummary currentSummary;
    private SettingsView.SettingsState settingsState;

    // Navigation and screen containers
    private BottomNavigationView bottomNav;
    private View screenHome;
    private View screenBackends;
    private View screenDevice;
    private View screenAudit;
    private View screenLogs;
    private View screenSettings;

    // Home views
    private View cardActivationStatus;
    private ImageView ivHomeStatusWatermark;
    private TextView tvHomeStatusBadge;
    private TextView tvHomeDeviceTitle;
    private TextView tvHomeKernelTitle;
    private TextView tvHomeRootState;
    private TextView tvHomeSocName;
    private TextView tvHomeBspPlatform;
    private TextView tvHomeArchInfo;
    private TextView tvHomeDataSource;
    private TextView tvHomeDfrootSummary;
    private TextView tvHomeGhostlockSummary;
    private MaterialButton btnHomeRunAudit;
    private MaterialButton btnHomeGotoBackends;
    private MaterialButton btnHomeExportReport;
    private MaterialButton btnHomeGotoSettings;
    private ImageButton btnActionSettings;

    // Backends views
    private MaterialButton btnBackendAuditDfroot;
    private MaterialButton btnBackendAuditGhostlock;

    // Device views
    private TextView tvDeviceModelVal;
    private TextView tvDeviceBrandVal;
    private TextView tvDeviceBoardVal;
    private TextView tvDevicePlatformVal;
    private TextView tvDeviceKernelVal;
    private TextView tvDeviceSecurityVal;
    private TextView tvDeviceDatasourceVal;

    // Logs views
    private EditText etLogSearch;
    private TextView tvLogOutput;
    private MaterialButton btnFilterAll;
    private MaterialButton btnFilterInfo;
    private MaterialButton btnFilterWarn;
    private MaterialButton btnFilterError;
    private MaterialButton btnLogsCopy;
    private MaterialButton btnLogsExportJson;
    private DiagnosticLogger.Level currentLogLevel = DiagnosticLogger.Level.DEBUG;
    private String currentLogQuery = "";

    // Settings views
    private RadioGroup rgTheme;
    private SwitchCompat switchReducedMotion;
    private SwitchCompat switchAnonymize;
    private MaterialButton btnSettingsUpdates;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.app = SpeedLockApp.getInstance();
        this.settingsState = new SettingsView.SettingsState();

        bindViews();
        setupNavigation();
        setupListeners();
        initializeDashboard();
        updateAllScreens();
    }

    private void bindViews() {
        bottomNav = findViewById(R.id.bottom_navigation);

        // Screens
        screenHome = findViewById(R.id.screen_home);
        screenBackends = findViewById(R.id.screen_backends);
        screenDevice = findViewById(R.id.screen_device);
        screenAudit = findViewById(R.id.screen_audit);
        screenLogs = findViewById(R.id.screen_logs);
        screenSettings = findViewById(R.id.screen_settings);

        // Toolbar Actions
        ImageButton btnRefresh = findViewById(R.id.btn_action_refresh);
        if (btnRefresh != null) {
            btnRefresh.setOnClickListener(v -> {
                initializeDashboard();
                updateAllScreens();
                Toast.makeText(this, "Detection refreshed.", Toast.LENGTH_SHORT).show();
            });
        }
        btnActionSettings = findViewById(R.id.btn_action_settings);

        // Home
        cardActivationStatus = findViewById(R.id.card_activation_status);
        ivHomeStatusWatermark = findViewById(R.id.iv_home_status_watermark);
        tvHomeStatusBadge = findViewById(R.id.tv_home_status_badge);
        tvHomeDeviceTitle = findViewById(R.id.tv_home_device_title);
        tvHomeKernelTitle = findViewById(R.id.tv_home_kernel_title);
        tvHomeRootState = findViewById(R.id.tv_home_root_state);
        tvHomeSocName = findViewById(R.id.tv_home_soc_name);
        tvHomeBspPlatform = findViewById(R.id.tv_home_bsp_platform);
        tvHomeArchInfo = findViewById(R.id.tv_home_arch_info);
        tvHomeDataSource = findViewById(R.id.tv_home_data_source);
        tvHomeDfrootSummary = findViewById(R.id.tv_home_dfroot_summary);
        tvHomeGhostlockSummary = findViewById(R.id.tv_home_ghostlock_summary);
        btnHomeRunAudit = findViewById(R.id.btn_home_run_audit);
        btnHomeGotoBackends = findViewById(R.id.btn_home_goto_backends);
        btnHomeExportReport = findViewById(R.id.btn_home_export_report);
        btnHomeGotoSettings = findViewById(R.id.btn_home_goto_settings);

        // Backends
        btnBackendAuditDfroot = findViewById(R.id.btn_backend_audit_dfroot);
        btnBackendAuditGhostlock = findViewById(R.id.btn_backend_audit_ghostlock);

        // Device
        tvDeviceModelVal = findViewById(R.id.tv_device_model_val);
        tvDeviceBrandVal = findViewById(R.id.tv_device_brand_val);
        tvDeviceBoardVal = findViewById(R.id.tv_device_board_val);
        tvDevicePlatformVal = findViewById(R.id.tv_device_platform_val);
        tvDeviceKernelVal = findViewById(R.id.tv_device_kernel_val);
        tvDeviceSecurityVal = findViewById(R.id.tv_device_security_val);
        tvDeviceDatasourceVal = findViewById(R.id.tv_device_datasource_val);

        // Logs
        etLogSearch = findViewById(R.id.et_log_search);
        tvLogOutput = findViewById(R.id.tv_log_output);
        btnFilterAll = findViewById(R.id.btn_filter_all);
        btnFilterInfo = findViewById(R.id.btn_filter_info);
        btnFilterWarn = findViewById(R.id.btn_filter_warn);
        btnFilterError = findViewById(R.id.btn_filter_error);
        btnLogsCopy = findViewById(R.id.btn_logs_copy);
        btnLogsExportJson = findViewById(R.id.btn_logs_export_json);

        // Settings
        rgTheme = findViewById(R.id.rg_theme);
        switchReducedMotion = findViewById(R.id.switch_reduced_motion);
        switchAnonymize = findViewById(R.id.switch_anonymize);
        btnSettingsUpdates = findViewById(R.id.btn_settings_updates);
    }

    private void setupNavigation() {
        if (bottomNav == null) return;

        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            hideAllScreens();

            if (itemId == R.id.nav_home) {
                if (screenHome != null) screenHome.setVisibility(View.VISIBLE);
                return true;
            } else if (itemId == R.id.nav_backends) {
                if (screenBackends != null) screenBackends.setVisibility(View.VISIBLE);
                return true;
            } else if (itemId == R.id.nav_device) {
                if (screenDevice != null) screenDevice.setVisibility(View.VISIBLE);
                return true;
            } else if (itemId == R.id.nav_audit) {
                if (screenAudit != null) screenAudit.setVisibility(View.VISIBLE);
                return true;
            } else if (itemId == R.id.nav_logs) {
                if (screenLogs != null) screenLogs.setVisibility(View.VISIBLE);
                refreshLogDisplay();
                return true;
            }
            return false;
        });
    }

    private void hideAllScreens() {
        if (screenHome != null) screenHome.setVisibility(View.GONE);
        if (screenBackends != null) screenBackends.setVisibility(View.GONE);
        if (screenDevice != null) screenDevice.setVisibility(View.GONE);
        if (screenAudit != null) screenAudit.setVisibility(View.GONE);
        if (screenLogs != null) screenLogs.setVisibility(View.GONE);
        if (screenSettings != null) screenSettings.setVisibility(View.GONE);
    }

    private void setupListeners() {
        // Home Navigation Shortcuts
        if (cardActivationStatus != null) {
            cardActivationStatus.setOnClickListener(v -> showActivationStatusDialog());
        }
        if (btnHomeRunAudit != null) {
            btnHomeRunAudit.setOnClickListener(v -> runFullAuditAndReport());
        }
        if (btnHomeGotoBackends != null) {
            btnHomeGotoBackends.setOnClickListener(v -> {
                if (bottomNav != null) bottomNav.setSelectedItemId(R.id.nav_backends);
            });
        }
        if (btnHomeExportReport != null) {
            btnHomeExportReport.setOnClickListener(v -> exportReportAndShowDialog());
        }
        if (btnHomeGotoSettings != null) {
            btnHomeGotoSettings.setOnClickListener(v -> openSettingsScreen());
        }
        if (btnActionSettings != null) {
            btnActionSettings.setOnClickListener(v -> openSettingsScreen());
        }

        // Backends Audit Triggers
        if (btnBackendAuditDfroot != null) {
            btnBackendAuditDfroot.setOnClickListener(v -> auditBackendWithDialog("dfroot"));
        }
        if (btnBackendAuditGhostlock != null) {
            btnBackendAuditGhostlock.setOnClickListener(v -> auditBackendWithDialog("ghostlock"));
        }

        // Logs Search and Filters
        if (etLogSearch != null) {
            etLogSearch.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int count, int after) {
                    currentLogQuery = s.toString();
                    refreshLogDisplay();
                }
                @Override public void afterTextChanged(Editable s) {}
            });
        }

        if (btnFilterAll != null) {
            btnFilterAll.setOnClickListener(v -> {
                currentLogLevel = DiagnosticLogger.Level.DEBUG;
                refreshLogDisplay();
            });
        }
        if (btnFilterInfo != null) {
            btnFilterInfo.setOnClickListener(v -> {
                currentLogLevel = DiagnosticLogger.Level.INFO;
                refreshLogDisplay();
            });
        }
        if (btnFilterWarn != null) {
            btnFilterWarn.setOnClickListener(v -> {
                currentLogLevel = DiagnosticLogger.Level.WARN;
                refreshLogDisplay();
            });
        }
        if (btnFilterError != null) {
            btnFilterError.setOnClickListener(v -> {
                currentLogLevel = DiagnosticLogger.Level.ERROR;
                refreshLogDisplay();
            });
        }

        // Logs Clipboard Copy
        if (btnLogsCopy != null) {
            btnLogsCopy.setOnClickListener(v -> {
                if (app != null) {
                    String logs = DiagnosticLogsView.renderFiltered(app.getLogger(), currentLogLevel, currentLogQuery);
                    ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    if (cm != null) {
                        ClipData clip = ClipData.newPlainText("Speed Lock Logs", logs);
                        cm.setPrimaryClip(clip);
                        Toast.makeText(this, "Diagnostic logs copied to clipboard.", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }

        // Logs Export JSON
        if (btnLogsExportJson != null) {
            btnLogsExportJson.setOnClickListener(v -> exportReportAndShowDialog());
        }

        // Settings Theme Switcher
        if (rgTheme != null) {
            rgTheme.setOnCheckedChangeListener((group, checkedId) -> {
                int targetMode;
                if (checkedId == R.id.rb_theme_dark) {
                    targetMode = AppCompatDelegate.MODE_NIGHT_YES;
                } else if (checkedId == R.id.rb_theme_light) {
                    targetMode = AppCompatDelegate.MODE_NIGHT_NO;
                } else {
                    targetMode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
                }
                if (AppCompatDelegate.getDefaultNightMode() != targetMode) {
                    AppCompatDelegate.setDefaultNightMode(targetMode);
                }
            });
        }

        // Settings Updates Action
        if (btnSettingsUpdates != null) {
            btnSettingsUpdates.setOnClickListener(v -> {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://github.com/sheikhmehraann/Speed-Lock/releases"));
                startActivity(browserIntent);
            });
        }
    }

    public void openSettingsScreen() {
        hideAllScreens();
        if (screenSettings != null) {
            screenSettings.setVisibility(View.VISIBLE);
        }
        if (bottomNav != null) {
            bottomNav.getMenu().setGroupCheckable(0, true, false);
            for (int i = 0; i < bottomNav.getMenu().size(); i++) {
                bottomNav.getMenu().getItem(i).setChecked(false);
            }
            bottomNav.getMenu().setGroupCheckable(0, true, true);
        }
    }

    @Override
    public void onBackPressed() {
        if (screenHome != null && screenHome.getVisibility() != View.VISIBLE) {
            if (bottomNav != null) {
                bottomNav.setSelectedItemId(R.id.nav_home);
            } else {
                hideAllScreens();
                screenHome.setVisibility(View.VISIBLE);
            }
            return;
        }
        super.onBackPressed();
    }

    public void initializeDashboard() {
        if (app == null) {
            app = SpeedLockApp.getInstance();
        }
        if (settingsState == null) {
            settingsState = new SettingsView.SettingsState();
        }
        app.getLogger().info("MainActivity", "Initializing Speed Lock engine and evaluating device profile...");
        this.currentProfile = DeviceDetector.detectLiveDevice();
        this.currentSummary = app.getCompatibilityEngine().evaluate(currentProfile);
        app.getLogger().info("MainActivity", "Device profile evaluated: " + currentProfile.getModel() +
            " [" + currentProfile.getBspPlatform() + "], Status: " +
            (currentSummary.hasActionableBackend ? "METADATA_COMPATIBLE" : "LIMITED_SUPPORT"));
    }

    private void updateAllScreens() {
        if (currentProfile == null || currentSummary == null) {
            return;
        }

        SocClassifier.SocClassification soc = SocClassifier.classify(currentProfile);

        // Update Home Screen
        if (cardActivationStatus != null) {
            if (currentSummary.hasActionableBackend) {
                cardActivationStatus.setBackgroundResource(R.drawable.bg_status_banner_success);
                if (ivHomeStatusWatermark != null) {
                    ivHomeStatusWatermark.setImageResource(R.drawable.ic_status_check);
                }
            } else {
                cardActivationStatus.setBackgroundResource(R.drawable.bg_status_banner_warning);
                if (ivHomeStatusWatermark != null) {
                    ivHomeStatusWatermark.setImageResource(R.drawable.ic_status_alert);
                }
            }
        }
        if (tvHomeStatusBadge != null) {
            tvHomeStatusBadge.setText(currentSummary.hasActionableBackend ? "METADATA_COMPATIBLE" : "LIMITED_SUPPORT");
        }
        if (tvHomeDeviceTitle != null) {
            tvHomeDeviceTitle.setText(currentProfile.getMarketingName() + " (" + currentProfile.getModel() + ")");
        }
        if (tvHomeKernelTitle != null) {
            tvHomeKernelTitle.setText("Kernel: " + (currentProfile.getKernelRelease().isEmpty() ? "Unidentified" : currentProfile.getKernelRelease()));
        }
        if (tvHomeRootState != null) {
            tvHomeRootState.setText("Operational Root: " + currentProfile.getOperationalRootStatus());
        }
        if (tvHomeSocName != null) {
            tvHomeSocName.setText("Commercial SoC: " + soc.commercialName);
        }
        if (tvHomeBspPlatform != null) {
            tvHomeBspPlatform.setText("Silicon BSP: " + soc.bspPlatform + " (Board: " + currentProfile.getBoard() + ")");
        }
        if (tvHomeArchInfo != null) {
            tvHomeArchInfo.setText("Architecture: " + currentProfile.getArchitecture() + " • Page Size: " +
                currentProfile.getPageSizeBytes() + " bytes • " + currentProfile.getVaBits() + "-bit VA");
        }
        if (tvHomeDataSource != null) {
            tvHomeDataSource.setText("Data Source: " + currentProfile.getFirmwareDataSource());
        }
        if (tvHomeDfrootSummary != null) {
            BackendCapability dfrootCap = currentSummary.backendCapabilities.get("dfroot");
            if (dfrootCap != null) {
                tvHomeDfrootSummary.setText("• DFRoot (" + dfrootCap.getCve() + "): " +
                    (dfrootCap.isPrerequisitesMet() ? "Prerequisites Met" : "Prerequisites Incomplete") +
                    " • " + (dfrootCap.getCriticalBlockers().isEmpty() ? "Ready" : "LKM Blocker"));
            }
        }
        if (tvHomeGhostlockSummary != null) {
            BackendCapability ghostCap = currentSummary.backendCapabilities.get("ghostlock");
            if (ghostCap != null) {
                tvHomeGhostlockSummary.setText("• GhostLock (" + ghostCap.getCve() + "): " +
                    (ghostCap.isPrerequisitesMet() ? "Prerequisites Met" : "Prerequisites Incomplete") +
                    " • " + (ghostCap.getCriticalBlockers().isEmpty() ? "Ready" : "MTK Offsets Blocker"));
            }
        }

        // Update Device Screen
        if (tvDeviceModelVal != null) tvDeviceModelVal.setText("Model: " + currentProfile.getModel());
        if (tvDeviceBrandVal != null) tvDeviceBrandVal.setText("Brand: " + currentProfile.getBrand());
        if (tvDeviceBoardVal != null) tvDeviceBoardVal.setText("Board: " + currentProfile.getBoard());
        if (tvDevicePlatformVal != null) tvDevicePlatformVal.setText("SoC / BSP: " + soc.commercialName + " [" + soc.bspPlatform + "]");
        if (tvDeviceKernelVal != null) tvDeviceKernelVal.setText("Kernel: " + (currentProfile.getKernelRelease().isEmpty() ? "Not Accessible" : currentProfile.getKernelRelease()));
        if (tvDeviceSecurityVal != null) tvDeviceSecurityVal.setText("Security Patch: " + (currentProfile.getAvbSecurityPatch().isEmpty() ? "Not Accessible" : currentProfile.getAvbSecurityPatch()));
        if (tvDeviceDatasourceVal != null) tvDeviceDatasourceVal.setText("Profile Source: " + currentProfile.getFirmwareDataSource());

        refreshLogDisplay();
    }

    public void showActivationStatusDialog() {
        ensureInitialized();
        StringBuilder sb = new StringBuilder();
        sb.append("OPERATIONAL ROOT STATUS: NOT OBTAINED\n");
        sb.append("Execution Mode: Unprivileged Diagnostic & Audit\n\n");
        sb.append("FIRMWARE & ARCHITECTURE:\n");
        sb.append("• Model: ").append(currentProfile.getModel()).append(" (").append(currentProfile.getMarketingName()).append(")\n");
        sb.append("• Kernel: ").append(currentProfile.getKernelRelease().isEmpty() ? "Unidentified" : currentProfile.getKernelRelease()).append("\n");
        sb.append("• Silicon / BSP: ").append(currentProfile.getBspPlatform()).append("\n");
        sb.append("• Architecture: ").append(currentProfile.getArchitecture()).append(" (").append(currentProfile.getPageSizeBytes()).append(" B page, ").append(currentProfile.getVaBits()).append("-bit VA)\n\n");
        sb.append("OPERATIONAL BLOCKERS IDENTIFIED:\n");
        sb.append("1. DFRoot (CVE-2026-43284):\n");
        sb.append("   Prebuilt LKM ABI mismatch (vermagic '5.10.252-dirty' vs stock target kernel). Requires custom compilation against ACK 5.10 commit f82f7360927e.\n\n");
        sb.append("2. GhostLock (CVE-2026-43499):\n");
        sb.append("   Missing MediaTek physical load offsets (kernel_phys_load/offset) and target.h struct offsets for build ab14119954.\n\n");
        sb.append("3. System Security Enforcement:\n");
        sb.append("   Android 15 SELinux (enforcing) and AVB 2.0 prevent in-place execution without verified weaponized modules.\n\n");
        sb.append("Note: Speed Lock strictly refuses to simulate exploit execution. Tap 'View Audit' to review tier checks.");

        new AlertDialog.Builder(this)
            .setTitle("Activation Status: " + (currentSummary.hasActionableBackend ? "METADATA_COMPATIBLE" : "LIMITED_SUPPORT"))
            .setMessage(sb.toString())
            .setPositiveButton("View Audit", (dialog, which) -> {
                if (bottomNav != null) bottomNav.setSelectedItemId(R.id.nav_audit);
            })
            .setNeutralButton("Manage Backends", (dialog, which) -> {
                if (bottomNav != null) bottomNav.setSelectedItemId(R.id.nav_backends);
            })
            .setNegativeButton("Close", null)
            .show();
    }

    public void runFullAuditAndReport() {
        if (app == null) app = SpeedLockApp.getInstance();
        DiagnosticLogger logger = app.getLogger();

        logger.info("AuditEngine", "==================================================");
        logger.info("AuditEngine", "Initiating Full 5-Tier Compatibility Audit...");
        logger.info("AuditEngine", "==================================================");

        // Re-detect live device
        this.currentProfile = DeviceDetector.detectLiveDevice();
        SocClassifier.SocClassification soc = SocClassifier.classify(currentProfile);

        // Tier 1: Hardware & Architecture
        logger.info("AuditEngine", "[Tier 1] Hardware & Arch: Model=" + currentProfile.getModel() +
            ", Brand=" + currentProfile.getBrand() + ", Board=" + currentProfile.getBoard() +
            ", SoC=" + soc.commercialName + " (" + soc.bspPlatform + ")" +
            ", Arch=" + currentProfile.getArchitecture() +
            ", PageSize=" + currentProfile.getPageSizeBytes() + "B, VA=" + currentProfile.getVaBits() + "-bit");

        // Tier 2: Kernel Configuration
        Map<String, String> cfg = currentProfile.getConfigFlags();
        logger.info("AuditEngine", "[Tier 2] Kernel Config: Release=" + (currentProfile.getKernelRelease().isEmpty() ? "Unidentified" : currentProfile.getKernelRelease()) +
            ", XFRM=" + cfg.get("CONFIG_XFRM") + ", ESP=" + cfg.get("CONFIG_INET_ESP") +
            ", MODULES=" + cfg.get("CONFIG_MODULES") + ", KPROBES=" + cfg.get("CONFIG_KPROBES") +
            ", FUTEX=" + cfg.get("CONFIG_FUTEX"));

        // Tier 3: Vendor Filesystem & Symlinks
        logger.info("AuditEngine", "[Tier 3] Vendor Filesystem: insmod=" + (currentProfile.hasInsmodSymlink() ? "VERIFIED" : "MISSING") +
            ", CandidateLibs=" + currentProfile.getCandidateLibraries().size() +
            " (Primary: " + (currentProfile.getCandidateLibraries().isEmpty() ? "None" : currentProfile.getCandidateLibraries().get(0)) + ")");

        // Tier 4: Exploit Prerequisites
        this.currentSummary = app.getCompatibilityEngine().evaluate(currentProfile);
        for (Map.Entry<String, BackendCapability> entry : currentSummary.backendCapabilities.entrySet()) {
            BackendCapability cap = entry.getValue();
            logger.info("AuditEngine", "[Tier 4] Backend '" + cap.getBackendId() + "' (" + cap.getCve() + "): State=" +
                cap.getState().name() + ", PrerequisitesMet=" + cap.isPrerequisitesMet());
            for (String r : cap.getReasons()) {
                logger.info("AuditEngine", "  -> Verified: " + r);
            }
            for (String b : cap.getCriticalBlockers()) {
                logger.warn("AuditEngine", "  -> Blocker: " + b);
            }
        }

        // Tier 5: Operational Root State
        logger.warn("AuditEngine", "[Tier 5] Operational Root: " + currentProfile.getOperationalRootStatus() +
            " (System enforcing, live payload armed=false)");
        logger.info("AuditEngine", "Full 5-tier compatibility audit completed. Overall Status: " +
            (currentSummary.hasActionableBackend ? "METADATA_COMPATIBLE" : "LIMITED_SUPPORT"));

        // Refresh UI across all views
        updateAllScreens();

        Toast.makeText(this, "Audit Complete: 5 Tiers Evaluated (" +
            (currentSummary.hasActionableBackend ? "METADATA_COMPATIBLE" : "LIMITED_SUPPORT") + ")",
            Toast.LENGTH_LONG).show();

        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.nav_audit);
        }
    }

    public void auditBackendWithDialog(String backendId) {
        ensureInitialized();
        if (app == null) app = SpeedLockApp.getInstance();
        DiagnosticLogger logger = app.getLogger();

        IRootBackend backend = app.getBackendRegistry().getBackend(backendId);
        if (backend == null) {
            Toast.makeText(this, "Backend not found: " + backendId, Toast.LENGTH_SHORT).show();
            return;
        }

        BackendCapability cap = backend.evaluateCompatibility(currentProfile);

        // Structured logging
        String tag = backend.getId().toUpperCase() + "_Audit";
        logger.info(tag, "--------------------------------------------------");
        logger.info(tag, "Auditing " + backend.getDisplayName() + " on " + currentProfile.getModel());
        logger.info(tag, "State: " + cap.getState().name() + " | Prerequisites Met: " + cap.isPrerequisitesMet());
        for (String reason : cap.getReasons()) {
            logger.info(tag, "PASS: " + reason);
        }
        for (String blocker : cap.getCriticalBlockers()) {
            logger.warn(tag, "BLOCKER: " + blocker);
        }
        logger.info(tag, "--------------------------------------------------");

        // Build modal dialog
        StringBuilder sb = new StringBuilder();
        sb.append("CVE: ").append(cap.getCve()).append("\n");
        sb.append("Evaluation: ").append(cap.getState().name());
        if (cap.isPrerequisitesMet()) {
            sb.append(" (Prerequisites Met)\n\n");
        } else {
            sb.append(" (Prerequisites Incomplete)\n\n");
        }

        sb.append("VERIFIED PREREQUISITES:\n");
        if (cap.getReasons().isEmpty()) {
            sb.append("• No prerequisites verified\n");
        } else {
            for (String r : cap.getReasons()) {
                sb.append("✓ ").append(r).append("\n");
            }
        }

        sb.append("\nCRITICAL OPERATIONAL BLOCKERS:\n");
        if (cap.getCriticalBlockers().isEmpty()) {
            sb.append("• None identified\n");
        } else {
            for (String b : cap.getCriticalBlockers()) {
                sb.append("✗ ").append(b).append("\n");
            }
        }

        sb.append("\nOPERATIONAL STATUS:\n");
        sb.append("NOT OBTAINED (Unprivileged Diagnostic Mode).\n");
        sb.append("Speed Lock detects architectural alignment without arming unsafe exploits.");

        new AlertDialog.Builder(this)
            .setTitle(backend.getDisplayName())
            .setMessage(sb.toString())
            .setPositiveButton("OK", null)
            .setNeutralButton("View in Logs", (dialog, which) -> {
                if (bottomNav != null) {
                    bottomNav.setSelectedItemId(R.id.nav_logs);
                }
            })
            .show();

        refreshLogDisplay();
    }

    private void refreshLogDisplay() {
        if (app == null || tvLogOutput == null) return;
        String logs = DiagnosticLogsView.renderFiltered(app.getLogger(), currentLogLevel, currentLogQuery);
        tvLogOutput.setText(logs);
    }

    private void exportReportAndShowDialog() {
        try {
            ensureInitialized();
            String reportJson = ReportExporter.exportToJson(currentSummary);

            File exportDir = getExternalFilesDir(null);
            if (exportDir == null) {
                exportDir = getFilesDir();
            }
            File outFile = new File(exportDir, "speedlock_diagnostic_report.json");
            byte[] bytes = reportJson.getBytes(StandardCharsets.UTF_8);

            try (FileOutputStream fos = new FileOutputStream(outFile)) {
                fos.write(bytes);
            }

            // Calculate SHA-256
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(bytes);
            StringBuilder hashBuilder = new StringBuilder();
            for (byte b : hashBytes) {
                hashBuilder.append(String.format("%02x", b));
            }
            String sha256 = hashBuilder.toString();

            if (app != null) {
                app.getLogger().info("ReportExporter", "Exported JSON report to " + outFile.getAbsolutePath() + " [SHA-256: " + sha256 + "]");
            }

            // Show Dialog
            new AlertDialog.Builder(this)
                .setTitle("Diagnostic Report Exported")
                .setMessage("File: " + outFile.getName() + "\n" +
                    "Size: " + bytes.length + " bytes\n\n" +
                    "SHA-256 Checksum:\n" + sha256 + "\n\n" +
                    "Path:\n" + outFile.getAbsolutePath())
                .setPositiveButton("OK", null)
                .show();

        } catch (Exception e) {
            Toast.makeText(this, "Export failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    public DeviceProfile getCurrentProfile() {
        if (currentProfile == null) initializeDashboard();
        return currentProfile;
    }

    public CompatibilityEngine.CompatibilitySummary getCurrentSummary() {
        if (currentSummary == null) initializeDashboard();
        return currentSummary;
    }

    public SettingsView.SettingsState getSettingsState() {
        if (settingsState == null) settingsState = new SettingsView.SettingsState();
        return settingsState;
    }

    // Headless screen rendering API for unit testing and CLI export
    public String renderHomeScreen() {
        ensureInitialized();
        return "SPEED LOCK DASHBOARD\nDevice: " + currentProfile.getModel() + "\nStatus: " +
            (currentSummary.hasActionableBackend ? "METADATA_COMPATIBLE" : "LIMITED_SUPPORT");
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
        if (app == null) app = SpeedLockApp.getInstance();
        return DiagnosticLogsView.renderFiltered(app.getLogger(), filter, query);
    }

    public String renderSettingsScreen() {
        if (settingsState == null) settingsState = new SettingsView.SettingsState();
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

    private void ensureInitialized() {
        if (currentSummary == null || currentProfile == null) {
            initializeDashboard();
        }
    }
}
