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

package org.lucidj.navigatormanager;

import org.lucidj.api.core.ServiceContext;
import org.lucidj.api.core.ServiceObject;
import org.lucidj.api.vui.NavigatorManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.navigator.Navigator;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewProvider;
import com.vaadin.server.VaadinSession;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ProxyViewProvider implements ViewProvider, ServiceObject.Listener
{
    private final static Logger log = LoggerFactory.getLogger (ProxyViewProvider.class);

    private Map<String, ViewProvider> view_name_to_provider;
    private Map<String, View> view_name_to_view;

    private DefaultNavigatorManager navigatorManager;
    private Navigator navigator;

    @ServiceObject.Context
    private ServiceContext serviceContext;

    public ProxyViewProvider (DefaultNavigatorManager navigatorManager, Navigator navigator)
    {
        this.navigatorManager = navigatorManager;
        this.navigator = navigator;
        view_name_to_provider = new ConcurrentHashMap<> ();
        view_name_to_view = new ConcurrentHashMap<> ();
    }

    @Override // ViewProvider
    public String getViewName (String s)
    {
        ViewProvider view_provider = navigatorManager.findViewProvider (s);

        if (view_provider != null)
        {
            String view_name = view_provider.getViewName (s);

            if (view_name != null)
            {
                // We're actually looking for a provider which knowns the requested view
                view_name_to_provider.put (view_name, view_provider);
                return (view_name);
            }
        }
        return (null);
    }

    @Override // ViewProvider
    public View getView (String viewName)
    {
        View view = view_name_to_view.get (viewName);

        // Do we have a cached view?
        if (view == null)
        {
            ViewProvider provider = view_name_to_provider.get (viewName);

            if (provider == null)
            {
                log.error ("Provider not found for view: {}", viewName);
                return (null);
            }
            view = provider.getView (viewName);
            view_name_to_view.put (viewName, view);
        }
        log.info ("ProxyViewProvider.getView (viewName={}) => {}", viewName, view);
        return (view);
    }

    private void clear_view (Object departing_view)
    {
        String current_state = navigator.getState ();
        View current_view = navigator.getCurrentView ();

        Iterator<Map.Entry<String, View>> it_view = view_name_to_view.entrySet ().iterator ();

        while (it_view.hasNext ())
        {
            Map.Entry<String, View> entry = it_view.next ();

            if (entry.getValue () == departing_view)
            {
                it_view.remove ();

                if (entry.getKey ().equals (current_state)
                    || entry.getValue ().equals (current_view))
                {
                    // We are running from system land, no sessions at all, so
                    // we need to set Vaadin session from navigator UI session
                    VaadinSession current = VaadinSession.getCurrent ();
                    VaadinSession.setCurrent (navigator.getUI ().getSession ());

                    // We always have a dedicated home view
                    navigator.navigateTo (NavigatorManager.HOME);

                    // Back to system land defaults
                    VaadinSession.setCurrent (current);
                }
            }
        }
    }

    private void clear_view_provider (Object departing_view_provider)
    {
        Iterator<Map.Entry<String, ViewProvider>> it_provider = view_name_to_provider.entrySet ().iterator ();

        while (it_provider.hasNext ())
        {
            Map.Entry<String, ViewProvider> entry = it_provider.next ();

            if (entry.getValue () == departing_view_provider)
            {
                it_provider.remove ();
            }
        }
    }

    @Override // ServiceObject.Listener
    public void event (int type, Object serviceObject)
    {
        if (type == ServiceObject.INVALIDATE)
        {
            if (serviceObject instanceof View)
            {
                clear_view (serviceObject);
            }
            else if (serviceObject instanceof ViewProvider)
            {
                clear_view_provider (serviceObject);
            }
        }
    }

    @ServiceObject.Validate
    public void validate ()
    {
        serviceContext.addListener (this, null);
    }

    @ServiceObject.Invalidate
    public void invalidate ()
    {
        // Make null local vars
        view_name_to_provider = null;
        view_name_to_view = null;
        navigatorManager = null;
        navigator = null;
    }
}

// EOF
