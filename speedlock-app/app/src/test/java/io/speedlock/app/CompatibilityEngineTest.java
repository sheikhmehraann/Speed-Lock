package io.speedlock.app;

import io.speedlock.app.backend.BackendRegistry;
import io.speedlock.app.diagnostic.CompatibilityEngine;
import io.speedlock.app.model.DeviceProfile;

public class CompatibilityEngineTest {

    public void testEvaluateX6871StockProfile() {
        BackendRegistry registry = new BackendRegistry();
        CompatibilityEngine engine = new CompatibilityEngine(registry);

        DeviceProfile prof = new DeviceProfile();
        prof.setBrand("Infinix");
        prof.setModel("X6871");
        prof.setBspPlatform("MT6895");
        prof.setBoard("x6871_h962");
        prof.setArchitecture("arm64");
        prof.setPageSizeBytes(4096);
        prof.setVaBits(39);
        prof.setKernelRelease("5.10.237-android12-9-00014-gf82f7360927e-ab14119954");
        prof.getConfigFlags().put("CONFIG_XFRM", "y");
        prof.getConfigFlags().put("CONFIG_INET_ESP", "y");
        prof.getConfigFlags().put("CONFIG_MODULES", "y");
        prof.getConfigFlags().put("CONFIG_KPROBES", "y");
        prof.getConfigFlags().put("CONFIG_FUTEX", "y");
        prof.getConfigFlags().put("CONFIG_MODULE_SIG", "is not set");
        prof.getCandidateLibraries().add("/vendor/lib64/libbinderdebug.so");
        prof.setHasInsmodSymlink(true);

        CompatibilityEngine.CompatibilitySummary summary = engine.evaluate(prof);

        SpeedLockTestRunner.assertTrue(summary.verifiedFacts.size() >= 5, "Expected at least 5 verified facts");
        SpeedLockTestRunner.assertTrue(summary.criticalBlockers.size() > 0, "Expected critical blockers logged");
        // Epistemological truth: hasActionableBackend must be false because no compiled LKM with matching vermagic/CRCs is ready
        SpeedLockTestRunner.assertFalse(summary.hasActionableBackend, "Actionable backend should be false until physical LKM verified");
    }

    public void testEvaluateUnsupportedArchitecture() {
        BackendRegistry registry = new BackendRegistry();
        CompatibilityEngine engine = new CompatibilityEngine(registry);

        DeviceProfile prof = new DeviceProfile();
        prof.setArchitecture("x86"); // Unsupported architecture

        CompatibilityEngine.CompatibilitySummary summary = engine.evaluate(prof);
        SpeedLockTestRunner.assertTrue(summary.criticalBlockers.stream().anyMatch(b -> b.contains("x86")), "Expected x86 blocker");
    }
}
