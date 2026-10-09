package io.speedlock.app.ui;

import io.speedlock.app.detector.SocClassifier;
import io.speedlock.app.model.DeviceProfile;

/**
 * Renders the Device Information screen, displaying detected model,
 * MediaTek BSP platform, commercial SoC, kernel release, and security patch level.
 */
public class DeviceInfoView {

    public static String render(DeviceProfile profile) {
        SocClassifier.SocClassification soc = SocClassifier.classify(profile);
        StringBuilder sb = new StringBuilder();
        sb.append("=== DEVICE IDENTIFICATION ===\n");
        sb.append("Brand:              ").append(profile.getBrand()).append("\n");
        sb.append("Model:              ").append(profile.getModel()).append("\n");
        sb.append("Marketing Name:     ").append(profile.getMarketingName()).append("\n");
        sb.append("Commercial SoC:     ").append(soc.commercialName).append("\n");
        sb.append("BSP Platform Code:  ").append(soc.bspPlatform).append("\n");
        sb.append("Board / Product:    ").append(profile.getBoard()).append("\n");
        sb.append("Kernel Release:     ").append(profile.getKernelRelease()).append("\n");
        sb.append("Architecture:       ").append(profile.getArchitecture()).append("\n");
        sb.append("Page Size:          ").append(profile.getPageSizeBytes()).append(" bytes\n");
        sb.append("Security Patch:     ").append(profile.getAvbSecurityPatch()).append("\n");
        sb.append("\n=== SILICON ARCHITECTURE NOTICE ===\n");
        sb.append(soc.technicalExplanation).append("\n");
        return sb.toString();
    }
}
