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

package org.lucidj.newview;

import org.lucidj.api.core.SecurityEngine;
import org.lucidj.api.core.ServiceContext;
import org.lucidj.api.vui.NavigatorManager;
import org.lucidj.api.vui.RendererFactory;

import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewProvider;

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
public class NewProvider implements ViewProvider
{
    @Requires
    private ServiceContext serviceContext;

    @Requires
    private SecurityEngine securityEngine;

    @Requires
    private RendererFactory rendererFactory;

    @Requires
    private NavigatorManager navigatorManager;

    @Context
    private BundleContext bundleContext;

    @Override // ViewProvider
    public String getViewName (String s)
    {
        if (NewView.NAVID.equals (s))
        {
            return (NewView.NAVID);
        }
        return null;
    }

    @Override // ViewProvider
    public View getView (String s)
    {
        if (NewView.NAVID.equals (s))
        {
            return (serviceContext.newServiceObject (NewView.class));
        }
        return null;
    }

    @Validate
    private void validate ()
    {
        serviceContext.putService (bundleContext, SecurityEngine.class, securityEngine);
        serviceContext.putService (bundleContext, RendererFactory.class, rendererFactory);
        serviceContext.putService (bundleContext, NavigatorManager.class, navigatorManager);
        serviceContext.register (NewView.class);
    }
}

// EOF
