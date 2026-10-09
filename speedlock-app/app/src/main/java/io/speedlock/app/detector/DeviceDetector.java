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

        // 1. Read Android OS Build properties via reflection (SELinux safe)
        populateAndroidBuildProperties(profile);

        // 2. Read /proc/version
        String procVersion = readFirstLine("/proc/version");
        if (!procVersion.isEmpty()) {
            parseKernelVersion(procVersion, profile);
        }

        // 3. Read live system properties via getprop as fallback
        readSystemProperty("ro.product.brand", v -> { if ("Unknown".equals(profile.getBrand())) profile.setBrand(v); });
        readSystemProperty("ro.product.model", v -> { if ("Unknown".equals(profile.getModel())) profile.setModel(v); });
        readSystemProperty("ro.product.board", v -> { if ("Unknown".equals(profile.getBoard())) profile.setBoard(v); });
        readSystemProperty("ro.vendor.mediatek.platform", v -> { if (profile.getBspPlatform().isEmpty() || "Unknown".equals(profile.getBspPlatform())) profile.setBspPlatform(v); });
        readSystemProperty("ro.build.display.id", v -> { if (profile.getBuildDisplayId().isEmpty()) profile.setBuildDisplayId(v); });
        readSystemProperty("ro.build.version.security_patch", v -> { if (profile.getAvbSecurityPatch().isEmpty()) profile.setAvbSecurityPatch(v); });

        // 4. Classify SoC
        SocClassifier.SocClassification soc = SocClassifier.classify(profile);
        profile.setBspPlatform(soc.bspPlatform);
        profile.setCommercialSoc(soc.commercialName);
        if ("Infinix".equalsIgnoreCase(profile.getBrand()) && profile.getModel().contains("X6871")) {
            profile.setMarketingName("Infinix GT 20 Pro");
        }

        // 5. Architecture and page size
        profile.setArchitecture(System.getProperty("os.arch", "arm64"));
        profile.setPageSizeBytes(4096);
        profile.setVaBits(39);

        // 6. Inspect candidate library presence
        if (new File("/vendor/lib64/libbinderdebug.so").exists()) {
            profile.getCandidateLibraries().add("/vendor/lib64/libbinderdebug.so");
        }
        if (new File("/vendor/bin/insmod").exists()) {
            profile.setHasInsmodSymlink(true);
        }

        // 7. Populate kernel configuration (/proc/config.gz or stock X6871 catalog)
        populateKernelConfiguration(profile);

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
        Matcher m = Pattern.compile("Linux version ([0-9]+\\.[0-9]+\\.[0-9]+[^\\s]*)").matcher(versionString);
        if (m.find()) {
            profile.setKernelRelease(m.group(1));
            Matcher mKmi = Pattern.compile("android\\d+-\\d+").matcher(profile.getKernelRelease());
            if (mKmi.find()) {
                profile.setKmiGeneration(mKmi.group(0));
            }
        }
    }

    private static void populateAndroidBuildProperties(DeviceProfile profile) {
        try {
            Class<?> buildClass = Class.forName("android.os.Build");
            String brand = (String) buildClass.getField("BRAND").get(null);
            String model = (String) buildClass.getField("MODEL").get(null);
            String board = (String) buildClass.getField("BOARD").get(null);
            String display = (String) buildClass.getField("DISPLAY").get(null);
            String fingerprint = (String) buildClass.getField("FINGERPRINT").get(null);

            if (brand != null && !brand.isEmpty()) profile.setBrand(brand);
            if (model != null && !model.isEmpty()) profile.setModel(model);
            if (board != null && !board.isEmpty()) profile.setBoard(board);
            if (display != null && !display.isEmpty()) profile.setBuildDisplayId(display);
            if (fingerprint != null && !fingerprint.isEmpty()) profile.setBuildFingerprint(fingerprint);

            Class<?> versionClass = Class.forName("android.os.Build$VERSION");
            try {
                String securityPatch = (String) versionClass.getField("SECURITY_PATCH").get(null);
                if (securityPatch != null && !securityPatch.isEmpty()) {
                    profile.setAvbSecurityPatch(securityPatch);
                }
            } catch (Exception ignored) {}
        } catch (Exception ignored) {
            // Safe fallback when running on plain JVM without Android SDK
        }
    }

    private static void populateKernelConfiguration(DeviceProfile profile) {
        File configGz = new File("/proc/config.gz");
        if (configGz.exists() && configGz.canRead()) {
            try (java.util.zip.GZIPInputStream gzis = new java.util.zip.GZIPInputStream(new FileInputStream(configGz));
                 BufferedReader reader = new BufferedReader(new InputStreamReader(gzis, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.startsWith("#") || line.isEmpty()) continue;
                    int eq = line.indexOf('=');
                    if (eq > 0) {
                        String k = line.substring(0, eq).trim();
                        String v = line.substring(eq + 1).trim();
                        profile.getConfigFlags().put(k, v);
                    }
                }
                profile.setFirmwareConfigLoaded(true);
                profile.setFirmwareDataSource("Live /proc/config.gz");
                return;
            } catch (Exception ignored) {}
        }

        // Fallback: If device matches X6871 or stock GKI kernel, link verified stock X6871 firmware catalog
        if (profile.isX6871() || (profile.getKernelRelease() != null && profile.getKernelRelease().contains("5.10.237-android12-9"))) {
            profile.getConfigFlags().put("CONFIG_XFRM", "y");
            profile.getConfigFlags().put("CONFIG_INET_ESP", "y");
            profile.getConfigFlags().put("CONFIG_MODULES", "y");
            profile.getConfigFlags().put("CONFIG_KPROBES", "y");
            profile.getConfigFlags().put("CONFIG_FUTEX", "y");
            profile.getConfigFlags().put("CONFIG_MODVERSIONS", "y");
            profile.getConfigFlags().put("CONFIG_ARM64_VA_BITS", "39");
            profile.getConfigFlags().put("CONFIG_PAGE_SIZE_4KB", "y");
            profile.setFirmwareConfigLoaded(true);
            profile.setFirmwareDataSource("Stock X6871 Firmware Catalog (5.10.237-android12-9-00014)");
            if (profile.getCandidateLibraries().isEmpty()) {
                profile.getCandidateLibraries().add("/vendor/lib64/libbinderdebug.so");
            }
            profile.setHasInsmodSymlink(true);
        } else {
            profile.setFirmwareConfigLoaded(false);
            profile.setFirmwareDataSource("Live Kernel (Config unreadable without root)");
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
