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

package org.lucidj.search;

import org.lucidj.api.MenuInstance;
import org.lucidj.api.MenuProvider;
import org.lucidj.runtime.Kernel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewProvider;
import com.vaadin.server.ClassResource;
import com.vaadin.server.Resource;

import java.util.Map;

import org.osgi.framework.BundleContext;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Context;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

@Component
@Instantiate
@Provides
public class Search implements MenuProvider, ViewProvider
{
    private final static transient Logger log = LoggerFactory.getLogger (Search.class);
    private final static String V_SEARCH = "search";

    @Context
    private BundleContext context;

    @Override // MenuProvider
    public Map<String, Object> getProperties ()
    {
        return (null);
    }

    @Override // MenuProvider
    public void buildMenuEntries (MenuInstance menu, Map<String, Object> properties)
    {
        // The explicit class is used to bind the resource with its source bundle
        Resource icon = new ClassResource (this.getClass (), "Resources/icons/zoom-seach-icon-16x16.png");
        menu.addMenuEntry (menu.newMenuEntry ("Search", icon, 250, V_SEARCH));
    }

    @Override // ViewProvider
    public String getViewName (String s)
    {
        if (V_SEARCH.equals (s))
        {
            return (V_SEARCH);
        }
        return null;
    }

    @Override // ViewProvider
    public View getView (String s)
    {
        if (V_SEARCH.equals (s))
        {
            return (Kernel.newComponent (SearchView.class));
        }
        return null;
    }
}

// EOF
