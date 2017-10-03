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

package org.lucidj.explorer;

import org.lucidj.api.ArtifactDeployer;
import org.lucidj.api.BundleManager;
import org.lucidj.api.IconHelper;
import org.lucidj.api.MenuInstance;
import org.lucidj.api.MenuProvider;
import org.lucidj.api.RendererFactory;
import org.lucidj.api.SecurityEngine;
import org.lucidj.api.ServiceContext;

import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewProvider;
import com.vaadin.server.Resource;

import java.util.Map;
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
public class Explorer implements MenuProvider, ViewProvider
{
    public final static String NAVID = "explorer";
    public final static String OPEN = "open";

    @Context
    private BundleContext context;

    @Requires
    private SecurityEngine security;

    @Requires
    private ServiceContext serviceContext;

    @Requires
    private ArtifactDeployer artifactDeployer;

    @Requires
    private BundleManager bundleManager;

    @Requires
    private RendererFactory rendererFactory;

    @Requires
    private IconHelper iconHelper;

    @Override // MenuProvider
    public Map<String, Object> getProperties ()
    {
        return (null);
    }

    @Override // MenuProvider
    public void buildMenuEntries (MenuInstance menu, Map<String, Object> properties)
    {
        Resource icon = iconHelper.getIcon ("places/folder", 32);
        menu.addMenuEntry (menu.newMenuEntry ("Explorer", icon, 100, NAVID));
    }

    @Override // ViewProvider
    public String getViewName (String navigationState)
    {
        Matcher m;

        if (NAVID.equals (navigationState))
        {
            return (NAVID);
        }
        else if (navigationState.startsWith (OPEN + "/"))  // The '/' is used since 'open' requires args
        {
            return (OPEN);
        }
        else if ((m = BundleView.NAV_PATTERN.matcher (navigationState)).find ())
        {
            return (m.group ());
        }
        return (null);
    }

    @Override // ViewProvider
    public View getView (String viewName)
    {
        if (NAVID.equals (viewName))
        {
            return (serviceContext.newServiceObject (ExplorerView.class));
        }
        else if (OPEN.equals (viewName))
        {
            return (serviceContext.newServiceObject (OpenView.class));
        }
        else if (BundleView.NAV_PATTERN.matcher (viewName).matches ())
        {
            return (serviceContext.newServiceObject (BundleView.class));
        }
        return (null);
    }

    @Validate
    private void validate ()
    {
        serviceContext.publishUrl (context, "/public/styles.css");
        serviceContext.putService (context, SecurityEngine.class, security);
        serviceContext.putService (context, ArtifactDeployer.class, artifactDeployer);
        serviceContext.putService (context, BundleManager.class, bundleManager);
        serviceContext.putService (context, RendererFactory.class, rendererFactory);
        serviceContext.putService (context, IconHelper.class, iconHelper);
        serviceContext.register (ExplorerView.class);
        serviceContext.register (OpenView.class);
        serviceContext.register (BundleView.class);
    }
}

// EOF
