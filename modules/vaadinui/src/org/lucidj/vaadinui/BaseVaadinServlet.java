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

package org.lucidj.vaadinui;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;

import com.vaadin.server.GlobalResourceHandler;
import com.vaadin.server.VaadinSession;
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

import java.lang.reflect.Field;
import java.util.Dictionary;
import java.util.Hashtable;

@Component
@Instantiate
@WebServlet(asyncSupported = true)
@VaadinServletConfiguration (ui = UI.class, productionMode = false, heartbeatInterval = 180)
public class BaseVaadinServlet extends VaadinServlet
{
    private final transient static Logger log = LoggerFactory.getLogger (BaseVaadinServlet.class);

    private UIProvider empty_provider = new EmptyUIProvider ();

    @Requires (optional = true)
    private UIProvider provider;

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

    // http://stackoverflow.com/questions/12485351/java-reflection-field-value-in-extends-class
    private Field findUnderlying(Class<?> clazz, String fieldName)
    {
        Class<?> current = clazz;
        do
        {
            try
            {
                return (current.getDeclaredField(fieldName));
            }
            catch(Exception ignore) {};
        }
        while((current = current.getSuperclass()) != null);

        return (null);
    }

    private boolean set_field (Object target, String field_name, Object field_value)
    {
        try
        {
            Field field = findUnderlying(target.getClass(), field_name);

            if (field != null)
            {
                field.setAccessible(true);
                field.set(target, field_value);
            }
            return (true);
        }
        catch (Exception e)
        {
            log.info("set_field: Exception {}", e);
        };
        return (false);
    }

    @Override
    protected VaadinServletService createServletService (DeploymentConfiguration deploymentConfiguration)
        throws ServiceException
    {
        VaadinServletService servletService = super.createServletService (deploymentConfiguration);
        log.info ("servletService = {}", servletService);

        servletService.addSessionInitListener (new SessionInitListener()
        {
            @Override
            public void sessionInit(SessionInitEvent sessionInitEvent) throws ServiceException
            {
                VaadinSession session = sessionInitEvent.getSession ();

                session.addUIProvider (provider != null? provider: empty_provider);

                // Create our own GlobalRequestHandler, extended to handle OSGi issues
                GlobalResourceHandler globalResourceHandler = new GlobalResourceHandlerEx ();

                // We need this ugly hack to be able to override the default global resource handler,
                // so we can put an osgi-aware in place.
                // TODO: FIND OUT WHY GlobalResourceHandler IS SET OUTSIDE VaadinService.createRequestHandlers()
                if (!set_field (session, "globalResourceHandler", globalResourceHandler))
                {
                    log.error ("Error setting resource handler; you may find problems retrieving class resources.");
                }

                // Step needed as VaadinSession.getGlobalResourceHandler() shows
                session.addRequestHandler (globalResourceHandler);
            }
        });

        return (servletService);
    }
}

// EOF
