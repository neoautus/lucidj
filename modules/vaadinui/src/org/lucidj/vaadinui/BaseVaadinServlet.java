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

import com.vaadin.server.ClientConnector;
import com.vaadin.server.UIClassSelectionEvent;
import com.vaadin.server.UICreateEvent;
import com.vaadin.server.VaadinSession;
import com.vaadin.ui.UI;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Context;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Requires;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.osgi.util.tracker.ServiceTracker;

import com.vaadin.annotations.VaadinServletConfiguration;
import com.vaadin.server.DeploymentConfiguration;
import com.vaadin.server.ServiceException;
import com.vaadin.server.SessionInitEvent;
import com.vaadin.server.SessionInitListener;
import com.vaadin.server.UIProvider;
import com.vaadin.server.VaadinServlet;
import com.vaadin.server.VaadinServletService;

import java.net.URL;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component (immediate = true, publicFactory = false)
@Instantiate
@WebServlet (asyncSupported = true)
@VaadinServletConfiguration (ui = UI.class, productionMode = false, heartbeatInterval = 180)
public class BaseVaadinServlet extends VaadinServlet
    implements SessionInitListener, ClientConnector.AttachListener, ClientConnector.DetachListener
{
    private final static Logger log = LoggerFactory.getLogger (BaseVaadinServlet.class);

    private DefaultUIProvider default_provider = new DefaultUIProvider ();
    private URLServiceTracker url_tracker;
    private Set<UI> attached_uis = new HashSet<> ();
    private ExecutorService background = Executors.newSingleThreadExecutor ();

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

        // Enable URL tracking
        url_tracker = new URLServiceTracker ();
        url_tracker.open ();
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

    @Override // ClientConnector.AttachListener
    public void attach (ClientConnector.AttachEvent attachEvent)
    {
        synchronized (attached_uis)
        {
            attached_uis.add (attachEvent.getConnector ().getUI ());
        }
    }

    @Override // ClientConnector.DetachListener
    public void detach (ClientConnector.DetachEvent detachEvent)
    {
        synchronized (attached_uis)
        {
            attached_uis.remove (detachEvent.getConnector ().getUI ());
        }
    }

    private void refresh_attached_uis ()
    {
        UI[] attached_ui_array;

        synchronized (attached_uis)
        {
            attached_ui_array = attached_uis.toArray (new UI [0]);
        }

        log.info ("Refreshing CSS on UIs: {} attached", attached_ui_array.length);

        for (UI ui: attached_ui_array)
        {
            try
            {
                log.info ("Refreshing CSS on: {}", ui);
                ui.getPage ().getJavaScript()
                    .execute("window.lucidj_vaadin_helper.reloadStyleSheet ('"
                             + VaadinMapper.VAADIN_DYNAMIC_STYLES_CSS
                             + "')");
            }
            catch (Throwable quirk)
            {
                log.error ("Quirk ocurred refreshing attached UI: {}", ui, quirk);
            }
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
            UI new_ui = new BaseVaadinUI (securityEngine, managedObjectFactory);
            new_ui.addAttachListener (BaseVaadinServlet.this);
            new_ui.addDetachListener (BaseVaadinServlet.this);
            return (serviceContext.wrapObject (UI.class, new_ui));
        }
    }

    class URLServiceTracker extends ServiceTracker<URL, URL>
    {
        public URLServiceTracker ()
        {
            super (bundleContext, URL.class, null);
        }

        @Override
        public URL addingService (ServiceReference<URL> serviceReference)
        {
            String extension = (String)serviceReference.getProperty ("extension");

            if (extension != null && extension.equals ("css"))
            {
                background.submit (new Runnable ()
                {
                    @Override
                    public void run ()
                    {
                        refresh_attached_uis ();
                    }
                });
                return (bundleContext.getService (serviceReference));
            }
            return (null);
        }
    }
}

// EOF
