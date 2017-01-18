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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.apache.felix.ipojo.annotations.Bind;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Unbind;
import org.apache.felix.ipojo.annotations.Validate;

@Component (immediate = true, publicFactory = false)
@Instantiate
@Provides
public class DefaultManagedObjectFactory implements ManagedObjectFactory
{
    private final static transient Logger log = LoggerFactory.getLogger (DefaultManagedObjectFactory.class);

    private final Map<String, Set<ManagedObjectProvider>> class_to_provider = new HashMap<> ();
    private final Map<Bundle, Set<WeakReference<ManagedObjectInstance>>> bundle_to_set = new HashMap<> ();

    @Override // ManagedObjectFactory
    public ManagedObjectInstance[] getManagedObjects (String clazz, String filter)
    {
        List<ManagedObjectInstance> found_objects = new ArrayList<> ();
        Set<ManagedObjectProvider> provider_list = class_to_provider.get (clazz);

        log.info ("getManagedObjects clazz={}", clazz);
        log.info ("provider_list = {}", provider_list);

        for (ManagedObjectProvider provider: provider_list)
        {
            if (filter == null || filter.contains ("bugabuga"))
            {
                ManagedObjectInstance ref = new DefaultManagedObjectInstance (null, null, null);
                ref.setProperty (ManagedObjectInstance.PROVIDER, provider);
                ref.setProperty (ManagedObjectInstance.CLASS, clazz);
                found_objects.add (ref);
                log.info ("add ref={}", ref);
            }
        }

        return (found_objects.toArray (new ManagedObjectInstance[0]));
    }

    @Override // ManagedObjectFactory
    public ManagedObjectInstance[] getManagedObjects (Class clazz, String filter)
    {
        return (getManagedObjects (clazz.getName (), filter));
    }

    @Override // ManagedObjectFactory
    public ManagedObjectInstance newInstance (ManagedObjectInstance ref, Map<String, Object> properties)
    {
        ManagedObjectProvider provider = (ManagedObjectProvider)ref.getProperty (ManagedObjectInstance.PROVIDER);
        String ref_class = (String)ref.getProperty (ManagedObjectInstance.CLASS);
        ManagedObjectInstance new_instance = null;

        log.info ("newInstance: provider={} ref_class={} ref={} properties={}", provider, ref_class, ref, properties);

        if (provider != null && ref_class != null)
        {
            if (properties == null)
            {
                properties = new HashMap<> ();
            }

            // Create new ManagedObject itself...
            ManagedObject new_object = provider.newInstance (ref_class, properties);

            // ...create the instance...
            Bundle provider_bundle = FrameworkUtil.getBundle (provider.getClass ());
            new_instance = new DefaultManagedObjectInstance (new_object, provider_bundle, properties);
            new_instance.setProperty (ManagedObjectInstance.PROVIDER, provider);
            new_instance.setProperty (ManagedObjectInstance.CLASS, ref_class);

            // ...and register it within the class instance set
            Set<WeakReference<ManagedObjectInstance>> instance_set = bundle_to_set.get (provider_bundle);

            if (instance_set != null)
            {
                instance_set.add (new WeakReference<> (new_instance));
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

        return ((available_providers.length == 0)? null: newInstance (available_providers [0], properties));
    }

    @Override // ManagedObjectFactory
    public ManagedObjectInstance newInstance (Class clazz, Map<String, Object> properties)
    {
        return (newInstance (clazz.getName (), properties));
    }

    @Validate
    private boolean validate ()
    {
        log.info ("ManagedObjectFactory started.");
        return (true);
    }

    @Invalidate
    private void invalidate ()
    {
        log.info ("ManagedObjectFactory terminated.");
    }

    @Bind (aggregate=true, optional=true, specification = ManagedObjectProvider.class)
    private void bindManagedObjectProvider (ManagedObjectProvider provider)
    {
        synchronized (class_to_provider)
        {
            String[] provided_classes = provider.getProvidedClasses ();

            for (String clazz: provided_classes)
            {
                log.info ("bindManagedObjectProvider: Adding {} for {}", provider, clazz);

                if (!class_to_provider.containsKey (clazz))
                {
                    class_to_provider.put (clazz, new HashSet<ManagedObjectProvider> ());
                }

                Set<ManagedObjectProvider> provider_set = class_to_provider.get (clazz);
                provider_set.add (provider);
            }

            Bundle provider_bundle = FrameworkUtil.getBundle (provider.getClass ());

            if (!bundle_to_set.containsKey (provider_bundle))
            {
                 bundle_to_set.put (provider_bundle, new HashSet<WeakReference<ManagedObjectInstance>> ());
            }
        }
    }

    @Unbind
    private void unbindManagedObjectProvider (ManagedObjectProvider provider)
    {
        String[] removed_classes = provider.getProvidedClasses ();

        synchronized (class_to_provider)
        {
            for (String clazz: removed_classes)
            {
                Set<ManagedObjectProvider> provider_set = class_to_provider.get (clazz);

                if (provider_set != null)
                {
                    provider_set.remove (provider);
                    log.info ("unbindManagedObjectProvider: Removing {} for {}", provider, clazz);
                }
            }
        }

        Bundle provider_bundle = FrameworkUtil.getBundle (provider.getClass ());
        Set<WeakReference<ManagedObjectInstance>> instance_set = bundle_to_set.get (provider_bundle);

        // Invalidate all orphaned instances
        if (instance_set != null)
        {
            log.info ("unbindManagedObjectProvider: Cleaning {} instances from {}", instance_set.size (), provider_bundle);

            for (WeakReference<ManagedObjectInstance> instance_ref: instance_set)
            {
                ManagedObjectInstance instance = instance_ref.get ();

                if (instance != null)
                {
                    instance.invalidate ();
                }
            }

            bundle_to_set.remove (provider_bundle);
        }
    }
}

// EOF
