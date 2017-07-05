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
import org.lucidj.api.ManagedObjectFactory;
import org.lucidj.api.ManagedObjectInstance;
import org.lucidj.api.MenuInstance;
import org.lucidj.api.MenuProvider;
import org.lucidj.api.SecurityEngine;

import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewProvider;
import com.vaadin.server.FontAwesome;

import java.util.Map;
import java.util.regex.Matcher;

import org.osgi.framework.BundleContext;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Context;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

@Component
@Instantiate
@Provides
public class Explorer implements MenuProvider, ViewProvider
{
    public final static String NAVID = "home";
    public final static String OPEN = "open";

    @Context
    private BundleContext context;

    @Requires
    private SecurityEngine security;

    @Requires
    private ManagedObjectFactory object_factory;

    @Requires
    private ArtifactDeployer artifactDeployer;

    @Override // MenuProvider
    public Map<String, Object> getProperties ()
    {
        return (null);
    }

    @Override // MenuProvider
    public void buildMenuEntries (MenuInstance menu, Map<String, Object> properties)
    {
        menu.addMenuEntry (menu.newMenuEntry ("Explorer", FontAwesome.FOLDER_OPEN_O, 100, NAVID));
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
            ManagedObjectInstance view_instance = object_factory.wrapObject (new ExplorerView (security));
            return (view_instance.adapt (View.class));
        }
        else if (OPEN.equals (viewName))
        {
            ManagedObjectInstance view_instance = object_factory.wrapObject (new OpenView (artifactDeployer));
            return (view_instance.adapt (View.class));
        }
        else if (BundleView.NAV_PATTERN.matcher (viewName).matches ())
        {
            ManagedObjectInstance view_instance = object_factory.wrapObject (new BundleView (context, artifactDeployer));
            return (view_instance.adapt (View.class));
        }
        return (null);
    }
}

// EOF
