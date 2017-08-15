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
import org.lucidj.api.RendererFactory;
import org.lucidj.api.SecurityEngine;
import org.lucidj.api.SerializerEngine;
import org.lucidj.api.ServiceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewProvider;

import java.util.regex.Matcher;

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
public class Browser implements ViewProvider
{
    private final static Logger log = LoggerFactory.getLogger (Browser.class);

    @Context
    private BundleContext context;

    @Requires
    private SecurityEngine securityEngine;

    @Requires
    private ManagedObjectFactory objectFactory;

    @Requires
    private SerializerEngine serializerEngine;

    @Requires
    private ComponentManager componentManager;

    @Requires
    private RendererFactory rendererFactory;

    @Requires
    private BundleManager bundleManager;

    @Requires
    private ServiceContext serviceContext;

    @Override // ViewProvider
    public String getViewName (String navigationState)
    {
        Matcher m;

        if ((m = BrowserView.NAV_PATTERN.matcher (navigationState)).find ())
        {
            return (m.group ());
        }
        return (null);
    }

    @Override // ViewProvider
    public View getView (String viewName)
    {
        if (BrowserView.NAV_PATTERN.matcher (viewName).matches ())
        {
            return (serviceContext.newServiceObject (BrowserView.class));
        }
        return null;
    }

    @Validate
    private void validate ()
    {
        serviceContext.putService (context, SecurityEngine.class, securityEngine);
        serviceContext.putService (context, ManagedObjectFactory.class, objectFactory);
        serviceContext.putService (context, RendererFactory.class, rendererFactory);
        serviceContext.putService (context, ComponentManager.class, componentManager);
        serviceContext.putService (context, SerializerEngine.class, serializerEngine);
        serviceContext.putService (context, BundleManager.class, bundleManager);
        serviceContext.register (BrowserView.class);
    }
}

// EOF
