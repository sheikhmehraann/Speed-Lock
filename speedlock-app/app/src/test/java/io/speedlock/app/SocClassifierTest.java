package io.speedlock.app;

import io.speedlock.app.detector.SocClassifier;
import io.speedlock.app.model.DeviceProfile;

public class SocClassifierTest {

    public void testClassifyMt6895X6871() {
        DeviceProfile prof = new DeviceProfile();
        prof.setModel("X6871");
        prof.setBspPlatform("MT6895");
        prof.setBoard("x6871_h962");

        SocClassifier.SocClassification c = SocClassifier.classify(prof);
        SpeedLockTestRunner.assertTrue(c.isDimensity8200Series, "Expected Dimensity 8200 series");
        SpeedLockTestRunner.assertEquals("MT6895", c.bspPlatform, "Expected MT6895 BSP platform");
        SpeedLockTestRunner.assertEquals("MediaTek Dimensity 8200 Ultimate (MT6896)", c.commercialName, "Expected commercial name");
        SpeedLockTestRunner.assertTrue(c.technicalExplanation.contains("pinctrl-mt6895.ko"), "Expected driver explanation");
    }

    public void testClassifyGenericMediaTek() {
        DeviceProfile prof = new DeviceProfile();
        prof.setBspPlatform("MT6765");
        SocClassifier.SocClassification c = SocClassifier.classify(prof);
        SpeedLockTestRunner.assertFalse(c.isDimensity8200Series, "Expected non-8200");
        SpeedLockTestRunner.assertEquals("MediaTek MT6765", c.commercialName, "Expected MediaTek MT6765");
    }

    public void testClassifyNonMediaTek() {
        DeviceProfile prof = new DeviceProfile();
        prof.setBspPlatform("qcom");
        SocClassifier.SocClassification c = SocClassifier.classify(prof);
        SpeedLockTestRunner.assertFalse(c.isDimensity8200Series, "Expected non-8200");
        SpeedLockTestRunner.assertEquals("Unknown SoC", c.commercialName, "Expected Unknown SoC");
    }
}
