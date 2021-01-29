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

import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.smarthome.config.core.ConfigurableService;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.http.HttpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main OSGi service and HTTP servlet for WebApp integration.
 *
 * @author Pasquale Pizzuti - Initial contribution
 */
@Component(immediate = true, service = HttpServlet.class, configurationPid = "org.openhab.webapp", property = {
        Constants.SERVICE_PID + "=org.openhab.webapp",
        ConfigurableService.SERVICE_PROPERTY_DESCRIPTION_URI + "=io:webapp",
        ConfigurableService.SERVICE_PROPERTY_CATEGORY + "=io",
        ConfigurableService.SERVICE_PROPERTY_LABEL + "=WebApp Integration" })
public class WebAppServlet extends HttpServlet {

    private static final long serialVersionUID = -1966364789075448441L;

    private static final String PATH = "/webapp";

    private final Logger logger = LoggerFactory.getLogger(WebAppServlet.class);

    private WebAppConfigWatchService configWatchService;

    private HttpService httpService;

    /**
     * Default constructor.
     */
    public WebAppServlet() {
    }

    /**
     * OSGi activation callback.
     *
     * @param config Service config.
     */
    @Activate
    protected void activate(Map<String, Object> config) {
        try {
            Dictionary<String, String> servletParams = new Hashtable<>();
            httpService.registerServlet(PATH, this, servletParams, httpService.createDefaultHttpContext());
            logger.info("Started WebApp integration service at " + PATH);
        } catch (Exception e) {
            logger.error("Could not start WebApp integration service: {}", e.getMessage(), e);
        }
    }

    /**
     * OSGi deactivation callback.
     */
    @Deactivate
    protected void deactivate() {
        try {
            httpService.unregister(PATH);
        } catch (IllegalArgumentException ignored) {
        }
        logger.info("WebApp integration service stopped");
    }

    @Reference
    protected void setConfigWatchService(WebAppConfigWatchService configWatchService) {
        this.configWatchService = configWatchService;
    }

    protected void unsetConfigWatchService(WebAppConfigWatchService configWatchService) {
        this.configWatchService = null;
    }

    @Reference
    protected void setHttpService(HttpService httpService) {
        this.httpService = httpService;
    }

    protected void unsetHttpService(HttpService httpService) {
        this.httpService = null;
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        configWatchService.service(req, resp);
    }
}
