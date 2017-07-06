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

package org.lucidj.browser;

import org.lucidj.api.BundleManager;
import org.lucidj.api.ComponentManager;
import org.lucidj.api.ManagedObjectFactory;
import org.lucidj.api.ManagedObjectInstance;
import org.lucidj.api.RendererFactory;
import org.lucidj.api.SecurityEngine;
import org.lucidj.api.SerializerEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewProvider;

import java.util.regex.Matcher;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

@Component
@Instantiate
@Provides
public class Browser implements ViewProvider
{
    private final static Logger log = LoggerFactory.getLogger (Browser.class);

    @Requires
    private SecurityEngine security;

    @Requires
    private ManagedObjectFactory objectFactory;
    private static ManagedObjectFactory static_objectFactory;

    @Requires
    private SerializerEngine serializer;

    @Requires
    private ComponentManager componentManager;
    private static ComponentManager static_componentManager;

    @Requires
    private RendererFactory rendererFactory;
    private static RendererFactory static_rendererFactory;

    @Requires
    private BundleManager bundleManager;

    public Browser ()
    {
        static_objectFactory = objectFactory;
        static_rendererFactory = rendererFactory;
        static_componentManager = componentManager;
    }

    public static ManagedObjectFactory getObjectFactory ()
    {
        return (static_objectFactory);
    }

    public static RendererFactory getRendererFactory ()
    {
        return (static_rendererFactory);
    }

    public static ComponentManager getComponentManager ()
    {
        return (static_componentManager);
    }

    @Override // ViewProvider
    public String getViewName (String navigationState)
    {
        Matcher m;

        if ((m = BrowserView.NAV_PATTERN.matcher (navigationState)).find ())
        {
            log.info ("m.group() = {}", m.group ());
            return (m.group ());
        }
        return (null);
    }

    @Override // ViewProvider
    public View getView (String viewName)
    {
        if (BrowserView.NAV_PATTERN.matcher (viewName).matches ())
        {
            // TODO: wrapObject() IS ANOTHER USE-CASE (FACTORY-LESS ManagedObject)
            ManagedObjectInstance view_instance =
                objectFactory.wrapObject (new BrowserView (security, serializer, componentManager, bundleManager));
            return (view_instance.adapt (View.class));
        }
        return null;
    }
}

// EOF
