package io.speedlock.app;

import io.speedlock.app.backend.BackendRegistry;
import io.speedlock.app.backend.IRootBackend;
import io.speedlock.app.model.BackendCapability;
import io.speedlock.app.model.DeviceProfile;
import java.util.List;
import java.util.Map;

public class BackendRegistryTest {

    public void testDefaultBackendsRegistered() {
        BackendRegistry reg = new BackendRegistry();
        List<IRootBackend> all = reg.getAllBackends();
        SpeedLockTestRunner.assertTrue(all.size() >= 4, "Expected at least 4 default backends");

        SpeedLockTestRunner.assertNotNull(reg.getBackend("dfroot"), "Expected dfroot backend");
        SpeedLockTestRunner.assertNotNull(reg.getBackend("ghostlock"), "Expected ghostlock backend");
        SpeedLockTestRunner.assertNotNull(reg.getBackend("dirtyinit"), "Expected dirtyinit backend");
        SpeedLockTestRunner.assertNotNull(reg.getBackend("uniroot"), "Expected uniroot backend");
    }

    public void testLookupByCve() {
        BackendRegistry reg = new BackendRegistry();
        List<IRootBackend> cve43284 = reg.getByCve("CVE-2026-43284");
        SpeedLockTestRunner.assertTrue(cve43284.size() >= 2, "Expected at least 2 backends for CVE-2026-43284");

        List<IRootBackend> cve43499 = reg.getByCve("CVE-2026-43499");
        SpeedLockTestRunner.assertTrue(cve43499.size() >= 1, "Expected at least 1 backend for CVE-2026-43499");
    }

    public void testEvaluateAllProducesResults() {
        BackendRegistry reg = new BackendRegistry();
        DeviceProfile prof = new DeviceProfile();
        prof.setArchitecture("arm64");
        prof.setKernelRelease("5.10.237-android12-9");

        Map<String, BackendCapability> results = reg.evaluateAll(prof);
        SpeedLockTestRunner.assertTrue(results.containsKey("dfroot"), "Expected dfroot evaluation");
        SpeedLockTestRunner.assertTrue(results.containsKey("ghostlock"), "Expected ghostlock evaluation");
    }
}
