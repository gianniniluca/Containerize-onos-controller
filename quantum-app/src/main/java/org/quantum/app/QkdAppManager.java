/*
 * Copyright 2023-present Open Networking Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.quantum.app;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Skeletal ONOS application component.
 */
@Component(immediate = true, service = QkdAppManager.class)
public class QkdAppManager {

    private final Logger log = LoggerFactory.getLogger(getClass());

    //Used key is the SAE_ID
    private static final Map<String, QkdApp> qkdAppDatabase = new HashMap<>();

    @Activate
    protected void activate() {
        log.info("STARTED QkdLApp Manager appId");
    }

    @Deactivate
    protected void deactivate() {
        log.info("STOPPED QkdLApp Manager appId");
    }

    public void addQkdApp(String key, QkdApp value) {
        qkdAppDatabase.put(key, value);
    }

    public void removeQkdApp(String key) {
        qkdAppDatabase.remove(key);
    }

    public QkdApp getQkdApp(String key) {
        return qkdAppDatabase.get(key);
    }

    public Collection<QkdApp> getQkdApps() {
        return qkdAppDatabase.values();
    }

    public int getDatabaseSize() {
        return qkdAppDatabase.size();
    }
}
