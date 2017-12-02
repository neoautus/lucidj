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

package org.lucidj.navtool;

import org.lucidj.api.vui.IconHelper;
import org.lucidj.api.vui.NavTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.data.Container;
import com.vaadin.data.Item;
import com.vaadin.data.util.HierarchicalContainer;
import com.vaadin.server.Resource;

import java.net.URI;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Context;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

@Component (immediate = true, publicFactory = false)
@Instantiate
@Provides
public class NavToolService implements NavTool
{
    private final static Logger log = LoggerFactory.getLogger (NavToolService.class);

    @Requires
    private IconHelper iconHelper;

    @Context
    private BundleContext bundleContext;

    private Map<Integer, NavContainer> handle_to_container = new HashMap<> ();
    private Map<String, Integer> id_to_handle = new HashMap<> ();

    private String make_id (String section, String caption)
    {
        return ((section == null? "": section) + "." + (caption == null? "": caption));
    }

    public int getHandle (String section, String caption)
    {
        String id = make_id (section, caption);

        if (id_to_handle.containsKey (id))
        {
            return (id_to_handle.get (id));
        }
        return (0);
    }

    public int publish (String section, String caption)
    {
        int handle = getHandle (section, caption);

        if (handle != 0)
        {
            return (handle);
        }

        NavContainer container = new NavContainer ();

        handle = container.newHandle ();
        handle_to_container.put (handle, container);
        id_to_handle.put (make_id (section, caption), handle);

        Dictionary<String, Object> props = container.getProperties ();
        props.put ("@section", section == null? "": section);
        props.put ("@caption", caption == null? "": caption);
        props.put ("@handle", handle);
        props.put ("@itemCaptionPropertyId", PROPERTY_NAME);
        props.put ("@itemIconPropertyId", PROPERTY_ICON);
        props.put ("@itemURIPropertyId", PROPERTY_URI);
        ServiceRegistration reg = bundleContext.registerService (Container.class, container, props);
        container.setServiceRegistration (reg);
        return (handle);
    }

    public Container hackGetContainer (int handle)
    {
        // Use at your own risk :)
        return (handle_to_container.get (handle));
    }

    public Object addItem (int handle, Object parentItemId, String name, Resource icon, URI uri, Object itemId)
    {
        log.info ("addItem: handle={} parentItemId={} name={} icon={} uri={} itemId={}",
            handle, parentItemId, name, icon, uri, itemId);

        NavContainer container = handle_to_container.get (handle);

        if (itemId == null)
        {
            itemId = container.newHandle ();
        }

        Item item = container.addItem (itemId);
        container.setChildrenAllowed (itemId, false);

        if (parentItemId != null)
        {
            container.setChildrenAllowed (parentItemId, true);
            container.setParent (itemId, parentItemId);
        }

        item.getItemProperty (PROPERTY_NAME).setValue (name);

        if (icon != null)
        {
            item.getItemProperty (PROPERTY_ICON).setValue (icon);
        }

        if (uri != null)
        {
            item.getItemProperty (PROPERTY_URI).setValue (uri);
        }
        return (itemId);
    }

    public Object addItem (int handle, Object parentItemId, String name, String icon, String uri, Object itemId)
    {
        Resource item_icon = (icon == null || icon.isEmpty ())? null: iconHelper.getIcon (icon, 32);
        URI item_uri = (uri == null || uri.isEmpty ())? null: URI.create (uri);
        return (addItem (handle, parentItemId, name, item_icon, item_uri, itemId));
    }

    public int addItem (int handle, int parentItemId, String name, String icon, String uri, int itemId)
    {
        return ((Integer)addItem (handle, new Integer (parentItemId), name, icon, uri, new Integer (itemId)));
    }

    public int addItem (int handle, int parentItemId, String name, String icon, String uri)
    {
        return ((Integer)addItem (handle, parentItemId, name, icon, uri, null));
    }

    public Object addItem (int handle, String name, String icon, String uri, Object itemId)
    {
        return (addItem (handle, null, name, icon, uri, itemId));
    }

    public int addItem (int handle, String name, String icon, String uri)
    {
        return ((Integer)addItem (handle, name, icon, uri, null));
    }

    public boolean containsId (int handle, Object itemId)
    {
        Container container = handle_to_container.get (handle);
        return (container != null && container.containsId (itemId));
    }

    public boolean containsId (int handle, int itemId)
    {
        return (containsId (handle, new Integer (itemId)));
    }

    public void setChildrenAllowed (int handle, Object itemId, boolean childrenAllowed)
    {
        if (handle_to_container.containsKey (handle))
        {
            HierarchicalContainer container = handle_to_container.get (handle);
            container.setChildrenAllowed (itemId, childrenAllowed);
        }
    }

    public void setChildrenAllowed (int handle, int itemId, boolean childrenAllowed)
    {
        setChildrenAllowed (handle, new Integer (itemId), childrenAllowed);
    }

    public void setParent (int handle, Object itemId, Object newParentId)
    {
        if (handle_to_container.containsKey (handle))
        {
            HierarchicalContainer container = handle_to_container.get (handle);
            container.setChildrenAllowed (newParentId, true);
            container.setParent (itemId, newParentId);
        }
    }

    public void setParent (int handle, int itemId, int newParentId)
    {
        setParent (handle, new Integer (itemId), new Integer (newParentId));
    }

    public void setExpandItem (int handle, Object itemId)
    {
        log.info ("setExpandItem: handle={} itemId={}", handle, itemId);
        if (handle_to_container.containsKey (handle))
        {
            NavContainer container = handle_to_container.get (handle);
            log.info ("setExpandItem: container={}", container);
            container.setChildrenAllowed (itemId, true);
            Dictionary<String, Object> properties = container.getProperties ();
            properties.put ("@expandItem", itemId);
            container.updateProperties (properties);
        }
    }

    public void setExpandItem (int handle, int itemId)
    {
        setExpandItem (handle, new Integer (itemId));
    }
}

// EOF
