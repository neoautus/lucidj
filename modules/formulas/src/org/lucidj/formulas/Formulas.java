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

package org.lucidj.formulas;

import org.lucidj.api.ComponentManager;
import org.lucidj.api.ManagedObjectFactory;
import org.lucidj.api.ManagedObjectInstance;
import org.lucidj.api.MenuInstance;
import org.lucidj.api.MenuProvider;
import org.lucidj.api.RendererFactory;
import org.lucidj.api.SecurityEngine;
import org.lucidj.api.SerializerEngine;

import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewProvider;
import com.vaadin.server.FontAwesome;

import java.util.Map;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

@Component
@Instantiate
@Provides
public class Formulas implements MenuProvider, ViewProvider
{
    private final static String NAVID = "formulas";

    @Requires
    private SecurityEngine security;

    @Requires
    private ManagedObjectFactory object_factory;

    @Requires
    private SerializerEngine serializer;

    @Requires
    private ComponentManager componentManager;

    @Requires
    private RendererFactory rendererFactory;
    private static RendererFactory static_rendererFactory;

    public Formulas ()
    {
        static_rendererFactory = rendererFactory;
    }

    public static RendererFactory getRendererFactory ()
    {
        return (static_rendererFactory);
    }

    @Override // MenuProvider
    public Map<String, Object> getProperties ()
    {
        return (null);
    }

    @Override // MenuProvider
    public void buildMenuEntries (MenuInstance menu, Map<String, Object> properties)
    {
        menu.addMenuEntry (menu.newMenuEntry ("Formulas", FontAwesome.FILE_CODE_O, 500, NAVID));
    }

    @Override // ViewProvider
    public String getViewName (String s)
    {
        // Split NAVID:ARGS
        if (s.contains (":"))
        {
            s = s.substring (0, s.indexOf (":"));
        }

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
            // TODO: wrapObject() IS ANOTHER USE-CASE (FACTORY-LESS ManagedObject)
            ManagedObjectInstance view_instance = object_factory.wrapObject (new FormulasView (security, serializer, componentManager));
            return (view_instance.adapt (View.class));
        }
        return null;
    }
}

// EOF
