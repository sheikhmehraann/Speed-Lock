package io.speedlock.app.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Encapsulates the detected hardware, operating system, and kernel profile
 * for the target device, including MediaTek BSP and commercial platform distinctions.
 */
public class DeviceProfile {
    private String brand = "Unknown";
    private String model = "Unknown";
    private String marketingName = "Unknown";
    private String bspPlatform = "Unknown";      // Internal BSP identifier, e.g., MT6895
    private String commercialSoc = "Unknown";     // Commercial marketing SKU, e.g., MT6896 / Dimensity 8200 Ultimate
    private String board = "Unknown";
    private String architecture = "arm64";
    private int pageSizeBytes = 4096;
    private int vaBits = 39;
    private String kernelRelease = "";
    private String vermagic = "";
    private String kmiGeneration = "";
    private String avbSecurityPatch = "";
    private String buildDisplayId = "";
    private String buildFingerprint = "";
    private boolean hasInsmodSymlink = false;
    private boolean hasSuperPartition = false;
    private final Map<String, String> configFlags = new HashMap<>();
    private final List<String> candidateLibraries = new ArrayList<>();
    private final Map<String, String> systemProperties = new HashMap<>();

    public DeviceProfile() {}

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public String getMarketingName() { return marketingName; }
    public void setMarketingName(String marketingName) { this.marketingName = marketingName; }

    public String getBspPlatform() { return bspPlatform; }
    public void setBspPlatform(String bspPlatform) { this.bspPlatform = bspPlatform; }

    public String getCommercialSoc() { return commercialSoc; }
    public void setCommercialSoc(String commercialSoc) { this.commercialSoc = commercialSoc; }

    public String getBoard() { return board; }
    public void setBoard(String board) { this.board = board; }

    public String getArchitecture() { return architecture; }
    public void setArchitecture(String architecture) { this.architecture = architecture; }

    public int getPageSizeBytes() { return pageSizeBytes; }
    public void setPageSizeBytes(int pageSizeBytes) { this.pageSizeBytes = pageSizeBytes; }

    public int getVaBits() { return vaBits; }
    public void setVaBits(int vaBits) { this.vaBits = vaBits; }

    public String getKernelRelease() { return kernelRelease; }
    public void setKernelRelease(String kernelRelease) { this.kernelRelease = kernelRelease; }

    public String getVermagic() { return vermagic; }
    public void setVermagic(String vermagic) { this.vermagic = vermagic; }

    public String getKmiGeneration() { return kmiGeneration; }
    public void setKmiGeneration(String kmiGeneration) { this.kmiGeneration = kmiGeneration; }

    public String getAvbSecurityPatch() { return avbSecurityPatch; }
    public void setAvbSecurityPatch(String avbSecurityPatch) { this.avbSecurityPatch = avbSecurityPatch; }

    public String getBuildDisplayId() { return buildDisplayId; }
    public void setBuildDisplayId(String buildDisplayId) { this.buildDisplayId = buildDisplayId; }

    public String getBuildFingerprint() { return buildFingerprint; }
    public void setBuildFingerprint(String buildFingerprint) { this.buildFingerprint = buildFingerprint; }

    public boolean hasInsmodSymlink() { return hasInsmodSymlink; }
    public void setHasInsmodSymlink(boolean hasInsmodSymlink) { this.hasInsmodSymlink = hasInsmodSymlink; }

    public boolean hasSuperPartition() { return hasSuperPartition; }
    public void setHasSuperPartition(boolean hasSuperPartition) { this.hasSuperPartition = hasSuperPartition; }

    public Map<String, String> getConfigFlags() { return configFlags; }
    public List<String> getCandidateLibraries() { return candidateLibraries; }
    public Map<String, String> getSystemProperties() { return systemProperties; }

    public boolean isGki510() {
        return kernelRelease.startsWith("5.10.") && kernelRelease.contains("android");
    }

    public boolean isX6871() {
        return "X6871".equalsIgnoreCase(model) || (model != null && model.contains("X6871"));
    }
}
