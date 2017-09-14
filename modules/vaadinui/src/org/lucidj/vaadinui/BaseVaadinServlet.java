/*
 * Copyright 2017 NEOautus Ltd. (http://neoautus.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.lucidj.vaadinui;

import org.lucidj.api.ManagedObjectFactory;
import org.lucidj.api.SecurityEngine;
import org.lucidj.api.ServiceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;

import com.vaadin.server.UIClassSelectionEvent;
import com.vaadin.server.UICreateEvent;
import com.vaadin.server.VaadinSession;
import com.vaadin.ui.UI;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Context;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Requires;
import org.osgi.framework.BundleContext;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;

import com.vaadin.annotations.VaadinServletConfiguration;
import com.vaadin.server.DeploymentConfiguration;
import com.vaadin.server.ServiceException;
import com.vaadin.server.SessionInitEvent;
import com.vaadin.server.SessionInitListener;
import com.vaadin.server.UIProvider;
import com.vaadin.server.VaadinServlet;
import com.vaadin.server.VaadinServletService;

import java.util.Dictionary;
import java.util.Hashtable;

@Component (immediate = true, publicFactory = false)
@Instantiate
@WebServlet (asyncSupported = true)
@VaadinServletConfiguration (ui = UI.class, productionMode = false, heartbeatInterval = 180)
public class BaseVaadinServlet extends VaadinServlet implements SessionInitListener
{
    private final transient static Logger log = LoggerFactory.getLogger (BaseVaadinServlet.class);

    private DefaultUIProvider default_provider = new DefaultUIProvider ();

    @Context
    private BundleContext bundleContext;

    @Requires
    private ServiceContext serviceContext;

    @Requires
    private SecurityEngine securityEngine;

    @Requires
    private ManagedObjectFactory managedObjectFactory;

    public BaseVaadinServlet (@Requires HttpService httpService)
        throws ServletException, NamespaceException
    {
        final HttpContext ctx = httpService.createDefaultHttpContext ();

        Dictionary<String, String> init_params = new Hashtable<> ();

        // Deals with the annoying websockets timeouts and interface freezes
        // (java.net.SocketTimeoutException: Timeout on Read)
        init_params.put ("org.atmosphere.websocket.maxIdleTime", "86400000"); // 24h

        httpService.registerServlet ("/*", this, init_params, ctx);
    }

    @Override
    protected VaadinServletService createServletService (DeploymentConfiguration deploymentConfiguration)
        throws ServiceException
    {
        VaadinServletService servletService = super.createServletService (deploymentConfiguration);
        servletService.addSessionInitListener (this);
        return (servletService);
    }

    @Override
    public void sessionInit (SessionInitEvent sessionInitEvent) throws ServiceException
    {
        VaadinSession session = sessionInitEvent.getSession ();
        session.addUIProvider (default_provider);

        // Create our own GlobalRequestHandler, extended to handle OSGi issues
        if (!new GlobalResourceHandlerEx ().hook (session))
        {
            log.error ("Error setting OSGi resource handler; you may find problems retrieving class resources.");
        }
    }

    public class DefaultUIProvider extends UIProvider
    {
        @Override
        public Class<? extends UI> getUIClass (UIClassSelectionEvent uiClassSelectionEvent)
        {
            return (BaseVaadinUI.class);
        }

        @Override
        public UI createInstance (UICreateEvent event)
        {
            return (serviceContext.wrapObject (UI.class, new BaseVaadinUI (securityEngine, managedObjectFactory)));
        }
    }
}

// EOF
