package io.speedlock.app;

import io.speedlock.app.backend.DFRootBackend;
import io.speedlock.app.backend.GhostLockBackend;
import io.speedlock.app.detector.DeviceDetector;
import io.speedlock.app.diagnostic.CompatibilityEngine;
import io.speedlock.app.diagnostic.DiagnosticLogger;
import io.speedlock.app.model.BackendCapability;
import io.speedlock.app.model.BackendState;
import io.speedlock.app.model.DeviceProfile;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static io.speedlock.app.SpeedLockTestRunner.*;

/**
 * Tests verifying that operational root access is never simulated or falsely reported,
 * that critical blockers are extracted and communicated, and that firmware data sources
 * are accurately separated from unprivileged live system properties.
 */
public class OperationalRootIntegrityTest {

    public void testRootStatusNeverClaimedAsObtained() {
        DeviceProfile profile = DeviceDetector.detectLiveDevice();
        assertNotNull(profile, "Profile should not be null");

        String rootStatus = profile.getOperationalRootStatus();
        assertTrue(rootStatus.contains("NOT_OBTAINED"), "Root status must state NOT_OBTAINED");
        assertFalse(rootStatus.contains("GRANTED"), "Root status must never claim GRANTED");
    }

    public void testX6871BackendBlockersArePreserved() {
        Map<String, String> props = new HashMap<>();
        props.put("ro.product.vendor.brand", "Infinix");
        props.put("ro.product.vendor.model", "X6871");
        props.put("ro.vendor.mediatek.platform", "MT6895");

        DeviceProfile profile = DeviceDetector.fromProperties(
            props,
            "5.10.237-android12-9-00014-gf82f7360927e-ab14119954",
            "5.10.237-android12-9-00014 SMP preempt mod_unload modversions aarch64"
        );

        profile.getConfigFlags().put("CONFIG_XFRM", "y");
        profile.getConfigFlags().put("CONFIG_INET_ESP", "y");
        profile.getConfigFlags().put("CONFIG_MODULES", "y");
        profile.getConfigFlags().put("CONFIG_KPROBES", "y");
        profile.getConfigFlags().put("CONFIG_FUTEX", "y");
        profile.getCandidateLibraries().add("/vendor/lib64/libbinderdebug.so");
        profile.setHasInsmodSymlink(true);

        io.speedlock.app.backend.BackendRegistry registry = new io.speedlock.app.backend.BackendRegistry();
        CompatibilityEngine engine = new CompatibilityEngine(registry);
        CompatibilityEngine.CompatibilitySummary summary = engine.evaluate(profile);

        assertNotNull(summary, "Summary must not be null");
        assertFalse(summary.hasActionableBackend, "Stock metadata compatibility must NOT be flagged as actionable operational root without build verification");

        // Verify DFRoot has critical blockers despite metadata compatibility
        BackendCapability dfroot = summary.backendCapabilities.get("dfroot");
        assertNotNull(dfroot, "DFRoot capability must be present");
        assertEquals(BackendState.METADATA_COMPATIBLE, dfroot.getState(), "State should be METADATA_COMPATIBLE");
        assertFalse(dfroot.getCriticalBlockers().isEmpty(), "DFRoot must have critical blockers recorded");
        boolean hasLkmBlocker = false;
        for (String blocker : dfroot.getCriticalBlockers()) {
            if (blocker.toLowerCase().contains("vermagic") || blocker.toLowerCase().contains("lkm") || blocker.toLowerCase().contains("enoexec")) {
                hasLkmBlocker = true;
                break;
            }
        }
        assertTrue(hasLkmBlocker, "DFRoot must cite vermagic / LKM ABI blocker");

        // Verify GhostLock has critical blockers recorded
        BackendCapability ghost = summary.backendCapabilities.get("ghostlock");
        assertNotNull(ghost, "GhostLock capability must be present");
        assertEquals(BackendState.METADATA_COMPATIBLE, ghost.getState(), "State should be METADATA_COMPATIBLE");
        assertFalse(ghost.getCriticalBlockers().isEmpty(), "GhostLock must have critical blockers recorded");
        boolean hasPhysBlocker = false;
        for (String blocker : ghost.getCriticalBlockers()) {
            if (blocker.toLowerCase().contains("phys") || blocker.toLowerCase().contains("load") || blocker.toLowerCase().contains("offsets")) {
                hasPhysBlocker = true;
                break;
            }
        }
        assertTrue(hasPhysBlocker, "GhostLock must cite physical offset / load address blocker");
    }

    public void testNegativeUnsupportedDeviceHandling() {
        DeviceProfile unsupported = new DeviceProfile();
        unsupported.setArchitecture("x86_64"); // Non-arm64
        unsupported.setModel("Emulator_x86");

        DFRootBackend dfroot = new DFRootBackend();
        BackendCapability cap = dfroot.evaluateCompatibility(unsupported);
        assertEquals(BackendState.UNSUPPORTED, cap.getState(), "x86_64 must be rejected as UNSUPPORTED");
        assertFalse(cap.getCriticalBlockers().isEmpty(), "Must list architecture blocker");

        GhostLockBackend ghost = new GhostLockBackend();
        BackendCapability ghostCap = ghost.evaluateCompatibility(unsupported);
        assertEquals(BackendState.UNSUPPORTED, ghostCap.getState(), "x86_64 must be rejected as UNSUPPORTED");
    }

    public void testFirmwareDataSourceLabeling() {
        DeviceProfile profile = new DeviceProfile();
        profile.setModel("X6871");
        profile.setKernelRelease("5.10.237-android12-9-00014");

        // Before loading catalog
        assertEquals("Unprivileged Userspace (No Live /proc/config.gz)", profile.getFirmwareDataSource(), "Default source");

        // Simulate catalog attachment
        profile.setFirmwareDataSource("Stock X6871 Firmware Catalog (5.10.237-android12-9-00014)");
        profile.setFirmwareConfigLoaded(true);

        assertTrue(profile.isFirmwareConfigLoaded(), "Catalog must be marked as loaded");
        assertTrue(profile.getFirmwareDataSource().contains("Stock X6871"), "Source must explicitly cite Stock catalog");
    }
}
