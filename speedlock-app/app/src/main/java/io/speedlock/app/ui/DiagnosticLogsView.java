package io.speedlock.app.ui;

import io.speedlock.app.diagnostic.DiagnosticLogger;
import java.util.List;

/**
 * Renders the Structured Diagnostic Logs screen with interactive filtering,
 * keyword searching, and export verification confirmation.
 */
public class DiagnosticLogsView {

    public static String render(DiagnosticLogger logger) {
        return renderFiltered(logger, null, null);
    }

    /**
     * Renders filtered and searched diagnostic logs.
     *
     * @param logger the diagnostic logger
     * @param filterLevel specific log level to include, or null for ALL
     * @param searchQuery case-insensitive text search query, or null/empty for all
     * @return formatted log view string
     */
    public static String renderFiltered(DiagnosticLogger logger, DiagnosticLogger.Level filterLevel, String searchQuery) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== STRUCTURED DIAGNOSTIC LOGS ===\n");
        sb.append("Filter Level: ").append(filterLevel != null ? filterLevel.name() : "ALL").append("\n");
        sb.append("Search Query: ").append(searchQuery != null && !searchQuery.isEmpty() ? "\"" + searchQuery + "\"" : "None").append("\n\n");

        List<DiagnosticLogger.LogEntry> entries = logger.getEntries();
        int matched = 0;
        String queryLower = (searchQuery != null) ? searchQuery.toLowerCase() : null;

        for (DiagnosticLogger.LogEntry entry : entries) {
            if (filterLevel != null && entry.level != filterLevel) {
                continue;
            }
            if (queryLower != null && !entry.message.toLowerCase().contains(queryLower) && !entry.tag.toLowerCase().contains(queryLower)) {
                continue;
            }
            sb.append(entry.toString()).append("\n");
            matched++;
        }

        if (matched == 0) {
            sb.append("  [No matching log entries found for current criteria]\n");
        }

        sb.append("\nTotal matching entries: ").append(matched).append(" / ").append(entries.size()).append("\n");
        return sb.toString();
    }

    /**
     * Formats an export confirmation dialog / banner upon saving logs to storage.
     */
    public static String formatExportConfirmation(String exportPath, int entryCount, String sha256) {
        StringBuilder sb = new StringBuilder();
        sb.append("✓ DIAGNOSTIC LOG EXPORT CONFIRMED\n");
        sb.append("--------------------------------------------------\n");
        sb.append("Destination: ").append(exportPath).append("\n");
        sb.append("Records:     ").append(entryCount).append(" entries exported\n");
        sb.append("Integrity:   SHA-256: ").append(sha256).append("\n");
        sb.append("Timestamp:   ").append(new java.util.Date()).append("\n");
        return sb.toString();
    }
}
