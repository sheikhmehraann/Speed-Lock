package io.speedlock.app;

import io.speedlock.app.backend.DFRootBackend;
import io.speedlock.app.model.BackendCapability;
import io.speedlock.app.model.BackendState;
import io.speedlock.app.model.DeviceProfile;

public class DFRootBackendTest {

    public void testDFRootMetadataCompatible() {
        DFRootBackend backend = new DFRootBackend();
        DeviceProfile prof = new DeviceProfile();
        prof.setArchitecture("arm64");
        prof.getConfigFlags().put("CONFIG_XFRM", "y");
        prof.getConfigFlags().put("CONFIG_INET_ESP", "y");
        prof.getConfigFlags().put("CONFIG_MODULES", "y");
        prof.getConfigFlags().put("CONFIG_KPROBES", "y");
        prof.getCandidateLibraries().add("/vendor/lib64/libbinderdebug.so");
        prof.setHasInsmodSymlink(true);

        BackendCapability cap = backend.evaluateCompatibility(prof);
        SpeedLockTestRunner.assertEquals(BackendState.METADATA_COMPATIBLE, cap.getState(), "Expected METADATA_COMPATIBLE");
        SpeedLockTestRunner.assertTrue(cap.isPrerequisitesMet(), "Expected prerequisites met");
        SpeedLockTestRunner.assertFalse(cap.isActionable(), "Expected not actionable without compiled LKM");
        SpeedLockTestRunner.assertTrue(cap.getCriticalBlockers().size() > 0, "Expected critical blocker for prebuilt LKM");
    }

    public void testDFRootUnsupportedMissingConfig() {
        DFRootBackend backend = new DFRootBackend();
        DeviceProfile prof = new DeviceProfile();
        prof.setArchitecture("arm64");
        // Missing CONFIG_XFRM and CONFIG_INET_ESP
        prof.getConfigFlags().put("CONFIG_MODULES", "y");
        prof.getConfigFlags().put("CONFIG_KPROBES", "y");
        prof.getCandidateLibraries().add("/vendor/lib64/libbinderdebug.so");
        prof.setHasInsmodSymlink(true);

        BackendCapability cap = backend.evaluateCompatibility(prof);
        SpeedLockTestRunner.assertEquals(BackendState.UNSUPPORTED, cap.getState(), "Expected UNSUPPORTED on missing config");
        SpeedLockTestRunner.assertFalse(cap.isPrerequisitesMet(), "Prerequisites should not be met");
    }

    public void testDFRootUnsupportedMissingCandidateLib() {
        DFRootBackend backend = new DFRootBackend();
        DeviceProfile prof = new DeviceProfile();
        prof.setArchitecture("arm64");
        prof.getConfigFlags().put("CONFIG_XFRM", "y");
        prof.getConfigFlags().put("CONFIG_INET_ESP", "y");
        prof.getConfigFlags().put("CONFIG_MODULES", "y");
        prof.getConfigFlags().put("CONFIG_KPROBES", "y");
        prof.setHasInsmodSymlink(true);
        // Candidate libraries list is empty

        BackendCapability cap = backend.evaluateCompatibility(prof);
        SpeedLockTestRunner.assertEquals(BackendState.UNSUPPORTED, cap.getState(), "Expected UNSUPPORTED on missing lib");
    }
}
