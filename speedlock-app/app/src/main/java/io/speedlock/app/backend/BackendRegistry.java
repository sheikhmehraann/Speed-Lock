package io.speedlock.app.backend;

import io.speedlock.app.model.BackendCapability;
import io.speedlock.app.model.DeviceProfile;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Central discovery and lookup registry for all available exploit backends.
 */
public class BackendRegistry {
    private final Map<String, IRootBackend> backends = new LinkedHashMap<>();

    public BackendRegistry() {
        registerDefaultBackends();
    }

    private void registerDefaultBackends() {
        register(new DFRootBackend());
        register(new GhostLockBackend());
        register(new DirtyInitBackend());
        register(new UniRootBackend());
    }

    public void register(IRootBackend backend) {
        backends.put(backend.getId().toLowerCase(), backend);
    }

    public IRootBackend getBackend(String id) {
        if (id == null) return null;
        return backends.get(id.toLowerCase());
    }

    public List<IRootBackend> getAllBackends() {
        return Collections.unmodifiableList(new ArrayList<>(backends.values()));
    }

    public List<IRootBackend> getByCve(String cve) {
        List<IRootBackend> results = new ArrayList<>();
        if (cve == null) return results;
        for (IRootBackend b : backends.values()) {
            if (b.getCve() != null && b.getCve().toLowerCase().contains(cve.toLowerCase())) {
                results.add(b);
            }
        }
        return results;
    }

    public Map<String, BackendCapability> evaluateAll(DeviceProfile profile) {
        Map<String, BackendCapability> results = new LinkedHashMap<>();
        for (IRootBackend b : backends.values()) {
            results.put(b.getId(), b.evaluateCompatibility(profile));
        }
        return results;
    }
}
