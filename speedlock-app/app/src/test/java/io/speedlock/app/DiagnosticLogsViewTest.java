package io.speedlock.app;

import io.speedlock.app.diagnostic.DiagnosticLogger;
import io.speedlock.app.ui.DiagnosticLogsView;

public class DiagnosticLogsViewTest {

    public void testRenderFilteredShowsAllWhenLevelDebug() {
        DiagnosticLogger logger = new DiagnosticLogger();
        logger.debug("TestTag", "Debug message");
        logger.info("TestTag", "Info message");
        logger.warn("TestTag", "Warn message");
        logger.error("TestTag", "Error message");

        String rendered = DiagnosticLogsView.renderFiltered(logger, DiagnosticLogger.Level.DEBUG, null);
        assert rendered.contains("Debug message") : "Should contain debug message";
        assert rendered.contains("Info message") : "Should contain info message";
        assert rendered.contains("Warn message") : "Should contain warn message";
        assert rendered.contains("Error message") : "Should contain error message";
        assert rendered.contains("Total matching entries: 4 / 4") : "All 4 entries should match";
    }

    public void testRenderFilteredInfoExcludesDebug() {
        DiagnosticLogger logger = new DiagnosticLogger();
        logger.debug("TestTag", "Debug message");
        logger.info("TestTag", "Info message");
        logger.warn("TestTag", "Warn message");
        logger.error("TestTag", "Error message");

        String rendered = DiagnosticLogsView.renderFiltered(logger, DiagnosticLogger.Level.INFO, null);
        assert !rendered.contains("Debug message") : "Should NOT contain debug message";
        assert rendered.contains("Info message") : "Should contain info message";
        assert rendered.contains("Warn message") : "Should contain warn message";
        assert rendered.contains("Error message") : "Should contain error message";
        assert rendered.contains("Total matching entries: 3 / 4") : "3 of 4 entries should match";
    }

    public void testRenderFilteredKeywordSearch() {
        DiagnosticLogger logger = new DiagnosticLogger();
        logger.info("Alpha", "Matching keyword inside");
        logger.info("Beta", "Different message completely");

        String rendered = DiagnosticLogsView.renderFiltered(logger, null, "keyword");
        assert rendered.contains("Matching keyword inside") : "Should match keyword";
        assert !rendered.contains("Different message completely") : "Should not match different message";
        assert rendered.contains("Total matching entries: 1 / 2") : "1 of 2 entries should match";
    }

    public void testFormatExportConfirmation() {
        String confirm = DiagnosticLogsView.formatExportConfirmation("/tmp/export.json", 10, "abcdef123456");
        assert confirm.contains("DIAGNOSTIC LOG EXPORT CONFIRMED") : "Header should exist";
        assert confirm.contains("/tmp/export.json") : "Path should exist";
        assert confirm.contains("10 entries exported") : "Count should exist";
        assert confirm.contains("abcdef123456") : "Hash should exist";
    }
}
