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

package org.lucidj.httpservices;

import org.lucidj.api.core.MenuInstance;
import org.lucidj.api.core.MenuProvider;
import org.lucidj.api.core.ServiceContext;
import org.lucidj.api.vui.IconHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewProvider;
import com.vaadin.server.ClassResource;
import com.vaadin.server.Resource;

import java.util.Map;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;

@Component (immediate = true, publicFactory = false)
@Instantiate
@Provides
public class HttpServices implements MenuProvider, ViewProvider
{
    private final static Logger log = LoggerFactory.getLogger (HttpServices.class);
    private final static String NAVID = "httpservices";

    @Requires
    private ServiceContext serviceContext;

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
        Resource icon = new ClassResource (this.getClass (), "META-INF/http-icon-32.svg");
        menu.addMenuEntry (menu.newMenuEntry ("Http Services", icon, 250, NAVID));
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
            return (serviceContext.newServiceObject (HttpServicesView.class));
        }
        return null;
    }

    @Validate
    private void validate ()
    {
        serviceContext.register (HttpServicesView.class);
    }
}

// EOF
