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

package org.lucidj.containertree;

import org.lucidj.api.Aggregate;
import org.lucidj.api.vui.Renderer;
import org.lucidj.api.ServiceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.data.Container;
import com.vaadin.event.ItemClickEvent;
import com.vaadin.server.Resource;
import com.vaadin.ui.Component;
import com.vaadin.ui.Tree;

import java.util.Map;

import org.osgi.framework.BundleContext;

public class ContainerTreeRenderer extends Tree implements Renderer, ItemClickEvent.ItemClickListener
{
    private final static Logger log = LoggerFactory.getLogger (ContainerTreeRenderer.class);

    private Object object;
    private Container source;

    public ContainerTreeRenderer (ServiceContext serviceContext, BundleContext bundleContext)
    {
        addStyleName ("x-container-tree-renderer");
        setSelectable (false);
        setImmediate (true);
        addItemClickListener (this);
    }

    private void update_components ()
    {
        // Called on objectLinked() and objectUpdated()
    }

    public static boolean isCompatible (Object object)
    {
        return ((Aggregate.adapt (Container.class, object) != null));
    }

    @Override // Renderer
    public void objectLinked (Object obj)
    {
        // Store the full object
        object = obj;

        // Set the Container element
        source = Aggregate.adapt (Container.class, object);
        setContainerDataSource (source);

        // Try to retrieve properties from the object
        Map<String, Object> properties = Aggregate.adapt (Map.class, object);

        if (properties != null)
        {
            // They should indicate what to use as caption and icon properties
            if (properties.containsKey ("@itemCaptionPropertyId"))
            {
                setItemCaptionPropertyId (properties.get ("@itemCaptionPropertyId"));
            }
            if (properties.containsKey ("@itemIconPropertyId"))
            {
                setItemIconPropertyId (properties.get ("@itemIconPropertyId"));
            }
        }
        else // No properties, we'll try to guess something meaningful
        {
            for (Object pid: source.getContainerPropertyIds ())
            {
                if (source.getType (pid).isAssignableFrom (String.class)
                        && getItemCaptionPropertyId () == null)
                {
                    setItemCaptionPropertyId (pid);
                }
                else if (source.getType (pid).isAssignableFrom (Resource.class)
                        && getItemIconPropertyId () == null)
                {
                    setItemIconPropertyId (pid);
                }
            }
        }

        if (properties.containsKey ("@expandItem"))
        {
            expandItem (properties.get ("@expandItem"));
        }
        update_components ();
    }

    @Override // Renderer
    public void objectUnlinked ()
    {
        source = null;
    }

    @Override // Renderer
    public Component renderingComponent ()
    {
        return (this);
    }

    @Override // Renderer
    public void objectUpdated ()
    {
        update_components ();
    }

    @Override // ItemClickEvent.ItemClickListener
    public void itemClick (ItemClickEvent itemClickEvent)
    {
        // There's no need to bubble-up the event since the foreign
        // listeners are already attached directly to this component
        // via bypass. We only need to do local housekeeping.


        if (itemClickEvent.isDoubleClick ()
            && itemClickEvent.getSource () instanceof Tree)
        {
            // Handles directory expand/contract automatically
            Tree tree = (Tree)itemClickEvent.getSource ();
            Object item_id = itemClickEvent.getItemId ();

            if (tree.isExpanded (item_id))
            {
                tree.collapseItem (item_id);
            }
            else
            {
                tree.expandItem (item_id);
            }
        }
    }
}

// EOF
