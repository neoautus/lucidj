/*
 * Copyright 2016 NEOautus Ltd. (http://neoautus.com)
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

package xyz.kuori.vaadin.kernel;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;

import com.vaadin.ui.UI;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Requires;
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
import java.util.HashMap;
import java.util.Hashtable;
import java.util.logging.Logger;

@Component
@Instantiate
@WebServlet(asyncSupported = true)
@VaadinServletConfiguration (ui = UI.class, productionMode = false, heartbeatInterval = 180)
public class BaseVaadinServlet extends VaadinServlet
{
    private static final Logger log = Logger.getLogger ("BaseVaadinServlet");

    private UIProvider empty_provider = new EmptyUIProvider ();

    @Requires (optional = true)
    private UIProvider provider;

    public BaseVaadinServlet (@Requires HttpService httpService)
        throws ServletException, NamespaceException
    {
        final HttpContext ctx = httpService.createDefaultHttpContext();

        Dictionary<String, String> init_params = new Hashtable<>();

        // Deals with the annoying websockets timeouts and interface freezes
        // (java.net.SocketTimeoutException: Timeout on Read)
        init_params.put("org.atmosphere.websocket.maxIdleTime", "86400000"); // 24h

        httpService.registerServlet ("/*", this, init_params, ctx);
    }

    @Override
    protected VaadinServletService createServletService (DeploymentConfiguration deploymentConfiguration)
        throws ServiceException
    {
        VaadinServletService servletService = super.createServletService (deploymentConfiguration);

        log.info ("createServletService");

        servletService.addSessionInitListener (new SessionInitListener()
        {
            @Override
            public void sessionInit(SessionInitEvent sessionInitEvent) throws ServiceException
            {
                if (provider == null)
                {
                    log.info ("empty_provider");
                    sessionInitEvent.getSession().addUIProvider (empty_provider);
                }
                else
                {
                    log.info (provider.toString());
                    sessionInitEvent.getSession().addUIProvider (provider);
                }
            }
        });

        return (servletService);
    }
}

// EOF
