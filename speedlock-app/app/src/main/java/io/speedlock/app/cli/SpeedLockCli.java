package io.speedlock.app.cli;

import io.speedlock.app.backend.BackendRegistry;
import io.speedlock.app.detector.DeviceDetector;
import io.speedlock.app.detector.SocClassifier;
import io.speedlock.app.diagnostic.CompatibilityEngine;
import io.speedlock.app.diagnostic.ReportExporter;
import io.speedlock.app.model.BackendCapability;
import io.speedlock.app.model.DeviceProfile;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Command-line entrypoint for Speed Lock diagnostic and compatibility validation.
 * Enables standalone verification and report generation in developer environments.
 */
public class SpeedLockCli {

    public static void main(String[] args) {
        System.out.println("======================================================================");
        System.out.println(" Speed Lock — Unified Root Management & Diagnostic Framework");
        System.out.println("======================================================================");

        String firmwareDir = null;
        String jsonOut = null;
        String mdOut = null;

        for (int i = 0; i < args.length; i++) {
            if ("--firmware".equals(args[i]) && i + 1 < args.length) {
                firmwareDir = args[++i];
            } else if ("--export-json".equals(args[i]) && i + 1 < args.length) {
                jsonOut = args[++i];
            } else if ("--export-md".equals(args[i]) && i + 1 < args.length) {
                mdOut = args[++i];
            }
        }

        DeviceProfile profile;
        if (firmwareDir != null && new File(firmwareDir).isDirectory()) {
            System.out.println("[+] Inspecting firmware directory: " + firmwareDir);
            profile = inspectFirmwareDirectory(new File(firmwareDir));
        } else {
            System.out.println("[*] Detecting live host / Android system properties...");
            profile = DeviceDetector.detectLiveDevice();
        }

        BackendRegistry registry = new BackendRegistry();
        CompatibilityEngine engine = new CompatibilityEngine(registry);
        CompatibilityEngine.CompatibilitySummary summary = engine.evaluate(profile);

        // Display results to stdout
        SocClassifier.SocClassification soc = SocClassifier.classify(profile);
        System.out.println("\n[DEVICE IDENTIFICATION]");
        System.out.println("  Brand:            " + profile.getBrand());
        System.out.println("  Model:            " + profile.getModel());
        System.out.println("  Marketing Name:   " + profile.getMarketingName());
        System.out.println("  SoC (Commercial): " + soc.commercialName);
        System.out.println("  SoC (BSP Fam.):   " + soc.bspPlatform);
        System.out.println("  Kernel Release:   " + profile.getKernelRelease());
        System.out.println("  Security Patch:   " + profile.getAvbSecurityPatch());

        System.out.println("\n[BACKEND CAPABILITY EVALUATION]");
        for (BackendCapability cap : summary.backendCapabilities.values()) {
            System.out.printf("  %-32s [%s] -> %s (Actionable: %s)%n",
                cap.getDisplayName(), cap.getCve(), cap.getState().name(), cap.isActionable());
        }

        if (!summary.criticalBlockers.isEmpty()) {
            System.out.println("\n[CRITICAL BLOCKERS DETECTED]");
            for (String blocker : summary.criticalBlockers) {
                System.out.println("  ! " + blocker);
            }
        }

        // Export reports if requested
        if (jsonOut != null) {
            String json = ReportExporter.exportToJson(summary);
            writeToFile(new File(jsonOut), json);
            System.out.println("\n[+] Exported JSON report: " + jsonOut);
        }
        if (mdOut != null) {
            String md = ReportExporter.exportToMarkdown(summary);
            writeToFile(new File(mdOut), md);
            System.out.println("[+] Exported Markdown report: " + mdOut);
        }

        System.out.println("======================================================================");
        System.out.println(" Execution completed successfully.");
    }

    public static DeviceProfile inspectFirmwareDirectory(File firmwareDir) {
        Map<String, String> props = new HashMap<>();

        // 1. Read vendor_ramdisk_build.prop
        File vendorProp = new File(firmwareDir, "kernel-metadata/vendor_ramdisk_build.prop");
        if (vendorProp.exists()) {
            props.putAll(parsePropFile(vendorProp));
        }

        // 2. Read boot_ramdisk_build.prop
        File bootProp = new File(firmwareDir, "kernel-metadata/boot_ramdisk_build.prop");
        if (bootProp.exists()) {
            props.putAll(parsePropFile(bootProp));
        }

        String release = "";
        String vermagic = "";

        // 3. Inspect kernel image
        File kernelImg = new File(firmwareDir, "kernel-metadata/Image-x6871-5.10.237");
        if (kernelImg.exists()) {
            try (FileInputStream fis = new FileInputStream(kernelImg)) {
                byte[] data = fis.readAllBytes();
                String raw = new String(data, StandardCharsets.ISO_8859_1);
                Matcher mRel = Pattern.compile("Linux version ([0-9]+\\.[0-9]+\\.[0-9]+-android[0-9]+-[^\\s]+)").matcher(raw);
                if (mRel.find()) {
                    release = mRel.group(1);
                }
                Matcher mVm = Pattern.compile("(5\\.10\\.[0-9]+-android[0-9]+-[0-9]+-[^\\x00]+modversions aarch64)").matcher(raw);
                if (mVm.find()) {
                    vermagic = mVm.group(1);
                }
            } catch (Exception ignored) {}
        }

        DeviceProfile profile = DeviceDetector.fromProperties(props, release, vermagic);

        // 4. Inspect kernel config
        File cfgFile = new File(firmwareDir, "kernel-metadata/extracted_config.txt");
        if (cfgFile.exists()) {
            parseConfig(cfgFile, profile);
        }

        // 5. Inspect candidate library
        File vendorMap = new File(firmwareDir, "stock-builds/vendor.map");
        if (vendorMap.exists()) {
            String vMap = readFile(vendorMap);
            if (vMap.contains("/vendor/lib64/libbinderdebug.so")) {
                profile.getCandidateLibraries().add("/vendor/lib64/libbinderdebug.so");
            }
        }

        // 6. Inspect insmod symlink
        File installedVendor = new File(firmwareDir, "kernel-metadata/installed-files-vendor.txt");
        if (installedVendor.exists()) {
            String inv = readFile(installedVendor);
            if (inv.contains("/vendor/bin/insmod")) {
                profile.setHasInsmodSymlink(true);
            }
        }

        // 7. Inspect scatter file
        File scatter = new File(firmwareDir, "partition-metadata/MT6895_Android_scatter.xml");
        if (scatter.exists()) {
            String sct = readFile(scatter);
            profile.setHasSuperPartition(sct.contains("super"));
        }

        return profile;
    }

    private static Map<String, String> parsePropFile(File f) {
        Map<String, String> map = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                int idx = line.indexOf('=');
                if (idx != -1) {
                    map.put(line.substring(0, idx).trim(), line.substring(idx + 1).trim());
                }
            }
        } catch (Exception ignored) {}
        return map;
    }

    private static void parseConfig(File f, DeviceProfile profile) {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("# CONFIG_") && line.contains("is not set")) {
                    String[] parts = line.split("\\s+");
                    if (parts.length >= 2) {
                        profile.getConfigFlags().put(parts[1], "is not set");
                    }
                } else if (line.startsWith("CONFIG_") && line.contains("=")) {
                    int idx = line.indexOf('=');
                    profile.getConfigFlags().put(line.substring(0, idx).trim(), line.substring(idx + 1).trim());
                }
            }
        } catch (Exception ignored) {}
    }

    private static String readFile(File f) {
        try (FileInputStream fis = new FileInputStream(f)) {
            return new String(fis.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception ignored) {
            return "";
        }
    }

    private static void writeToFile(File f, String content) {
        try (FileOutputStream fos = new FileOutputStream(f)) {
            fos.write(content.getBytes(StandardCharsets.UTF_8));
        } catch (Exception ignored) {}
    }
}
