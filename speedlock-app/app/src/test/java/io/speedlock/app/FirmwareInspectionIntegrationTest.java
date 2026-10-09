package io.speedlock.app;

import io.speedlock.app.cli.SpeedLockCli;
import io.speedlock.app.model.DeviceProfile;
import java.io.File;

public class FirmwareInspectionIntegrationTest {

    public void testInspectArchivedFirmware() {
        File fwDir = new File("C:/Users/Admin/Videos/Github/Speed Lock/research/firmware/X6871");
        if (!fwDir.exists()) {
            System.out.println("    [SKIP] Firmware directory not found: " + fwDir.getPath());
            return;
        }

        DeviceProfile prof = SpeedLockCli.inspectFirmwareDirectory(fwDir);

        SpeedLockTestRunner.assertEquals("Infinix", prof.getBrand(), "Firmware brand mismatch");
        SpeedLockTestRunner.assertEquals("X6871", prof.getModel(), "Firmware model mismatch");
        SpeedLockTestRunner.assertEquals("Infinix GT 20 Pro", prof.getMarketingName(), "Firmware marketing name mismatch");
        SpeedLockTestRunner.assertEquals("MT6895", prof.getBspPlatform(), "Firmware BSP platform mismatch");
        SpeedLockTestRunner.assertEquals("MediaTek Dimensity 8200 Ultimate (MT6896)", prof.getCommercialSoc(), "Firmware commercial SoC mismatch");
        SpeedLockTestRunner.assertEquals("x6871_h962", prof.getBoard(), "Firmware board mismatch");
        SpeedLockTestRunner.assertEquals("2026-07-01", prof.getAvbSecurityPatch(), "Firmware security patch mismatch");

        SpeedLockTestRunner.assertTrue(prof.getKernelRelease().startsWith("5.10.237-android12-9"), "Kernel release mismatch");
        SpeedLockTestRunner.assertTrue(prof.getVermagic().contains("modversions aarch64"), "Vermagic mismatch");
        SpeedLockTestRunner.assertEquals("y", prof.getConfigFlags().get("CONFIG_XFRM"), "CONFIG_XFRM mismatch");
        SpeedLockTestRunner.assertEquals("is not set", prof.getConfigFlags().get("CONFIG_MODULE_SIG"), "CONFIG_MODULE_SIG mismatch");

        SpeedLockTestRunner.assertTrue(prof.getCandidateLibraries().contains("/vendor/lib64/libbinderdebug.so"), "Binderdebug candidate library missing");
        SpeedLockTestRunner.assertTrue(prof.hasInsmodSymlink(), "Insmod symlink flag should be true");
        SpeedLockTestRunner.assertTrue(prof.hasSuperPartition(), "Super partition flag should be true");
    }
}
