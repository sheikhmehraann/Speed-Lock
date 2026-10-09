package io.speedlock.app.detector;

import io.speedlock.app.model.DeviceProfile;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Detects device hardware, OS properties, and kernel release strings from
 * either the live Android system (/proc, getprop) or extracted firmware artifacts.
 */
public class DeviceDetector {

    public static DeviceProfile detectLiveDevice() {
        DeviceProfile profile = new DeviceProfile();

        // 1. Read /proc/version
        String procVersion = readFirstLine("/proc/version");
        if (!procVersion.isEmpty()) {
            parseKernelVersion(procVersion, profile);
        }

        // 2. Read live system properties via getprop or Android Build
        readSystemProperty("ro.product.brand", v -> profile.setBrand(v));
        readSystemProperty("ro.product.model", v -> profile.setModel(v));
        readSystemProperty("ro.product.board", v -> profile.setBoard(v));
        readSystemProperty("ro.vendor.mediatek.platform", v -> profile.setBspPlatform(v));
        readSystemProperty("ro.build.display.id", v -> profile.setBuildDisplayId(v));
        readSystemProperty("ro.build.version.security_patch", v -> profile.setAvbSecurityPatch(v));

        // 3. Classify SoC
        SocClassifier.SocClassification soc = SocClassifier.classify(profile);
        profile.setBspPlatform(soc.bspPlatform);
        profile.setCommercialSoc(soc.commercialName);

        // 4. Default architecture and page size
        profile.setArchitecture(System.getProperty("os.arch", "arm64"));
        profile.setPageSizeBytes(4096);
        profile.setVaBits(39);

        // 5. Inspect candidate library presence
        if (new File("/vendor/lib64/libbinderdebug.so").exists()) {
            profile.getCandidateLibraries().add("/vendor/lib64/libbinderdebug.so");
        }
        if (new File("/vendor/bin/insmod").exists()) {
            profile.setHasInsmodSymlink(true);
        }

        return profile;
    }

    public static DeviceProfile fromProperties(Map<String, String> props, String kernelRelease, String vermagic) {
        DeviceProfile profile = new DeviceProfile();
        profile.getSystemProperties().putAll(props);

        String brand = props.getOrDefault("ro.product.vendor.brand", props.getOrDefault("ro.product.system.brand", "Unknown"));
        if ("alps".equalsIgnoreCase(brand) && props.containsKey("ro.product.vendor.brand")) {
            brand = props.get("ro.product.vendor.brand");
        }
        profile.setBrand(brand);

        String model = props.getOrDefault("ro.product.vendor.model", props.getOrDefault("ro.product.system.model", "Unknown"));
        if (model.startsWith(brand + " ")) {
            model = model.substring(brand.length() + 1);
        }
        profile.setModel(model);

        profile.setBoard(props.getOrDefault("ro.build.product", props.getOrDefault("ro.product.board", "Unknown")));
        profile.setBspPlatform(props.getOrDefault("ro.vendor.mediatek.platform", ""));
        profile.setBuildDisplayId(props.getOrDefault("ro.build.display.id", ""));
        profile.setBuildFingerprint(props.getOrDefault("ro.vendor.build.fingerprint", props.getOrDefault("ro.system.build.fingerprint", "")));
        profile.setAvbSecurityPatch(props.getOrDefault("ro.build.version.security_patch", ""));

        // Classify SoC
        SocClassifier.SocClassification soc = SocClassifier.classify(profile);
        profile.setBspPlatform(soc.bspPlatform);
        profile.setCommercialSoc(soc.commercialName);

        if ("Infinix".equalsIgnoreCase(profile.getBrand()) && profile.getModel().contains("X6871")) {
            profile.setMarketingName("Infinix GT 20 Pro");
        }

        if (kernelRelease != null && !kernelRelease.isEmpty()) {
            profile.setKernelRelease(kernelRelease);
            Matcher mKmi = Pattern.compile("android\\d+-\\d+").matcher(kernelRelease);
            if (mKmi.find()) {
                profile.setKmiGeneration(mKmi.group(0));
            }
        }
        if (vermagic != null && !vermagic.isEmpty()) {
            profile.setVermagic(vermagic);
        }

        return profile;
    }

    private static void parseKernelVersion(String versionString, DeviceProfile profile) {
        Matcher m = Pattern.compile("Linux version ([0-9]+\\.[0-9]+\\.[0-9]+-android[0-9]+-[^\\s]+)").matcher(versionString);
        if (m.find()) {
            profile.setKernelRelease(m.group(1));
            Matcher mKmi = Pattern.compile("android\\d+-\\d+").matcher(profile.getKernelRelease());
            if (mKmi.find()) {
                profile.setKmiGeneration(mKmi.group(0));
            }
        }
    }

    private static String readFirstLine(String path) {
        File f = new File(path);
        if (!f.exists() || !f.canRead()) return "";
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8))) {
            String line = reader.readLine();
            return line != null ? line.trim() : "";
        } catch (Exception ignored) {
            return "";
        }
    }

    private interface PropertyConsumer {
        void accept(String val);
    }

    private static void readSystemProperty(String key, PropertyConsumer consumer) {
        try {
            Process p = Runtime.getRuntime().exec(new String[]{"getprop", key});
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                String line = reader.readLine();
                if (line != null && !line.trim().isEmpty()) {
                    consumer.accept(line.trim());
                }
            }
        } catch (Exception ignored) {
            // Live getprop may fail on non-Android or sandboxed test runs; handled safely
        }
    }
}
