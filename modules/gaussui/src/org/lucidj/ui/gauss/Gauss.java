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

package org.lucidj.ui.gauss;

import org.lucidj.api.core.MenuManager;
import org.lucidj.api.core.ServiceContext;
import org.lucidj.api.core.ServiceObject;
import org.lucidj.api.vui.DesktopInterface;
import org.lucidj.api.vui.NavigatorManager;
import org.lucidj.api.vui.RendererFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewProvider;

import java.util.Map;

import org.osgi.framework.BundleContext;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Context;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;

@Component (immediate = true, publicFactory = false)
@Instantiate
@Provides
public class Gauss implements ViewProvider, ServiceObject.Provider
{
    private final static Logger log = LoggerFactory.getLogger (Gauss.class);

    @Context
    private BundleContext bundleContext;

    @Requires
    private ServiceContext serviceContext;

    @Requires
    private MenuManager menuManager;

    @Requires
    private NavigatorManager navigatorManager;

    @Requires
    private RendererFactory rendererFactory;

    @Override // ViewProvider
    public String getViewName (String navigationState)
    {
        if (Home.NAVID.equals (navigationState))
        {
            return (Home.NAVID);
        }
        return (null);
    }

    @Override // ViewProvider
    public View getView (String viewName)
    {
        if (Home.NAVID.equals (viewName))
        {
            return (serviceContext.newServiceObject (Home.class));
        }
        return (null);
    }

    @Override // ServiceObject.Provider
    public Object newObject (String objectClassName, Map<String, Object> properties)
    {
        return (serviceContext.wrapObject (DesktopInterface.class, new GaussUI (serviceContext, bundleContext)));
    }

    @Validate
    private boolean validate ()
    {
        log.info ("Gauss UI Provider started");
        serviceContext.publishUrl (bundleContext, "/public/styles.css");
        serviceContext.putService (bundleContext, MenuManager.class, menuManager);
        serviceContext.putService (bundleContext, NavigatorManager.class, navigatorManager);
        serviceContext.putService (bundleContext, RendererFactory.class, rendererFactory);
        serviceContext.register (DesktopInterface.class, this);
        serviceContext.register (Home.class);
        return (true);
    }
}

// EOF
