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

package org.lucidj.formulas;

import org.lucidj.api.MenuInstance;
import org.lucidj.api.MenuProvider;
import org.lucidj.runtime.Kernel;

import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewProvider;
import com.vaadin.server.FontAwesome;

import java.util.Map;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

@Component
@Instantiate
@Provides
public class Formulas implements MenuProvider, ViewProvider
{
    private final static String NAVID = "formulas";

    @Override // MenuProvider
    public Map<String, Object> getProperties ()
    {
        return (null);
    }

    @Override // MenuProvider
    public void buildMenuEntries (MenuInstance menu, Map<String, Object> properties)
    {
        menu.addMenuEntry (menu.newMenuEntry ("Formulas", FontAwesome.FILE_CODE_O, 500, NAVID));
        menu.registry ().register (this);
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
            return (Kernel.newComponent (FormulasView.class));
        }
        return null;
    }
}

// EOF
