package org.thingsboard.server.transport.lwm2m.bootstrap.secure;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.core.util.Validate;
import org.eclipse.leshan.server.bootstrap.BootstrapConfig;
import org.eclipse.leshan.server.bootstrap.InMemoryBootstrapConfigStore;
import org.eclipse.leshan.server.bootstrap.InvalidConfigurationException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Slf4j
@Component("LwM2MInMemoryBootstrapConfigStore")
@ConditionalOnExpression("'${service.type:null}'=='tb-transport' || ('${service.type:null}'=='monolith' && '${transport.lwm2m.enabled}'=='true'&& '${transport.lwm2m.bootstrap.enable}'=='true')")
public class LwM2MInMemoryBootstrapConfigStore extends InMemoryBootstrapConfigStore {
    // default location for persistence
//    public static final String DEFAULT_FILE = "data/bootstrap.json";
    // lock for the two maps
    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    private final Lock readLock = readWriteLock.readLock();
    private final Lock writeLock = readWriteLock.writeLock();

    @Override
    public Map<String, BootstrapConfig> getAll() {
        readLock.lock();
        try {
            return super.getAll();
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public void add(String endpoint, BootstrapConfig config) throws InvalidConfigurationException {
        writeLock.lock();
        try {
            addToStore(endpoint, config);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public BootstrapConfig remove(String enpoint) {
        writeLock.lock();
        try {
            BootstrapConfig res = super.remove(enpoint);
            return res;
        } finally {
            writeLock.unlock();
        }
    }

    public void addToStore(String endpoint, BootstrapConfig config) throws InvalidConfigurationException {
        super.add(endpoint, config);
    }
}
