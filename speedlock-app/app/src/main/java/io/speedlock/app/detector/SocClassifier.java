package io.speedlock.app.detector;

import io.speedlock.app.model.DeviceProfile;

/**
 * Resolves MediaTek SoC platform identity, specifically classifying the relationship
 * between the internal silicon BSP platform (MT6895) and the commercial part (MT6896).
 */
public class SocClassifier {

    public static class SocClassification {
        public final String bspPlatform;
        public final String commercialName;
        public final String technicalExplanation;
        public final boolean isDimensity8200Series;

        public SocClassification(String bspPlatform, String commercialName, String technicalExplanation, boolean isDimensity8200Series) {
            this.bspPlatform = bspPlatform;
            this.commercialName = commercialName;
            this.technicalExplanation = technicalExplanation;
            this.isDimensity8200Series = isDimensity8200Series;
        }
    }

    /**
     * Classifies SoC platform based on detected properties and kernel artifacts.
     */
    public static SocClassification classify(DeviceProfile profile) {
        String platform = profile.getBspPlatform();
        if (platform == null || platform.isEmpty()) {
            platform = profile.getSystemProperties().getOrDefault("ro.vendor.mediatek.platform", "Unknown");
        }

        String board = profile.getBoard();
        String model = profile.getModel();

        if ("MT6895".equalsIgnoreCase(platform) || "x6871_h962".equalsIgnoreCase(board) || (model != null && model.contains("X6871"))) {
            return new SocClassification(
                "MT6895",
                "MediaTek Dimensity 8200 Ultimate (MT6896)",
                "MediaTek designates the underlying silicon architecture and BSP drivers as MT6895 " +
                "(shared across Dimensity 8100/8200 series, e.g. pinctrl-mt6895.ko, clk-dbg-mt6895.ko, " +
                "and MT6895_Android_scatter.xml). Commercial retail marketing identifies the 4nm enhanced part " +
                "in Infinix GT 20 Pro as MT6896 / Dimensity 8200 Ultimate.",
                true
            );
        }

        if (platform.startsWith("MT")) {
            return new SocClassification(
                platform,
                "MediaTek " + platform,
                "Standard MediaTek SoC platform.",
                false
            );
        }

        return new SocClassification(
            platform,
            "Unknown SoC",
            "Unidentified SoC architecture.",
            false
        );
    }
}
