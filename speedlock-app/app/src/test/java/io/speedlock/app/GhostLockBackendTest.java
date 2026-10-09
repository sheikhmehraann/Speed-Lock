package io.speedlock.app;

import io.speedlock.app.backend.GhostLockBackend;
import io.speedlock.app.model.BackendCapability;
import io.speedlock.app.model.BackendState;
import io.speedlock.app.model.DeviceProfile;

public class GhostLockBackendTest {

    public void testGhostLockMetadataCompatible() {
        GhostLockBackend backend = new GhostLockBackend();
        DeviceProfile prof = new DeviceProfile();
        prof.setArchitecture("arm64");
        prof.getConfigFlags().put("CONFIG_FUTEX", "y");
        prof.setPageSizeBytes(4096);
        prof.setVaBits(39);

        BackendCapability cap = backend.evaluateCompatibility(prof);
        SpeedLockTestRunner.assertEquals(BackendState.METADATA_COMPATIBLE, cap.getState(), "Expected METADATA_COMPATIBLE");
        SpeedLockTestRunner.assertTrue(cap.isPrerequisitesMet(), "Prerequisites should be met");
        // Must contain MediaTek physical load blocker
        boolean hasMtkBlocker = false;
        for (String b : cap.getCriticalBlockers()) {
            if (b.contains("MediaTek physical load addresses")) {
                hasMtkBlocker = true;
                break;
            }
        }
        SpeedLockTestRunner.assertTrue(hasMtkBlocker, "Expected MediaTek physical address blocker");
    }

    public void testGhostLockUnsupportedMissingFutex() {
        GhostLockBackend backend = new GhostLockBackend();
        DeviceProfile prof = new DeviceProfile();
        prof.setArchitecture("arm64");
        prof.getConfigFlags().put("CONFIG_FUTEX", "is not set");

        BackendCapability cap = backend.evaluateCompatibility(prof);
        SpeedLockTestRunner.assertEquals(BackendState.UNSUPPORTED, cap.getState(), "Expected UNSUPPORTED when CONFIG_FUTEX missing");
    }
}
