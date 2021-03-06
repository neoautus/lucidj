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

import org.lucidj.api.core.ManagedObjectFactory;
import org.lucidj.api.core.ManagedObjectInstance;
import org.lucidj.api.core.MenuInstance;
import org.lucidj.api.core.MenuProvider;
import org.lucidj.api.vui.IconHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewProvider;
//import com.vaadin.server.ClassResource;
import com.vaadin.server.Resource;

import java.util.Map;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

@Component
@Instantiate
@Provides
public class Search implements MenuProvider, ViewProvider
{
    private final static Logger log = LoggerFactory.getLogger (Search.class);
    private final static String V_SEARCH = "search";

    @Requires
    private ManagedObjectFactory object_factory;

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
        // The explicit class is used to bind the resource with its source bundle
        //Resource icon = new ClassResource (this.getClass (), "Resources/icons/zoom-seach-icon-16x16.png");
        Resource icon = iconHelper.getIcon ("apps/plasma-search", 16);
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
            ManagedObjectInstance view_instance = object_factory.wrapObject (new SearchView ());
            return (view_instance.adapt (View.class));
        }
        return null;
    }
}

// EOF
