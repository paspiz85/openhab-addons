/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.io.webapp.internal;

import static java.nio.file.StandardWatchEventKinds.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.util.stream.Collectors;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.config.core.ConfigConstants;
import org.eclipse.smarthome.core.service.AbstractWatchService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link WebAppConfigWatchService} provides a config for webapp
 *
 * @author Pasquale Pizzuti - Initial contribution
 */
@Component(service = WebAppConfigWatchService.class)
@NonNullByDefault
public class WebAppConfigWatchService extends AbstractWatchService {
    private static final String CONFIG_PATH = ConfigConstants.getConfigFolder() + File.separator + "misc";
    private static final String CONFIG_FILE = "webapp.js";

    private final Logger logger = LoggerFactory.getLogger(WebAppConfigWatchService.class);
    private String script = "";

    @Activate
    public WebAppConfigWatchService() {
        super(CONFIG_PATH);
        processWatchEvent(null, null, Paths.get(CONFIG_PATH, CONFIG_FILE));
    }

    @Override
    protected boolean watchSubDirectories() {
        return false;
    }

    @Override
    protected Kind<?>[] getWatchEventKinds(@Nullable Path directory) {
        return new Kind<?>[] { ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY };
    }

    @Override
    protected void processWatchEvent(@Nullable WatchEvent<?> event, @Nullable Kind<?> kind, @Nullable Path path) {
        if (path != null && path.endsWith(CONFIG_FILE)) {
            script = "";
            try {
                script = Files.lines(path).collect(Collectors.joining());
                logger.debug("Updated config: {}", script);
            } catch (IOException e) {
                logger.warn("Cannot read config file, webapp won't be processed: {}", e.getMessage());
            }
        }
    }

    public void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            ScriptEngineManager engineManager = new ScriptEngineManager();
            ScriptEngine engine = engineManager.getEngineByExtension("js");
            Bindings bindings = engine.createBindings();
            bindings.put("logger", logger);
            bindings.put("request", req);
            bindings.put("response", resp);
            engine.eval(script, bindings);
        } catch (Exception e) {
            logger.error("Cannot process request: {}", e.getMessage());
        }
    }
}
