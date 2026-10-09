package io.speedlock.app.ui;

/**
 * Settings configuration and presentation for the Speed Lock application.
 * Manages Fluent/Material 3 theme preferences, motion reduction, and privacy modes.
 */
public class SettingsView {

    public enum ThemeMode {
        SYSTEM_DEFAULT("Follow System"),
        FLUENT_LIGHT("Fluent Light Acrylic"),
        MICA_DARK("Mica Dark (Default)");

        private final String displayName;

        ThemeMode(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public static class SettingsState {
        private ThemeMode themeMode = ThemeMode.MICA_DARK;
        private boolean dynamicColorEnabled = true;
        private boolean reducedMotion = false;
        private boolean strictPrivacyMode = true; // Zero telemetry, 100% offline

        public ThemeMode getThemeMode() {
            return themeMode;
        }

        public void setThemeMode(ThemeMode themeMode) {
            this.themeMode = themeMode;
        }

        public boolean isDynamicColorEnabled() {
            return dynamicColorEnabled;
        }

        public void setDynamicColorEnabled(boolean dynamicColorEnabled) {
            this.dynamicColorEnabled = dynamicColorEnabled;
        }

        public boolean isReducedMotion() {
            return reducedMotion;
        }

        public void setReducedMotion(boolean reducedMotion) {
            this.reducedMotion = reducedMotion;
        }

        public boolean isStrictPrivacyMode() {
            return strictPrivacyMode;
        }

        public void setStrictPrivacyMode(boolean strictPrivacyMode) {
            this.strictPrivacyMode = strictPrivacyMode;
        }
    }

    public static String render(SettingsState state) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== SPEED LOCK SETTINGS & PREFERENCES ===\n\n");
        sb.append("Visual Appearance:\n");
        sb.append("  • Theme Mode:          ").append(state.getThemeMode().getDisplayName()).append("\n");
        sb.append("  • Dynamic Color:       ").append(state.isDynamicColorEnabled() ? "Enabled (Material You)" : "Disabled").append("\n");
        sb.append("  • Reduced Motion:      ").append(state.isReducedMotion() ? "Enabled (Fluent static)" : "Disabled (Fluid animations)").append("\n");
        sb.append("\nSecurity & Privacy:\n");
        sb.append("  • Strict Privacy Mode: ").append(state.isStrictPrivacyMode() ? "Active (100% Local / Zero Telemetry)" : "Standard").append("\n");
        sb.append("  • Remote Logging:      Disabled (Hardcoded off)\n");
        sb.append("  • Firmware Cache:      Protected in app-private sandbox\n");
        return sb.toString();
    }
}
