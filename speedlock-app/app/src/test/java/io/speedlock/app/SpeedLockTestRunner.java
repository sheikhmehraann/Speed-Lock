package io.speedlock.app;

import io.speedlock.app.backend.*;
import io.speedlock.app.detector.*;
import io.speedlock.app.diagnostic.*;
import io.speedlock.app.model.*;

import java.io.File;
import java.lang.reflect.Method;
import java.util.*;

/**
 * Standalone test runner with zero external library dependencies.
 * Discovers and executes all test classes, measuring execution time and recording pass/fail assertions.
 */
public class SpeedLockTestRunner {

    public static class AssertionFailure extends RuntimeException {
        public AssertionFailure(String message) {
            super(message);
        }
    }

    public static void assertTrue(boolean condition, String message) {
        if (!condition) throw new AssertionFailure("Assertion failed: " + message);
    }

    public static void assertFalse(boolean condition, String message) {
        if (condition) throw new AssertionFailure("Assertion failed (expected false): " + message);
    }

    public static void assertEquals(Object expected, Object actual, String message) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionFailure(String.format("Expected [%s] but was [%s]: %s", expected, actual, message));
        }
    }

    public static void assertNotNull(Object obj, String message) {
        if (obj == null) throw new AssertionFailure("Object was null: " + message);
    }

    public static void main(String[] args) {
        System.out.println("======================================================================");
        System.out.println(" Speed Lock Android App Java Testbench Runner");
        System.out.println("======================================================================");

        List<Class<?>> testClasses = Arrays.asList(
            DeviceDetectorTest.class,
            SocClassifierTest.class,
            BackendRegistryTest.class,
            DFRootBackendTest.class,
            GhostLockBackendTest.class,
            CompatibilityEngineTest.class,
            ReportExporterTest.class,
            FirmwareInspectionIntegrationTest.class,
            OperationalRootIntegrityTest.class,
            LayoutInflationSafetyTest.class,
            DiagnosticLogsViewTest.class
        );

        int totalTests = 0;
        int passedTests = 0;
        int failedTests = 0;
        long startTime = System.currentTimeMillis();

        for (Class<?> clazz : testClasses) {
            System.out.println("Running suite: " + clazz.getSimpleName());
            try {
                Object instance = clazz.getDeclaredConstructor().newInstance();
                for (Method m : clazz.getDeclaredMethods()) {
                    if (m.getName().startsWith("test")) {
                        totalTests++;
                        try {
                            m.invoke(instance);
                            System.out.printf("  ✓ %-45s ... OK%n", m.getName());
                            passedTests++;
                        } catch (Exception e) {
                            Throwable cause = e.getCause() != null ? e.getCause() : e;
                            System.out.printf("  ✗ %-45s ... FAILED: %s%n", m.getName(), cause.getMessage());
                            failedTests++;
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Could not instantiate " + clazz.getName() + ": " + e.getMessage());
            }
        }

        long elapsed = System.currentTimeMillis() - startTime;
        System.out.println("======================================================================");
        System.out.printf("Tests run: %d, Passed: %d, Failed: %d (in %.3f s)%n",
            totalTests, passedTests, failedTests, elapsed / 1000.0);
        System.out.println("======================================================================");

        if (failedTests > 0) {
            System.out.println("TESTS FAILED.");
            System.exit(1);
        } else {
            System.out.println("ALL TESTS PASSED SUCCESSFULLY.");
            System.exit(0);
        }
    }
}
