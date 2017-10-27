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

package org.lucidj.pathrenderer;

import org.lucidj.api.vui.IconHelper;
import org.lucidj.api.vui.Renderer;
import org.lucidj.api.ServiceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.data.Property;
import com.vaadin.data.util.FilesystemContainer;
import com.vaadin.data.util.ObjectProperty;
import com.vaadin.event.ItemClickEvent;
import com.vaadin.server.Resource;
import com.vaadin.server.Sizeable;
import com.vaadin.ui.Component;
import com.vaadin.ui.Tree;

import java.io.File;
import java.io.FilenameFilter;
import java.nio.file.Path;

import org.osgi.framework.BundleContext;

public class PathRenderer extends Tree implements Renderer, ItemClickEvent.ItemClickListener
{
    private final static Logger log = LoggerFactory.getLogger (PathRenderer.class);

    private Path source;

    private IconHelper iconHelper;

    public PathRenderer (ServiceContext serviceContext, BundleContext bundleContext)
    {
        iconHelper = serviceContext.getService (bundleContext, IconHelper.class);
        setWidth (100, Sizeable.Unit.PERCENTAGE);
        setHeightUndefined ();
        addStyleName ("x-pathrenderer");
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
        if (object instanceof Path)
        {
            // We only render directories
            return (((Path)object).toFile ().isDirectory ());
        }
        return (false);
    }

    @Override // Renderer
    public void objectLinked (Object obj)
    {
        // TODO: HANDLE DIRECTORY NOT FOUND
        source = (Path)obj;
        CustomFilesystemContainer fs = new CustomFilesystemContainer (source);
        setContainerDataSource (fs);
        setItemIconPropertyId (FilesystemContainer.PROPERTY_ICON);
        setItemCaptionPropertyId (FilesystemContainer.PROPERTY_NAME);
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
            File item_id = ((File)itemClickEvent.getItemId ());

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

    public class CustomFilesystemContainer extends FilesystemContainer implements FilenameFilter
    {
        CustomFilesystemContainer (Path rootDir)
        {
            super (rootDir.toFile (), true);
            setFilter (this);
        }

        @Override // FilenameFilter
        public boolean accept (File dir, String name)
        {
            return (!name.startsWith ("."));
        }

        @Override // FilesystemContainer
        public Property getContainerProperty (Object itemId, Object propertyId)
        {
            if (PROPERTY_ICON.equals (propertyId))
            {
                File itemFile = itemId instanceof File? (File)itemId: null;
                Resource icon = iconHelper.getIcon (iconHelper.getMimeIconDescriptor (itemFile), 32);
                // TODO: CACHE ICON ObjectProperty
                return (new ObjectProperty<> (icon));
            }
            return (super.getContainerProperty (itemId, propertyId));
        }
    }
}

// EOF
