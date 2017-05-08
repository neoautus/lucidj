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

import org.lucidj.api.ComponentDescriptor;
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
    private HashMap<ComponentDescriptor, Bundle> component_to_bundle = new HashMap<> ();

    @Context
    private BundleContext ctx;

    @Override // ComponentManager
    public ComponentSet newComponentSet ()
    {
        ComponentSet palette_set = new PaletteSet ();
        listeners.add (palette_set);

        // Populate the ComponentSet initial load
        for (ComponentDescriptor component: component_to_bundle.keySet ())
        {
            palette_set.addingComponent (component);
        }
        return (palette_set);
    }

    @Override // ComponentManager
    public ComponentDescriptor newComponentDescriptor ()
    {
        return (new Descriptor ());
    }

//    @Override
//    public ComponentDescriptor getComponentDescriptor (Object component)
//    {
//        log.info ("getComponentDescriptor (component={})", component);
//
//        if (component instanceof ComponentInterface)
//        {
//            log.info ("getComponentDescriptor (component={}) is ComponentInterface", component);
//
//            String descriptor_id = ((ComponentInterface)component).getDescriptorId ();
//
//            if (descriptor_id != null && component_to_bundle.containsKey (descriptor_id))
//            {
//                log.info ("getComponentDescriptor (descriptor_id={}) ==> ", descriptor_id, component_to_bundle.get (descriptor_id));
//                return (component_to_bundle.get (descriptor_id));
//            }
//        }
//
//        String component_class = component.getClass ().getName ();
//
//        for (ComponentDescriptor descriptor: component_to_bundle.values ())
//        {
//            log.info ("getComponentDescriptor (descriptor_id={}) compare with {}", component_class, component);
//
//            if (descriptor.getComponentClass ().equals (component_class))
//            {
//                log.info ("getComponentDescriptor (descriptor_id={}) ==> ", component, descriptor);
//                return (descriptor);
//            }
//        }
//        return (null);
//    }

    private void notify_adding_component (ComponentDescriptor component)
    {
        for (ComponentInterface.ComponentListener listener: listeners)
        {
            listener.addingComponent (component);
        }
    }

    @Override // ComponentManager
    public boolean register (Bundle bundle, ComponentDescriptor descriptor)
    {
        // TODO: CHECK FOR REPEATED REGISTRATION WITH PROPER DESCRIPTOR_ID
        log.info ("{} register: {}", this, descriptor);
        component_to_bundle.put (descriptor, bundle);
        notify_adding_component (descriptor);
        return (true);
    }

    @Override // ComponentManager
    public boolean register (BundleContext context, ComponentDescriptor descriptor)
    {
        return (register (context.getBundle (), descriptor));
    }

    private void notify_removing_component (ComponentDescriptor component)
    {
        for (ComponentInterface.ComponentListener listener: listeners)
        {
            listener.removingComponent (component);
        }
    }

    private void clear_components_by_bundle (Bundle bnd)
    {
        // TODO: THREAD-SAFE
        log.info ("clear_components_by_bundle (bnd={})", bnd);

        Iterator<Map.Entry<ComponentDescriptor, Bundle>> itcm = component_to_bundle.entrySet ().iterator ();

        // Remove components
        while (itcm.hasNext ())
        {
            Map.Entry<ComponentDescriptor, Bundle> entry = itcm.next ();
            ComponentDescriptor descriptor = entry.getKey ();
            Bundle descriptor_bundle = entry.getValue ();

            log.info ("descriptor: {} bundle={}", descriptor, descriptor_bundle);

            if (descriptor_bundle == bnd)
            {
                log.info ("Removing component: {} for {}", descriptor, bnd);
                notify_removing_component (descriptor);
                itcm.remove ();
            }
        }

        Iterator<ComponentSet> itl = listeners.iterator ();

        // Remove listeners
        while (itl.hasNext ())
        {
            ComponentSet listener = itl.next ();

            log.info ("listener: {} bundle={}", listener, FrameworkUtil.getBundle (listener.getClass ()));

            // TODO: MAP LISTENER TO ITS _ORIGIN_ BUNDLE
            if (FrameworkUtil.getBundle (listener.getClass ()) == bnd)
            {
                log.info ("Removing listener: {} for {}", listener, bnd);
                itl.remove ();
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
            super (context, Bundle.ACTIVE, null);
        }

        @Override
        public void removedBundle (Bundle bundle, BundleEvent event, Object object)
        {
            clear_components_by_bundle (bundle);
        }
    }
}

// EOF
