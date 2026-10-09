package io.speedlock.app;

import io.speedlock.app.backend.BackendRegistry;
import io.speedlock.app.diagnostic.CompatibilityEngine;
import io.speedlock.app.diagnostic.ReportExporter;
import io.speedlock.app.model.DeviceProfile;

public class ReportExporterTest {

    public void testExportToJson() {
        BackendRegistry registry = new BackendRegistry();
        CompatibilityEngine engine = new CompatibilityEngine(registry);

        DeviceProfile prof = new DeviceProfile();
        prof.setBrand("Infinix");
        prof.setModel("X6871");
        prof.setBspPlatform("MT6895");
        prof.setKernelRelease("5.10.237-android12-9");

        CompatibilityEngine.CompatibilitySummary summary = engine.evaluate(prof);
        String json = ReportExporter.exportToJson(summary);

        SpeedLockTestRunner.assertTrue(json.contains("\"brand\": \"Infinix\""), "JSON missing brand");
        SpeedLockTestRunner.assertTrue(json.contains("\"commercial_soc\": \"MediaTek Dimensity 8200 Ultimate (MT6896)\""), "JSON missing commercial SoC");
        SpeedLockTestRunner.assertTrue(json.contains("\"bsp_platform\": \"MT6895\""), "JSON missing BSP platform");
        SpeedLockTestRunner.assertTrue(json.contains("\"dfroot\":"), "JSON missing dfroot");
        SpeedLockTestRunner.assertTrue(json.contains("\"operational_verdict\":"), "JSON missing operational verdict");
    }

    public void testExportToMarkdown() {
        BackendRegistry registry = new BackendRegistry();
        CompatibilityEngine engine = new CompatibilityEngine(registry);

        DeviceProfile prof = new DeviceProfile();
        prof.setBrand("Infinix");
        prof.setModel("X6871");
        prof.setBspPlatform("MT6895");
        prof.setKernelRelease("5.10.237-android12-9");

        CompatibilityEngine.CompatibilitySummary summary = engine.evaluate(prof);
        String md = ReportExporter.exportToMarkdown(summary);

        SpeedLockTestRunner.assertTrue(md.contains("# Speed Lock -- Device Diagnostic Report"), "Markdown missing header");
        SpeedLockTestRunner.assertTrue(md.contains("MediaTek Platform Analysis"), "Markdown missing platform section");
        SpeedLockTestRunner.assertTrue(md.contains("Epistemological Integrity Notice"), "Markdown missing honesty notice");
    }
}
