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

package org.lucidj.palette;

import org.lucidj.api.ComponentInterface;
import org.lucidj.api.ComponentManager;
import org.lucidj.api.ComponentSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.FrameworkUtil;
import org.osgi.util.tracker.BundleTracker;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Context;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Validate;

@Component (immediate = true, publicFactory = false)
@Instantiate
@Provides
public class Palette implements ComponentManager
{
    private final transient static Logger log = LoggerFactory.getLogger (Palette.class);

    private BundleTracker bundle_cleaner;
    private Set<ComponentSet> listeners = new HashSet<> ();
    private HashMap<String, ComponentInterface> component_map = new HashMap<> ();

    @Context
    private BundleContext ctx;

    @Override // ComponentManager
    public ComponentSet newComponentSet ()
    {
        ComponentSet palette_set = new PaletteSet ();
        listeners.add (palette_set);

        // Populate the ComponentSet initial load
        for (ComponentInterface component: component_map.values ())
        {
            palette_set.addingComponent (component);
        }
        return (palette_set);
    }

    private void notify_adding_component (ComponentInterface component)
    {
        for (ComponentInterface.ComponentListener listener: listeners)
        {
            listener.addingComponent (component);
        }
    }

    @Override // ComponentManager
    public boolean register (ComponentInterface component)
    {
        log.info ("{} register: {}", this, component);
        component_map.put (component.toString (), component);
        notify_adding_component (component);
        return (true);
    }

    private void notify_removing_component (ComponentInterface component)
    {
        for (ComponentInterface.ComponentListener listener: listeners)
        {
            listener.removingComponent (component);
        }
    }

    private void clear_components_by_bundle (Bundle bnd)
    {
        // TODO: THREAD-SAFE

        Iterator<ComponentSet> itl = listeners.iterator ();

        // Remove listeners
        while (itl.hasNext ())
        {
            ComponentSet entry = itl.next ();

            if (FrameworkUtil.getBundle (entry.getClass ()) == bnd)
            {
                log.info ("Removing listener: {} for {}", entry, bnd);
                itl.remove ();
            }
        }

        Iterator<Map.Entry<String, ComponentInterface>> itcm = component_map.entrySet ().iterator ();

        // Remove components
        while (itcm.hasNext ())
        {
            Map.Entry<String, ComponentInterface> entry = itcm.next ();
            ComponentInterface component = entry.getValue ();

            if (FrameworkUtil.getBundle (component.getClass ()) == bnd)
            {
                log.info ("Removing component: {} for {}", component, bnd);
                notify_removing_component (component);
                itcm.remove ();
            }
        }
    }

    @Validate
    private void validate ()
    {
        bundle_cleaner = new BundleCleanup (ctx);
        bundle_cleaner.open ();
    }

    @Invalidate
    private void invalidate ()
    {
        bundle_cleaner.close ();
        bundle_cleaner = null;
    }

    class BundleCleanup extends BundleTracker
    {
        BundleCleanup (BundleContext context)
        {
            super (context, Bundle.STOPPING, null);
        }

        @Override
        public void removedBundle (Bundle bundle, BundleEvent event, Object object)
        {
            clear_components_by_bundle (bundle);
        }
    }
}

// EOF
