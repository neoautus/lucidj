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

import org.lucidj.api.ManagedObject;
import org.lucidj.api.ManagedObjectInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewProvider;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ProxyViewProvider implements ViewProvider, ManagedObject
{
    private final static transient Logger log = LoggerFactory.getLogger (ProxyViewProvider.class);

    private Map<String, ViewProvider> view_name_to_provider;
    private Map<String, View> view_name_to_view;

    private DefaultNavigatorManager navigatorManager;

    public ProxyViewProvider (DefaultNavigatorManager navigatorManager)
    {
        this.navigatorManager = navigatorManager;
        view_name_to_provider = new ConcurrentHashMap<> ();
        view_name_to_view = new ConcurrentHashMap<> ();
    }

    @Override // ViewProvider
    public String getViewName (String s)
    {
        log.info ("getViewName: {}", s);

        ViewProvider view_provider = navigatorManager.findViewProvider (s);

        if (view_provider != null)
        {
            String view_name = view_provider.getViewName (s);

            log.info ("provider={} view_name={}", view_provider, view_name);

            if (view_name != null)
            {
                // We're actually looking for a provider which knowns the requested view
                log.info ("Provider found! {} => {}", view_name, view_provider);
                view_name_to_provider.put (view_name, view_provider);
                return (view_name);
            }
        }
        return (null);
    }

    @Override // ViewProvider
    public View getView (String viewName)
    {
        log.info ("getView: {}", viewName);

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

            log.info ("getView: provider={}", provider);

            view = provider.getView (viewName);
            log.info ("viewName={} view={}", viewName, view);
            // TODO: INVALIDATE CACHE WHEN BUNDLE GOES AWAY
            view_name_to_view.put (viewName, view);
        }
        log.info ("getView (viewName={}) = {}", viewName, view);
        return (view);
    }

    @Override
    public void validate (ManagedObjectInstance instance)
    {
        // Nop
    }

    @Override
    public void invalidate (ManagedObjectInstance instance)
    {
        // Make null local vars
    }
}

// EOF
