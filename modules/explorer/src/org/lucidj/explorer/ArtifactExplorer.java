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

package org.lucidj.explorer;

import org.lucidj.api.core.Artifact;
import org.lucidj.api.core.EmbeddingContext;
import org.lucidj.api.vui.IconHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.data.Container;
import com.vaadin.data.Item;
import com.vaadin.data.util.HierarchicalContainer;
import com.vaadin.server.Resource;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Date;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.regex.Pattern;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

// TODO: THIS PROBABLY SHOULD BE A SEPARATED COMPONENT
public class ArtifactExplorer
{
    private final static Logger log = LoggerFactory.getLogger (ArtifactExplorer.class);

    private static String PROPERTY_NAME = "Name";
    private static String PROPERTY_SIZE = "Size";
    private static String PROPERTY_ICON = "Icon";
    private static String PROPERTY_LASTMODIFIED = "Last Modified";
    private static String PROPERTY_URI = URI.class.getName ();

    private IconHelper iconHelper;

    private Artifact artifact;
    private EmbeddingContext embeddingContext;
    private Bundle bundle;
    private BundleContext bundleContext;
    private HierarchicalContainer container = new HierarchicalContainer ();

    public ArtifactExplorer (Artifact artifact, IconHelper iconHelper)
    {
        this.artifact = artifact;
        this.iconHelper = iconHelper;

        embeddingContext = artifact.adapt (EmbeddingContext.class);
        bundle = artifact.getMainBundle ();
        bundleContext = bundle.getBundleContext ();

        container.addContainerProperty (PROPERTY_NAME, String.class, "-");
        container.addContainerProperty (PROPERTY_ICON, Resource.class, null);
        container.addContainerProperty (PROPERTY_SIZE, Long.class, -1);
        container.addContainerProperty (PROPERTY_LASTMODIFIED, Date.class, null);
        container.addContainerProperty (PROPERTY_URI, URI.class, null);

        enum_entries ();
        publish_menu ();
    }

    private void publish_menu ()
    {
        // TEST: Publish the container as a navigation component
        Dictionary<String, Object> props = new Hashtable<> ();
        props.put ("@section", "Navigation");
        props.put ("@caption", bundle.getSymbolicName ());
        props.put ("@itemCaptionPropertyId", PROPERTY_NAME);
        props.put ("@itemIconPropertyId", PROPERTY_ICON);
        props.put ("@itemURIPropertyId", PROPERTY_URI);
        props.put ("@expandItem", bundle.getSymbolicName () + "/");
        bundleContext.registerService (Container.class, container, props);
    }

    private void enum_entries ()
    {
        log.info ("EmbeddingContext => {}", embeddingContext);

        //-----------
        // ROOT ITEM
        //-----------

        String artifact_root = bundle.getSymbolicName () + "/";
        Item root = container.addItem (artifact_root);
        root.getItemProperty (PROPERTY_NAME).setValue ("/ bundle [" + bundle.getBundleId () + "]");
        root.getItemProperty (PROPERTY_ICON).setValue (iconHelper.getIcon (artifact.getMimeType (), 32));
        root.getItemProperty (PROPERTY_URI).setValue (URI.create ("navigator://bundle/" + bundle.getSymbolicName ()));

        //----------
        // THE TREE
        //----------

        // Find all bundle entries and add them to context file list
        for (Enumeration<URL> entries = bundle.findEntries ("/", null, true); entries.hasMoreElements (); )
        {
            // Split the entry URL into it's directories (entry_dirs) and it's name (entry_name).
            // If the last element is a directory, entry_name is empty.
            URI entry_uri = URI.create (entries.nextElement ().toString ());
            String[] entry_dirs = entry_uri.getPath ().split (Pattern.quote ("/"), -1);
            String entry_name = entry_dirs [entry_dirs.length - 1];

            String parent_item_path = artifact_root;

            for (int i = 1; i < entry_dirs.length - 1; i++)
            {
                String item_path = parent_item_path + entry_dirs [i] + "/";

                //----------
                // BRANCHES
                //----------

                if (container.getItem (item_path) == null)
                {
                    Item item = container.addItem (item_path);
                    item.getItemProperty (PROPERTY_NAME).setValue (entry_dirs [i]);
                    item.getItemProperty (PROPERTY_ICON).setValue (iconHelper.getIcon ("places/folder", 32));
                    try
                    {
                        URI uri = new URI ("navigator", "bundle", item_path, null);
                        item.getItemProperty (PROPERTY_URI).setValue (uri);
                    }
                    catch (URISyntaxException ignore) {};

                    container.setParent (item_path, parent_item_path);
                }
                parent_item_path = item_path;
            }

            //--------
            // LEAVES
            //--------

            if (!entry_name.isEmpty ())
            {
                String item_path = parent_item_path + entry_name;

                Item item = container.addItem (item_path);
                item.getItemProperty (PROPERTY_NAME).setValue (entry_name);
                Resource icon = iconHelper.getIcon (iconHelper.getMimeIconDescriptor (entry_name), 32);
                item.getItemProperty (PROPERTY_ICON).setValue (icon);
                try
                {
                    URI uri = new URI ("navigator", "browse", "/" + item_path, null);
                    item.getItemProperty (PROPERTY_URI).setValue (uri);
                }
                catch (URISyntaxException ignore) {};

                container.setChildrenAllowed (item_path, false);
                container.setParent (item_path, parent_item_path);
            }
        }
    }
}

// EOF
