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

package org.lucidj.top;

import org.lucidj.api.MenuInstance;
import org.lucidj.api.MenuProvider;
import org.lucidj.api.ServiceContext;

import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewProvider;
import com.vaadin.server.FontAwesome;

import java.util.Map;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;

@Component (immediate = true, publicFactory = false)
@Instantiate
@Provides
public class Top implements MenuProvider, ViewProvider
{
    private final static String NAVID = "top";

    @Requires
    private ServiceContext serviceContext;

    @Override // MenuProvider
    public Map<String, Object> getProperties ()
    {
        return (null);
    }

    @Override // MenuProvider
    public void buildMenuEntries (MenuInstance menu, Map<String, Object> properties)
    {
        menu.addMenuEntry (menu.newMenuEntry ("Top tasks", FontAwesome.TASKS, 200, NAVID));
    }

    @Override // ViewProvider
    public String getViewName (String s)
    {
        if (NAVID.equals (s))
        {
            return (NAVID);
        }
        return null;
    }

    @Override // ViewProvider
    public View getView (String s)
    {
        if (NAVID.equals (s))
        {
            return (serviceContext.newServiceObject (TopView.class));
        }
        return null;
    }

    @Validate
    private void validate ()
    {
        serviceContext.register (TopView.class);
    }
}

// EOF
