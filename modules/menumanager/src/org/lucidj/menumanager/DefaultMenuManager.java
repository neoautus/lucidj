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

package org.lucidj.menumanager;

import org.lucidj.api.core.EventHelper;
import org.lucidj.api.core.MenuInstance;
import org.lucidj.api.core.MenuManager;
import org.lucidj.api.core.MenuProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.felix.ipojo.annotations.Bind;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Unbind;
import org.apache.felix.ipojo.annotations.Validate;

@Component
@Instantiate
@Provides (specifications = MenuManager.class)
public class DefaultMenuManager implements MenuManager
{
    private final static Logger log = LoggerFactory.getLogger (MenuManager.class);

    private List<MenuProvider> menu_provider_list = new ArrayList<> ();
    private List<WeakReference<MenuInstance>> menu_instance_listeners = new ArrayList<> ();

    @Requires
    private EventHelper.Factory eventHelperFactory;

    private void notify_changes (MenuProvider provider)
    {
        Iterator<WeakReference<MenuInstance>> iterator = menu_instance_listeners.iterator();

        while (iterator.hasNext())
        {
            MenuInstance menu_instance = iterator.next ().get ();

            if (menu_instance == null)
            {
                iterator.remove();
                continue;
            }

            menu_instance.menuChanged (provider);
        }
    }

    @Override // MenuManager
    public void buildMenu (MenuInstance menu_instance, Map<String, Object> properties)
    {
        log.debug ("buildMenu: menu_instance={} properties={}", menu_instance, properties);

        for (MenuProvider provider: menu_provider_list)
        {
            log.debug ("buildMenuEntries: provider={}", provider);
            provider.buildMenuEntries (menu_instance, properties);
        }

        log.debug ("buildMenu: finished.");
    }

    @Override // MenuManager
    public MenuInstance newMenuInstance (Map<String, Object> properties)
    {
        MenuInstance new_menu = new DefaultMenuInstance (eventHelperFactory.newInstance ());

        // TODO: MANAGE MenuInstance DISPOSAL AND CLEANUP

        synchronized (menu_provider_list)
        {
            // Fill menu
            buildMenu (new_menu, properties);
        }

        // Keep all created menus listening changes
        new_menu.setMenuManager (this);
        menu_instance_listeners.add (new WeakReference<MenuInstance> (new_menu));

        return (new_menu);
    }

    @Validate
    private boolean validate ()
    {
        log.info ("MenuManager started.");
        return (true);
    }

    @Invalidate
    private void invalidate ()
    {
        log.info ("MenuManager terminated.");
    }

    @Bind (aggregate=true, optional=true, specification = MenuProvider.class)
    private void bindMenuProvider (MenuProvider menu_provider)
    {
        log.info ("bindMenuProvider: Adding {}", menu_provider);

        synchronized (menu_provider_list)
        {
            menu_provider_list.add (menu_provider);
        }

        notify_changes (menu_provider);
    }

    @Unbind
    private void unbindMenuProvider (MenuProvider menu_provider)
    {
        log.info ("unbindMenuProvider: Removing {}", menu_provider);

        synchronized (menu_provider_list)
        {
            menu_provider_list.remove (menu_provider);
        }

        notify_changes (menu_provider);
    }
}

// EOF
