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

import org.lucidj.api.NavigatorManager;
import org.lucidj.api.ServiceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.navigator.Navigator;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewProvider;
import com.vaadin.server.VaadinSession;
import com.vaadin.ui.AbstractComponent;
import com.vaadin.ui.UI;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.felix.ipojo.annotations.Bind;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Unbind;
import org.apache.felix.ipojo.annotations.Validate;

@Component (immediate = true, publicFactory = false)
@Instantiate
@Provides (specifications = NavigatorManager.class)
class DefaultNavigatorManager implements NavigatorManager
{
    private static final Logger log = LoggerFactory.getLogger (DefaultNavigatorManager.class);

    private static final String ATTR_VIEW_PROVIDER = NavigatorManager.class.getName () + ".view-provider";

    private Map<String, ViewProvider> view_providers;

    @Requires
    private ServiceContext serviceContext;

    public DefaultNavigatorManager ()
    {
        view_providers = new ConcurrentHashMap<> ();
    }

    private ViewProvider get_or_create_proxy_view_provider (Navigator navigator)
    {
        UI current_ui = UI.getCurrent ();
        VaadinSession current_session = (current_ui != null)? current_ui.getSession (): null;
        ViewProvider proxy_view_provider = null;

        if (current_session != null)
        {
            // Get or create the ProxyViewProvider, bound into VaadinSession
            Object view_provider_obj = current_session.getAttribute (ATTR_VIEW_PROVIDER);

            if (view_provider_obj instanceof ViewProvider)
            {
                // TODO: WHAT IF NAVIGATOR CHANGES INSIDE THE SAME UI? CREATE SOME WAY TO UPDATE IT.
                proxy_view_provider = (ViewProvider)view_provider_obj;
            }
            else
            {
                proxy_view_provider = serviceContext.wrapObject (ProxyViewProvider.class, new ProxyViewProvider (this, navigator));
                current_session.setAttribute (ATTR_VIEW_PROVIDER, proxy_view_provider);
            }
        }
        return (proxy_view_provider);
    }

    @Override // NavigatorManager
    public ViewProvider findViewProvider (String navigationState)
    {
        for (Map.Entry<String, ViewProvider> provider_entry: view_providers.entrySet ())
        {
            ViewProvider view_provider = provider_entry.getValue ();
            String view_name = view_provider.getViewName (navigationState);

            if (view_name != null)
            {
                return (view_provider);
            }
        }
        return (null);
    }

    @Override // NavigatorManager
    public boolean configureNavigator (Navigator navigator, Map<String, Object> properties)
    {
        ViewProvider proxy_view_provider = get_or_create_proxy_view_provider (navigator);

        if (proxy_view_provider == null)
        {
            return (false);
        }
        navigator.addProvider (proxy_view_provider);
        return (true);
    }

    @Override
    public boolean navigateTo (String navigationState, Map<String, Object> dataProperties)
    {
        UI ui = UI.getCurrent ();

        if (ui == null)
        {
            return (false);
        }

        Navigator navigator = ui.getNavigator ();

        if (navigator == null)
        {
            return (false);
        }

        ViewProvider proxy_view_provider = get_or_create_proxy_view_provider (navigator);
        String view_name = proxy_view_provider.getViewName (navigationState);

        if (view_name == null)
        {
            return (false);
        }

        View view = proxy_view_provider.getView (view_name);

        if (view instanceof AbstractComponent)
        {
            // Store properties as component data
            ((AbstractComponent)view).setData (dataProperties);
        }
        navigator.navigateTo (navigationState);
        return (true);
    }

    @Bind (aggregate=true, optional=true, specification = ViewProvider.class)
    private void bindViewProvider (ViewProvider view_provider, Map<String, Object> properties)
    {
        String factory_name = (String)properties.get ("factory.name");

        log.info ("Adding view provider: {}", factory_name);
        view_providers.put (factory_name, view_provider);
    }

    @Unbind
    private void unbindViewProvider (ViewProvider view_provider, Map properties)
    {
        String factory_name = (String)properties.get ("factory.name");

        log.info ("Removing view provider: {}", factory_name);
        view_providers.remove (factory_name);
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
}

// EOF
