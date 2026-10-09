package io.speedlock.app;

import io.speedlock.app.ui.SettingsView;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Regression test suite verifying Android layout inflation safety,
 * resource presence, and Material Design UI constraints to prevent startup crashes.
 */
public class LayoutInflationSafetyTest {

    private File findFile(String relativePath) {
        File f1 = new File(relativePath);
        if (f1.exists()) return f1;
        File f2 = new File("speedlock-app/" + relativePath);
        if (f2.exists()) return f2;
        File f3 = new File("../" + relativePath);
        if (f3.exists()) return f3;
        File f4 = new File("../../" + relativePath);
        if (f4.exists()) return f4;
        throw new RuntimeException("Could not locate resource file: " + relativePath);
    }

    private Document parseXml(File file) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(file);
    }

    /**
     * Regression test for critical crash:
     * Material Components BottomNavigationView hardcodes MAXIMUM_ITEM_COUNT = 5.
     * Inflating a menu with > 5 items throws IllegalArgumentException during setContentView().
     */
    public void testBottomNavMenuMaxItemCount() throws Exception {
        File menuFile = findFile("app/src/main/res/menu/bottom_nav_menu.xml");
        Document doc = parseXml(menuFile);
        NodeList items = doc.getElementsByTagName("item");

        int itemCount = items.getLength();
        SpeedLockTestRunner.assertTrue(itemCount <= 5,
            "BottomNavigationView menu items count (" + itemCount + ") exceeds Material Components limit of 5. " +
            "This causes an immediate fatal IllegalArgumentException crash on Android startup!");
        SpeedLockTestRunner.assertEquals(5, itemCount,
            "Expected exactly 5 bottom navigation items (Home, Backends, Device, Audit, Logs).");
    }

    /**
     * Verifies that all 40+ views bound by MainActivity.java exist in activity_main.xml.
     */
    public void testAllBoundViewIdsExistInLayout() throws Exception {
        File layoutFile = findFile("app/src/main/res/layout/activity_main.xml");
        Document doc = parseXml(layoutFile);

        Set<String> layoutIds = new HashSet<>();
        collectIds(doc.getDocumentElement(), layoutIds);

        List<String> requiredIds = Arrays.asList(
            "toolbar", "btn_action_refresh", "btn_action_settings",
            "bottom_navigation", "screens_container",
            "screen_home", "screen_backends", "screen_device", "screen_audit", "screen_logs", "screen_settings",
            "tv_home_status_badge", "tv_home_device_title", "tv_home_kernel_title", "tv_home_root_state",
            "tv_home_soc_name", "tv_home_bsp_platform", "tv_home_arch_info", "tv_home_data_source",
            "tv_home_dfroot_summary", "tv_home_ghostlock_summary",
            "btn_home_run_audit", "btn_home_goto_backends", "btn_home_export_report", "btn_home_goto_settings",
            "btn_backend_audit_dfroot", "btn_backend_audit_ghostlock",
            "tv_device_model_val", "tv_device_brand_val", "tv_device_board_val", "tv_device_platform_val",
            "tv_device_kernel_val", "tv_device_security_val", "tv_device_datasource_val",
            "et_log_search", "tv_log_output",
            "btn_filter_all", "btn_filter_info", "btn_filter_warn", "btn_filter_error",
            "btn_logs_copy", "btn_logs_export_json",
            "rg_theme", "switch_reduced_motion", "switch_anonymize", "btn_settings_updates"
        );

        for (String id : requiredIds) {
            SpeedLockTestRunner.assertTrue(layoutIds.contains(id),
                "Missing required view ID in activity_main.xml: @" + id);
        }
    }

    /**
     * Verifies that all required drawables exist on disk.
     */
    public void testAllRequiredDrawablesExist() {
        List<String> drawables = Arrays.asList(
            "ic_nav_home.xml", "ic_nav_backends.xml", "ic_nav_device.xml",
            "ic_nav_audit.xml", "ic_nav_logs.xml", "ic_nav_settings.xml",
            "ic_refresh.xml", "bg_rounded_card.xml", "bg_chip_badge.xml",
            "bg_status_banner_success.xml", "bg_status_banner_warning.xml", "bg_status_banner_danger.xml"
        );

        for (String d : drawables) {
            File f = findFile("app/src/main/res/drawable/" + d);
            SpeedLockTestRunner.assertTrue(f.exists() && f.length() > 0,
                "Drawable resource missing or empty: " + d);
        }
    }

    /**
     * Verifies that SettingsState model operates cleanly with safe defaults.
     */
    public void testSettingsStateSafeDefaults() {
        SettingsView.SettingsState state = new SettingsView.SettingsState();
        SpeedLockTestRunner.assertNotNull(state, "SettingsState must not be null");
        SpeedLockTestRunner.assertEquals(SettingsView.ThemeMode.MICA_DARK, state.getThemeMode(), "Default theme must be MICA_DARK");
        SpeedLockTestRunner.assertFalse(state.isReducedMotion(), "Reduced motion default must be false");
        SpeedLockTestRunner.assertTrue(state.isStrictPrivacyMode(), "Strict privacy default must be true");
    }

    private void collectIds(org.w3c.dom.Node node, Set<String> ids) {
        if (node instanceof org.w3c.dom.Element) {
            org.w3c.dom.Element el = (org.w3c.dom.Element) node;
            String idAttr = el.getAttributeNS("http://schemas.android.com/apk/res/android", "id");
            if (idAttr != null && !idAttr.isEmpty()) {
                int slash = idAttr.indexOf('/');
                if (slash >= 0) {
                    ids.add(idAttr.substring(slash + 1));
                }
            }
        }
        NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            collectIds(children.item(i), ids);
        }
    }
}
