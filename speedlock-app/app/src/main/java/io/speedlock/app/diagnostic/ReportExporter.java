package io.speedlock.app.diagnostic;

import io.speedlock.app.detector.SocClassifier;
import io.speedlock.app.model.BackendCapability;
import io.speedlock.app.model.DeviceProfile;
import java.util.Map;

/**
 * Serializes compatibility summaries and diagnostic logs into
 * structured JSON and formatted Markdown reports for archiving and analysis.
 */
public class ReportExporter {

    public static String exportToJson(CompatibilityEngine.CompatibilitySummary summary) {
        DeviceProfile p = summary.profile;
        SocClassifier.SocClassification soc = SocClassifier.classify(p);

        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"device_info\": {\n");
        sb.append("    \"brand\": \"").append(escapeJson(p.getBrand())).append("\",\n");
        sb.append("    \"model\": \"").append(escapeJson(p.getModel())).append("\",\n");
        sb.append("    \"marketing_name\": \"").append(escapeJson(p.getMarketingName())).append("\",\n");
        sb.append("    \"bsp_platform\": \"").append(escapeJson(soc.bspPlatform)).append("\",\n");
        sb.append("    \"commercial_soc\": \"").append(escapeJson(soc.commercialName)).append("\",\n");
        sb.append("    \"board\": \"").append(escapeJson(p.getBoard())).append("\",\n");
        sb.append("    \"display_id\": \"").append(escapeJson(p.getBuildDisplayId())).append("\"\n");
        sb.append("  },\n");

        sb.append("  \"kernel_info\": {\n");
        sb.append("    \"release\": \"").append(escapeJson(p.getKernelRelease())).append("\",\n");
        sb.append("    \"vermagic\": \"").append(escapeJson(p.getVermagic())).append("\",\n");
        sb.append("    \"kmi\": \"").append(escapeJson(p.getKmiGeneration())).append("\",\n");
        sb.append("    \"architecture\": \"").append(escapeJson(p.getArchitecture())).append("\",\n");
        sb.append("    \"page_size_bytes\": ").append(p.getPageSizeBytes()).append(",\n");
        sb.append("    \"va_bits\": ").append(p.getVaBits()).append("\n");
        sb.append("  },\n");

        sb.append("  \"security_profile\": {\n");
        sb.append("    \"security_patch\": \"").append(escapeJson(p.getAvbSecurityPatch())).append("\",\n");
        sb.append("    \"has_insmod_symlink\": ").append(p.hasInsmodSymlink()).append(",\n");
        sb.append("    \"has_super_partition\": ").append(p.hasSuperPartition()).append("\n");
        sb.append("  },\n");

        sb.append("  \"backends\": {\n");
        int count = 0;
        for (Map.Entry<String, BackendCapability> entry : summary.backendCapabilities.entrySet()) {
            count++;
            BackendCapability cap = entry.getValue();
            sb.append("    \"").append(entry.getKey()).append("\": {\n");
            sb.append("      \"name\": \"").append(escapeJson(cap.getDisplayName())).append("\",\n");
            sb.append("      \"cve\": \"").append(escapeJson(cap.getCve())).append("\",\n");
            sb.append("      \"state\": \"").append(cap.getState().name()).append("\",\n");
            sb.append("      \"prerequisites_met\": ").append(cap.isPrerequisitesMet()).append(",\n");
            sb.append("      \"is_actionable\": ").append(cap.isActionable()).append(",\n");
            sb.append("      \"reasons\": [");
            for (int i = 0; i < cap.getReasons().size(); i++) {
                sb.append("\"").append(escapeJson(cap.getReasons().get(i))).append("\"");
                if (i < cap.getReasons().size() - 1) sb.append(", ");
            }
            sb.append("],\n");
            sb.append("      \"critical_blockers\": [");
            for (int i = 0; i < cap.getCriticalBlockers().size(); i++) {
                sb.append("\"").append(escapeJson(cap.getCriticalBlockers().get(i))).append("\"");
                if (i < cap.getCriticalBlockers().size() - 1) sb.append(", ");
            }
            sb.append("]\n");
            sb.append("    }").append(count < summary.backendCapabilities.size() ? ",\n" : "\n");
        }
        sb.append("  },\n");

        sb.append("  \"operational_verdict\": {\n");
        sb.append("    \"has_actionable_backend\": ").append(summary.hasActionableBackend).append(",\n");
        sb.append("    \"overall_status\": \"").append(summary.hasActionableBackend ? "BUILD_VERIFIED" : "METADATA_COMPATIBLE").append("\"\n");
        sb.append("  }\n");
        sb.append("}\n");

        return sb.toString();
    }

    public static String exportToMarkdown(CompatibilityEngine.CompatibilitySummary summary) {
        DeviceProfile p = summary.profile;
        SocClassifier.SocClassification soc = SocClassifier.classify(p);

        StringBuilder sb = new StringBuilder();
        sb.append("# Speed Lock -- Device Diagnostic Report\n\n");
        sb.append("**Target Device:** ").append(p.getBrand()).append(" ").append(p.getModel());
        if (!p.getMarketingName().isEmpty()) {
            sb.append(" (").append(p.getMarketingName()).append(")");
        }
        sb.append("\n");
        sb.append("**Commercial SoC:** ").append(soc.commercialName).append("\n");
        sb.append("**BSP Platform Identifier:** ").append(soc.bspPlatform).append("\n");
        sb.append("**Kernel Release:** `").append(p.getKernelRelease()).append("`\n\n");

        sb.append("## 1. MediaTek Platform Analysis\n\n");
        sb.append(soc.technicalExplanation).append("\n\n");

        sb.append("## 2. Hardware & Kernel Characteristics\n\n");
        sb.append("| Characteristic | Detected Value |\n");
        sb.append("|---|---|\n");
        sb.append("| Architecture | `").append(p.getArchitecture()).append("` |\n");
        sb.append("| Page Size | `").append(p.getPageSizeBytes()).append(" bytes` |\n");
        sb.append("| Virtual Address Space | `").append(p.getVaBits()).append(" bits` |\n");
        sb.append("| Build Display ID | `").append(p.getBuildDisplayId()).append("` |\n");
        sb.append("| Security Patch Level | `").append(p.getAvbSecurityPatch()).append("` |\n");
        sb.append("| Insmod Symlink | `").append(p.hasInsmodSymlink()).append("` |\n\n");

        sb.append("## 3. Backend Capability Catalogue\n\n");
        sb.append("| Backend Implementation | CVE | Evaluated State | Actionable |\n");
        sb.append("|---|---|---|---|\n");
        for (BackendCapability cap : summary.backendCapabilities.values()) {
            sb.append("| **").append(cap.getDisplayName()).append("** | `")
              .append(cap.getCve()).append("` | `")
              .append(cap.getState().name()).append("` | `")
              .append(cap.isActionable()).append("` |\n");
        }
        sb.append("\n");

        sb.append("## 4. Critical Blockers & Non-Violated Principles\n\n");
        if (summary.criticalBlockers.isEmpty()) {
            sb.append("No critical blockers detected.\n\n");
        } else {
            for (String blocker : summary.criticalBlockers) {
                sb.append("- ").append(blocker).append("\n");
            }
            sb.append("\n");
        }

        sb.append("## 5. Epistemological Integrity Notice\n\n");
        sb.append("Metadata compatibility demonstrates that target preconditions (kernel configs, ");
        sb.append("memory geometry, and vendor paths) exist. In adherence to defensive principles, ");
        sb.append("live root support cannot and will not be claimed without physical device execution.\n");

        return sb.toString();
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
