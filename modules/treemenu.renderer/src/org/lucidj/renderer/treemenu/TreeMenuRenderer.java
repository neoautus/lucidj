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

package org.lucidj.renderer.treemenu;

import org.lucidj.api.ManagedObject;
import org.lucidj.api.ManagedObjectInstance;
import org.lucidj.api.MenuEntry;
import org.lucidj.api.MenuInstance;
import org.lucidj.api.vui.Renderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.data.Property;
import com.vaadin.event.ItemClickEvent;
import com.vaadin.server.ClientConnector;
import com.vaadin.server.Resource;
import com.vaadin.ui.AbstractSelect;
import com.vaadin.ui.Component;
import com.vaadin.ui.Tree;

import java.util.TreeSet;

public class TreeMenuRenderer implements ManagedObject, Renderer, ItemClickEvent.ItemClickListener
{
    private final static transient Logger log = LoggerFactory.getLogger (TreeMenuRenderer.class);

    private static String CP_ENTRY   = "entry";
    private static String CP_CAPTION = "caption";
    private static String CP_ICON    = "icon";
    private static String CP_NAVID   = "navid";

    private MenuInstance menu_instance;
    private transient boolean update_on_attach;
    private Tree tree_menu;

    public TreeMenuRenderer ()
    {
        tree_menu = new Tree ();
        tree_menu.addStyleName ("ui-treemenu");

        tree_menu.addContainerProperty (CP_ENTRY, MenuEntry.class, null);
        tree_menu.addContainerProperty (CP_CAPTION, String.class, null);
        tree_menu.addContainerProperty (CP_ICON, Resource.class, null);
        tree_menu.addContainerProperty (CP_NAVID, String.class, null);

        tree_menu.setItemCaptionPropertyId (CP_CAPTION);
        tree_menu.setItemCaptionMode (AbstractSelect.ItemCaptionMode.PROPERTY);
        tree_menu.setItemIconPropertyId (CP_ICON);

        tree_menu.addItemClickListener (this);
        tree_menu.setImmediate (true);
        tree_menu.setSelectable (false);

        tree_menu.addAttachListener (new ClientConnector.AttachListener ()
        {
            @Override
            public void attach (ClientConnector.AttachEvent attachEvent)
            {
                if (update_on_attach)
                {
                    render_tree_menu ();
                    update_on_attach = false;
                }
            }
        });

        tree_menu.addDetachListener (new ClientConnector.DetachListener ()
        {
            @Override
            public void detach (ClientConnector.DetachEvent detachEvent)
            {
                // Nothing for now
            }
        });
    }

    private void render_tree_menu ()
    {
        log.debug ("render_tree_menu");

        if (!tree_menu.isAttached ())
        {
            log.debug ("render_tree_menu: update on attach");
            update_on_attach = true;
            return;
        }

        log.debug ("render_tree_menu: updateUI rendering start");

        // Get the logical menu representation...
        TreeSet<MenuEntry> menu_entries = menu_instance.getMenuEntries ();

        // ...and start building the visible UI menu
        tree_menu.removeAllItems ();

        // TODO: EXPAND THIS TO BUILD MENU SUBITEMS
        for (MenuEntry entry: menu_entries)
        {
            Object item_id = tree_menu.addItem ();

            tree_menu.getContainerProperty (item_id, CP_ENTRY).setValue (entry);
            tree_menu.getContainerProperty (item_id, CP_CAPTION).setValue (entry.getTitle ());
            tree_menu.getContainerProperty (item_id, CP_NAVID).setValue (entry.getNavId ());
            tree_menu.getContainerProperty (item_id, CP_ICON).setValue (entry.getIcon ());

            // By default no one has children
            tree_menu.setChildrenAllowed (item_id, false);
        }

        log.debug ("render_tree_menu: updateUI rendering finished");
    }

    public static boolean isCompatible (Object object)
    {
        return (object instanceof MenuInstance);
    }

    @Override // Renderer
    public void objectLinked (Object obj)
    {
        menu_instance = (MenuInstance)obj;
    }

    @Override // Renderer
    public void objectUnlinked ()
    {
        menu_instance = null;
    }

    @Override // Renderer
    public Component renderingComponent ()
    {
        return (tree_menu);
    }

    @Override // Renderer
    public void objectUpdated ()
    {
        render_tree_menu ();
    }

    @Override // ItemClickEvent.ItemClickListener
    public void itemClick (ItemClickEvent itemClickEvent)
    {
        Object item_id = itemClickEvent.getItemId ();
        Property item_entry = tree_menu.getContainerProperty (item_id, CP_ENTRY);
        MenuEntry entry = (item_entry != null)? (MenuEntry)item_entry.getValue (): null;

        if (entry != null)
        {
            menu_instance.fireEventEntrySelected (entry);
        }
    }

    @Override // ManagedObject
    public void validate (ManagedObjectInstance instance)
    {
        // Nop
    }

    @Override // ManagedObject
    public void invalidate (ManagedObjectInstance instance)
    {
        // Nop
    }
}

// EOF
