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

package org.lucidj.managedobject;

import org.lucidj.api.ManagedObject;
import org.lucidj.api.ManagedObjectFactory;
import org.lucidj.api.ManagedObjectInstance;
import org.lucidj.api.ManagedObjectProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
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
public class DefaultManagedObjectFactory implements ManagedObjectFactory
{
    private final static transient Logger log = LoggerFactory.getLogger (DefaultManagedObjectFactory.class);

    private final String PROP_INSTANCES = "@instances";
    private final String PROP_CLASS     = "@class";
    private final String PROP_PROVIDER  = "@provider";

    private BundleTracker bundle_cleaner;

    @Context
    BundleContext ctx;

    private Set<WeakReference<ManagedObjectInstance>> get_instance_set (long bundle_id)
    {
        String filter = "(service.bundleid=" + bundle_id + ")";
        Set<WeakReference<ManagedObjectInstance>> instance_set = null;

        try
        {
            ServiceReference[] ref = ctx.getServiceReferences (Set.class.getName (), filter);

            if (ref != null)
            {
                instance_set = (Set<WeakReference<ManagedObjectInstance>>)ref [0].getProperty (PROP_INSTANCES);

                if (ref.length != 1)
                {
                    log.error ("Internal error: More than 1 instance tracker found: {}", ref);
                }
            }
        }
        catch (InvalidSyntaxException ignore) {};

        if (instance_set == null)
        {
            // No instance set, create on demand
            instance_set = new HashSet<> ();
            Dictionary<String, Object> instance_props = new Hashtable<> ();
            instance_props.put (PROP_INSTANCES, instance_set);
            Bundle base_bundle = ctx.getBundle (bundle_id);
            BundleContext base_bundle_context = base_bundle.getBundleContext ();
            base_bundle_context.registerService (Set.class.getName (), instance_set, instance_props);
        }
        return (instance_set);
    }

    private Set<WeakReference<ManagedObjectInstance>> get_instance_set (Bundle bundle)
    {
        return (get_instance_set (bundle.getBundleId ()));
    }

    @Override
    public ManagedObjectInstance register (String clazz, ManagedObjectProvider provider, Map<String, Object> properties)
    {
        log.info ("register: Adding {} for {}", provider, clazz);

        // Retrieve the provider bundle context, where the service will be attached
        Bundle provider_bundle = FrameworkUtil.getBundle (provider.getClass ());
        BundleContext provider_context = provider_bundle.getBundleContext ();

        // Attach the service to the bundle
        Dictionary<String, Object> props = new Hashtable<> ();
        props.put (PROP_PROVIDER, provider);
        props.put (PROP_INSTANCES, get_instance_set (provider_bundle));
        props.put (PROP_CLASS, clazz);
        provider_context.registerService (ManagedObjectProvider.class.getName (), provider, props);

        ManagedObjectInstance ref = new DefaultManagedObjectInstance (null);
        ref.setProperty (ManagedObjectInstance.PROVIDER, provider);
        ref.setProperty (ManagedObjectInstance.CLASS, clazz);
        return (ref);
    }

    @Override
    public ManagedObjectInstance register (Class clazz, ManagedObjectProvider provider, Map<String, Object> properties)
    {
        return (register (clazz.getName (), provider, properties));
    }

    @Override // ManagedObjectFactory
    public ManagedObjectInstance[] getManagedObjects (String clazz, String filter)
    {
        List<ManagedObjectInstance> found_objects = new ArrayList<> ();
        String cf = "(" + PROP_CLASS + "=" + clazz + ")";
        String fp = filter == null? cf: "&" + cf + filter;
        ServiceReference[] provider_list;

        try
        {
            provider_list = ctx.getServiceReferences (ManagedObjectProvider.class.getName (), fp);
        }
        catch (InvalidSyntaxException e)
        {
            provider_list = null;
        }

        log.info ("getManagedObjects clazz={}", clazz);
        log.info ("provider_list = {}", provider_list);

        if (provider_list == null)
        {
            return (new ManagedObjectInstance[0]);
        }

        for (ServiceReference provider_ref: provider_list)
        {
            ManagedObjectProvider provider = (ManagedObjectProvider)provider_ref.getProperty (PROP_PROVIDER);
            ManagedObjectInstance ref = new DefaultManagedObjectInstance (null);
            ref.setProperty (ManagedObjectInstance.PROVIDER, provider);
            ref.setProperty (ManagedObjectInstance.CLASS, clazz);
            found_objects.add (ref);
            log.info ("add ref={}", ref);
        }

        return (found_objects.toArray (new ManagedObjectInstance[0]));
    }

    @Override // ManagedObjectFactory
    public ManagedObjectInstance[] getManagedObjects (Class clazz, String filter)
    {
        return (getManagedObjects (clazz.getName (), filter));
    }

    @Override // ManagedObjectFactory
    public ManagedObjectInstance wrapObject (ManagedObject managed_object)
    {
        log.info ("wrapInstance: managed_object={}", managed_object);

        // Create the instance...
        DefaultManagedObjectInstance new_instance = new DefaultManagedObjectInstance (null);
        new_instance.setProperty (ManagedObjectInstance.CLASS, managed_object.getClass ().getName ());

        // ...set the ManagedObject...
        new_instance._setManagedObject (managed_object);

        // ...and register it within the class instance set
        Set<WeakReference<ManagedObjectInstance>> instance_set = get_instance_set (new_instance.getBundle ());

        if (instance_set != null)
        {
            instance_set.add (new WeakReference<> ((ManagedObjectInstance)new_instance));
        }

        // Validate ManagedObject
        managed_object.validate (new_instance);

        log.info ("Wrapped instance {} from {}", new_instance, managed_object);

        return (new_instance);
    }

    @Override // ManagedObjectFactory
    public ManagedObjectInstance newInstance (ManagedObjectInstance descriptor)
    {
        if (!DefaultManagedObjectInstance.class.isAssignableFrom (descriptor.getClass ()))
        {
            return (null);
        }

        DefaultManagedObjectInstance full_descriptor = (DefaultManagedObjectInstance)descriptor;

        ManagedObjectProvider provider =
            (ManagedObjectProvider)full_descriptor.getProperty (ManagedObjectInstance.PROVIDER);
        String ref_class = (String)full_descriptor.getProperty (ManagedObjectInstance.CLASS);
        DefaultManagedObjectInstance new_instance = null;

        log.info ("newInstance: provider={} ref_class={} ref={}", provider, ref_class, full_descriptor);

        if (provider != null && ref_class != null)
        {
            // The provided descriptor contains desired object properties
            Bundle provider_bundle = FrameworkUtil.getBundle (provider.getClass ());
            Map<String, Object> properties = full_descriptor.internalGetProperties ();

            // Create the instance...
            new_instance = new DefaultManagedObjectInstance (properties);
            new_instance.setProperty (ManagedObjectInstance.PROVIDER, provider);
            new_instance.setProperty (ManagedObjectInstance.CLASS, ref_class);

            // ...create new ManagedObject itself...
            ManagedObject new_object = provider.newObject (ref_class, new_instance);

            // ...set the ManagedObject...
            new_instance._setManagedObject (new_object);

            // ...and register it within the class instance set
            Set<WeakReference<ManagedObjectInstance>> instance_set = get_instance_set (provider_bundle);

            if (instance_set != null)
            {
                instance_set.add (new WeakReference<> ((ManagedObjectInstance)new_instance));
            }

            // Validate ManagedObject
            new_object.validate (new_instance);

            log.info ("New instance {} of {}", new_instance, new_object);
        }

        return (new_instance);
    }

    @Override // ManagedObjectFactory
    public ManagedObjectInstance newInstance (String clazz, Map<String, Object> properties)
    {
        ManagedObjectInstance[] available_providers = getManagedObjects (clazz, null);

        if (available_providers.length > 0 && available_providers [0] instanceof DefaultManagedObjectInstance)
        {
            DefaultManagedObjectInstance descriptor = (DefaultManagedObjectInstance)available_providers [0];

            if (properties != null)
            {
                descriptor.internalGetProperties ().putAll (properties);
            }
            return (newInstance (descriptor));
        }
        return (null);
    }

    @Override // ManagedObjectFactory
    public ManagedObjectInstance newInstance (Class clazz, Map<String, Object> properties)
    {
        return (newInstance (clazz.getName (), properties));
    }

    private void clear_components_by_bundle (Bundle provider_bundle)
    {
//        synchronized (class_to_provider)
//        {
//            // Remove all providers/classes originated from provider_bundle
//            Iterator<Map.Entry<String, Set<ManagedObjectProvider>>> class_it = class_to_provider.entrySet ().iterator ();
//
//            while (class_it.hasNext ())
//            {
//                Map.Entry<String, Set<ManagedObjectProvider>> class_entry = class_it.next ();
//                Set<ManagedObjectProvider> providers = class_entry.getValue ();
//                Iterator<ManagedObjectProvider> it = providers.iterator ();
//
//                // Remove all providers originated from provider_bundle
//                while (it.hasNext ())
//                {
//                    ManagedObjectProvider provider = it.next ();
//
//                    if (FrameworkUtil.getBundle (provider.getClass ()) == provider_bundle)
//                    {
//                        log.info ("Removing provider: {} for {}", provider, provider_bundle);
//                        it.remove ();
//                    }
//                }
//
//                // Check whether the set is empty
//                if (providers.isEmpty ())
//                {
//                    // We don't have any providers for the class
//                    class_it.remove ();
//                    log.info ("Removed class registration: {}", class_entry.getKey ());
//                }
//            }
//        }
//
//        Set<WeakReference<ManagedObjectInstance>> instance_set = bundle_to_set.get (provider_bundle);
//
//        // Invalidate all orphaned instances
//        if (instance_set != null)
//        {
//            log.info ("Cleaning {} instances from {}", instance_set.size (), provider_bundle);
//
//            for (WeakReference<ManagedObjectInstance> instance_ref: instance_set)
//            {
//                ManagedObjectInstance instance = instance_ref.get ();
//
//                if (instance != null && instance instanceof DefaultManagedObjectInstance)
//                {
//                    ManagedObject dying_object = instance.adapt (ManagedObject.class);
//
//                    if (dying_object != null)
//                    {
//                        dying_object.invalidate (instance);
//                    }
//                }
//            }
//
//            bundle_to_set.remove (provider_bundle);
//        }
    }

    @Validate
    private void validate ()
    {
        log.info ("ManagedObjectFactory started.");
        bundle_cleaner = new BundleCleanup (ctx);
        bundle_cleaner.open ();
    }

    @Invalidate
    private void invalidate ()
    {
        bundle_cleaner.close ();
        bundle_cleaner = null;
        log.info ("ManagedObjectFactory terminated.");
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
