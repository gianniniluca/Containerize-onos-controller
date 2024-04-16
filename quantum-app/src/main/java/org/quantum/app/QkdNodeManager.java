package org.quantum.app;

import org.onosproject.core.ApplicationId;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Component(immediate = true, service = QkdNodeManager.class)
public class QkdNodeManager {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private ApplicationId appId;
    private static final Map<String, QkdNode> qkdNodeDatabase = new HashMap<>();

    @Activate
    protected void activate() {
        log.info("STARTED QkdNode Manager appId");
    }

    @Deactivate
    protected void deactivate() {
        log.info("STOPPED QkdNode Manager appId");
    }

    public void addQkdNode(String key, QkdNode value) {
        qkdNodeDatabase.put(key, value);
    }

    public void removeQkdNode(String key) {
        qkdNodeDatabase.remove(key);
    }

    public QkdNode getQkdNode(String key) {
        return qkdNodeDatabase.get(key);
    }

    public Collection<QkdNode> getQkdNodes() {
        return qkdNodeDatabase.values();
    }

    public int getDatabaseSize() {
        return qkdNodeDatabase.size();
    }
}
