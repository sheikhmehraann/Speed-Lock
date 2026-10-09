package io.speedlock.app;

import io.speedlock.app.backend.BackendRegistry;
import io.speedlock.app.diagnostic.CompatibilityEngine;
import io.speedlock.app.diagnostic.DiagnosticLogger;

/**
 * Android Application singleton maintaining the global lifecycle instances
 * of BackendRegistry, CompatibilityEngine, and DiagnosticLogger.
 */
public class SpeedLockApp {
    private static SpeedLockApp sInstance;
    private final BackendRegistry backendRegistry;
    private final CompatibilityEngine compatibilityEngine;
    private final DiagnosticLogger logger;

    public SpeedLockApp() {
        sInstance = this;
        this.logger = new DiagnosticLogger();
        this.backendRegistry = new BackendRegistry();
        this.compatibilityEngine = new CompatibilityEngine(backendRegistry);
        logger.info("SpeedLockApp", "Application initialized successfully.");
    }

    public static synchronized SpeedLockApp getInstance() {
        if (sInstance == null) {
            sInstance = new SpeedLockApp();
        }
        return sInstance;
    }

    public BackendRegistry getBackendRegistry() {
        return backendRegistry;
    }

    public CompatibilityEngine getCompatibilityEngine() {
        return compatibilityEngine;
    }

    public DiagnosticLogger getLogger() {
        return logger;
    }
}
