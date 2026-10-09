package io.speedlock.app;

import io.speedlock.app.detector.DeviceDetector;
import io.speedlock.app.model.DeviceProfile;
import java.util.HashMap;
import java.util.Map;

public class DeviceDetectorTest {

    public void testFromPropertiesX6871() {
        Map<String, String> props = new HashMap<>();
        props.put("ro.product.vendor.brand", "Infinix");
        props.put("ro.product.vendor.model", "Infinix X6871");
        props.put("ro.vendor.mediatek.platform", "MT6895");
        props.put("ro.build.product", "x6871_h962");
        props.put("ro.build.display.id", "X6871-H962CF-U-BASE-260618V1066DevT");
        props.put("ro.build.version.security_patch", "2026-07-01");

        String release = "5.10.237-android12-9-00014-gf82f7360927e-ab14119954";
        String vm = "5.10.237-android12-9-00014-gf82f7360927e-ab14119954 SMP preempt mod_unload modversions aarch64";

        DeviceProfile prof = DeviceDetector.fromProperties(props, release, vm);

        SpeedLockTestRunner.assertEquals("Infinix", prof.getBrand(), "Brand mismatch");
        SpeedLockTestRunner.assertEquals("X6871", prof.getModel(), "Model normalization failed");
        SpeedLockTestRunner.assertEquals("Infinix GT 20 Pro", prof.getMarketingName(), "Marketing name mismatch");
        SpeedLockTestRunner.assertEquals("MT6895", prof.getBspPlatform(), "BSP platform mismatch");
        SpeedLockTestRunner.assertEquals("MediaTek Dimensity 8200 Ultimate (MT6896)", prof.getCommercialSoc(), "Commercial SoC mismatch");
        SpeedLockTestRunner.assertEquals("x6871_h962", prof.getBoard(), "Board mismatch");
        SpeedLockTestRunner.assertEquals("2026-07-01", prof.getAvbSecurityPatch(), "Security patch mismatch");
        SpeedLockTestRunner.assertEquals("android12-9", prof.getKmiGeneration(), "KMI generation mismatch");
        SpeedLockTestRunner.assertTrue(prof.isGki510(), "Expected isGki510 to be true");
        SpeedLockTestRunner.assertTrue(prof.isX6871(), "Expected isX6871 to be true");
    }

    public void testGenericDeviceProperties() {
        Map<String, String> props = new HashMap<>();
        props.put("ro.product.vendor.brand", "Google");
        props.put("ro.product.vendor.model", "Pixel 7");
        props.put("ro.board.platform", "gs201");

        DeviceProfile prof = DeviceDetector.fromProperties(props, "5.10.157-android12-9", "");
        SpeedLockTestRunner.assertEquals("Google", prof.getBrand(), "Brand mismatch");
        SpeedLockTestRunner.assertEquals("Pixel 7", prof.getModel(), "Model mismatch");
        SpeedLockTestRunner.assertFalse(prof.isX6871(), "Expected isX6871 to be false for Pixel");
    }
}
