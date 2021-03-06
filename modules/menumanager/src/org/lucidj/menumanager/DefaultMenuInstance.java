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
import org.lucidj.api.core.MenuEntry;
import org.lucidj.api.core.MenuInstance;
import org.lucidj.api.core.MenuManager;
import org.lucidj.api.core.MenuProvider;
import org.lucidj.api.vui.Renderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

public class DefaultMenuInstance implements MenuInstance, Renderer.Observable
{
    private final static Logger log = LoggerFactory.getLogger (DefaultMenuInstance.class);

    private TreeSet<MenuEntry> menu_entry_list = new TreeSet<>();
    private EventHelper event_helper;
    private MenuManager menu_manager;
    private volatile boolean menu_changed;
    private Map<String, Object> properties = new HashMap<> ();
    private EventListener event_listener;

    public DefaultMenuInstance (EventHelper event_helper)
    {
        this.event_helper = event_helper;
    }

    @Override // MenuInstance
    public void setMenuManager (MenuManager menu_manager)
    {
        this.menu_manager = menu_manager;
    }

    @Override // MenuInstance
    public TreeSet<MenuEntry> getMenuEntries ()
    {
        if (menu_changed)
        {
            menu_changed = false;

            // Rebuild the LOGICAL menu representation --
            // The representation will later be used by some renderer
            menu_entry_list.clear ();
            menu_manager.buildMenu (this, properties);
        }
        return (menu_entry_list);
    }

    @Override // MenuInstance
    public Map<String, Object> properties ()
    {
        return (properties);
    }

    @Override // MenuInstance
    public MenuEntry newMenuEntry (String title, Object icon, int weight, String navid)
    {
        return (new ComparableMenuEntry (title, icon, weight, navid));
    }

    @Override // MenuInstance
    public void addMenuEntry (MenuEntry menu_entry)
    {
        menu_entry_list.add (menu_entry);
    }

    @Override // MenuInstance
    public void removeMenuEntry (MenuEntry menu_entry)
    {
        menu_entry_list.remove (menu_entry);
    }

    @Override // MenuInstance
    public void menuChanged (MenuProvider menu_provider)
    {
        menu_changed = true;
        event_helper.publish (this);
    }

    @Override // MenuInstance
    public void setEventListener (EventListener listener)
    {
        event_listener = listener;
    }

    @Override // MenuInstance
    public void fireEventEntrySelected (MenuEntry entry)
    {
        if (event_listener != null)
        {
            event_listener.entrySelectedEvent (entry);
        }
    }

    @Override // Renderer.Observable
    public void addObserver (EventHelper.Subscriber observer)
    {
        event_helper.subscribe (observer);
    }

    @Override // Renderer.Observable
    public void deleteObserver (EventHelper.Subscriber observer)
    {
        event_helper.unsubscribe (observer);
    }
}

// EOF
