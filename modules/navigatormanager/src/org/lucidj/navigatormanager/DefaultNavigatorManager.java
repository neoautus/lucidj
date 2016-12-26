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

package org.lucidj.navigatormanager;

import org.lucidj.api.NavigatorManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.navigator.Navigator;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewProvider;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.felix.ipojo.annotations.Bind;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Unbind;
import org.apache.felix.ipojo.annotations.Validate;

@Component (immediate = true)
@Instantiate
@Provides (specifications = NavigatorManager.class)
class DefaultNavigatorManager implements NavigatorManager, ViewProvider
{
    private final static transient Logger log = LoggerFactory.getLogger (DefaultNavigatorManager.class);

    private Map<String, ViewProvider> view_providers;
    private Map<String, String> view_name_to_provider;

    public DefaultNavigatorManager ()
    {
        view_providers = new ConcurrentHashMap<> ();
        view_name_to_provider = new ConcurrentHashMap<> ();
    }

    @Bind (aggregate=true, optional=true, specification = ViewProvider.class)
    private void bindViewProvider (ViewProvider view_provider, Map<String, Object> properties)
    {
        String factory_name = (String)properties.get ("factory.name");

        log.info ("Adding view provider: {}", factory_name);
        view_providers.put (factory_name, view_provider);
    }

    private void clear_all_matching_values (String which_value)
    {
        Iterator<Map.Entry<String, String>> it = view_name_to_provider.entrySet ().iterator ();

        while (it.hasNext ())
        {
            Map.Entry<String, String> entry = it.next ();

            if (which_value.equals (entry.getValue ()))
            {
                it.remove ();
            }
        }
    }

    @Unbind
    private void unbindViewProvider (ViewProvider view_provider, Map properties)
    {
        String factory_name = (String)properties.get ("factory.name");

        log.info ("Removing view provider: {}", factory_name);
        view_providers.remove (factory_name);
        clear_all_matching_values (factory_name);
    }

    @Validate
    private void validate ()
    {
        log.info ("DefaultNavigatorManager started");
    }

    @Invalidate
    private void invalidate ()
    {
        log.info ("DefaultNavigatorManager stopped");
    }

    @Override // NavigatorManager
    public boolean configureNavigator (Navigator navigator, Map<String, Object> properties)
    {
        // We are the dinamic view provider :)
        navigator.addProvider (this);
        return (true);
    }

    @Override // ViewProvider
    public String getViewName (String s)
    {
        log.info ("getViewName: {}", s);

        for (Map.Entry<String, ViewProvider> provider_entry: view_providers.entrySet ())
        {
            ViewProvider provider = provider_entry.getValue ();
            String view_name = provider.getViewName (s);

            log.info ("provider={} view_name={}", provider, view_name);

            if (view_name != null)
            {
                log.info ("Provider found! {} => {}", view_name, provider_entry.getKey ());
                // We're actually looking for a provider which knowns the requested view
                view_name_to_provider.put (view_name, provider_entry.getKey ());
                return (view_name);
            }
        }

        return (null);
    }

    @Override // ViewProvider
    public View getView (String s)
    {
        log.info ("getView: {}", s);

        String provider_name = view_name_to_provider.get (s);

        if (provider_name == null)
        {
            log.error ("No provider associated with view '{}'", s);
            return (null);
        }

        ViewProvider provider = view_providers.get (provider_name);

        if (provider == null)
        {
            log.error ("Provider '{}' not available", provider_name);
            return (null);
        }

        log.info ("getView: provider={}", provider);
        return (provider.getView (s));
    }
}
