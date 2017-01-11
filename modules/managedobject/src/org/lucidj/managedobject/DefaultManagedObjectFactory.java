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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

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

    private final Map<String, ManagedObjectProvider> provider_list = new HashMap<> ();
    private final Map<String, Set<WeakReference<ManagedObjectInstance>>> class_to_set = new HashMap<> ();

    @Override
    public ManagedObjectInstance newInstance (String clazz, Map<String, Object> properties)
    {
        ManagedObjectProvider provider = provider_list.get (clazz);
        ManagedObjectInstance new_instance = null;

        if (provider != null)
        {
            if (properties == null)
            {
                properties = new HashMap<> ();
            }

            // Create new ManagedObject instance...
            ManagedObject new_object = provider.newInstance (clazz, properties);
            new_instance = new DefaultManagedObjectInstance (new_object);

            // ...and register it within the class instance set
            Set<WeakReference<ManagedObjectInstance>> instance_set = class_to_set.get (clazz);

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
        synchronized (provider_list)
        {
            String[] provided_classes = provider.getProvidedClasses ();

            for (String clazz: provided_classes)
            {
                log.info ("bindManagedObjectProvider: Adding {} for {}", provider, clazz);

                // We never override an existing provider
                if (!provider_list.containsKey (clazz))
                {
                    provider_list.put (clazz, provider);
                    class_to_set.put (clazz, new HashSet<WeakReference<ManagedObjectInstance>> ());
                }
            }
        }
    }

    @Unbind
    private void unbindManagedObjectProvider (ManagedObjectProvider provider)
    {
        Set<String> removed_classes = new HashSet<> ();

        synchronized (provider_list)
        {
            Iterator<Map.Entry<String, ManagedObjectProvider>> it = provider_list.entrySet().iterator();

            while (it.hasNext())
            {
                Map.Entry<String, ManagedObjectProvider> entry = it.next();

                if (entry.getValue ().equals (provider))
                {
                    log.info ("unbindManagedObjectProvider: Removing {} for {}", provider, entry.getKey ());
                    it.remove ();
                    removed_classes.add (entry.getKey ());
                }
            }
        }

        // Invalidate all orphaned instances
        for (String clazz: removed_classes)
        {
            Set<WeakReference<ManagedObjectInstance>> instance_set = class_to_set.get (clazz);

            if (instance_set != null)
            {
                log.info ("Cleaning {} instances of {}", instance_set.size (), clazz);

                for (WeakReference<ManagedObjectInstance> instance_ref: instance_set)
                {
                    ManagedObjectInstance instance = instance_ref.get ();

                    if (instance != null)
                    {
                        instance.invalidate ();
                    }
                }

                class_to_set.remove (clazz);
            }
        }
    }
}

// EOF
