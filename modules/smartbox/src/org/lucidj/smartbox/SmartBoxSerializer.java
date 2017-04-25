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

package org.lucidj.smartbox;

import org.lucidj.api.BundleRegistry;
import org.lucidj.api.CodeEngine;
import org.lucidj.api.CodeEngineManager;
import org.lucidj.api.ManagedObject;
import org.lucidj.api.ManagedObjectFactory;
import org.lucidj.api.ManagedObjectInstance;
import org.lucidj.api.ManagedObjectProvider;
import org.lucidj.api.Serializer;
import org.lucidj.api.SerializerEngine;
import org.lucidj.api.SerializerInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import org.osgi.framework.BundleContext;
import org.apache.felix.ipojo.annotations.Context;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;

@org.apache.felix.ipojo.annotations.Component (immediate = true)
@Instantiate
@Provides
public class SmartBoxSerializer implements Serializer, ManagedObjectProvider
{
    private final static transient Logger log = LoggerFactory.getLogger (SmartBoxSerializer.class);

    @Context
    private BundleContext context;

    @Requires
    private ManagedObjectFactory objectFactory;

    @Requires
    private BundleRegistry bundleRegistry;

    @Requires
    private SerializerEngine serializer;

    @Requires
    private CodeEngineManager engineManager;

    @Validate
    private void validate ()
    {
        objectFactory.register (SmartBox.class, this, null);
        serializer.register (SmartBox.class, this);
    }

    @Override // Serializer
    public boolean serializeObject (SerializerInstance instance, Object object)
    {
        SmartBox smartbox = (SmartBox)object;
        HashMap<String, Object> properties = smartbox.getProperties ();

        // Throw in all current properties
        for (Map.Entry<String, Object> entry: properties.entrySet ())
        {
            instance.setProperty (entry.getKey (), entry.getValue ());
        }

        // Base object data
        instance.setObjectClass (SmartBox.class);
        instance.setValue ((String)smartbox.getValue ());

        // Runtime properties
        instance.setProperty ("output", smartbox.getObjectManager ().getObjects ());
        return (true);
    }

    @Override // Serializer
    public Object deserializeObject (SerializerInstance instance)
    {
        ManagedObjectInstance smartbox_instance = objectFactory.newInstance (SmartBox.class, null);
        SmartBox smartbox = smartbox_instance.adapt (SmartBox.class);
        HashMap<String, Object> properties = smartbox.getProperties ();

        for (String key: instance.getPropertyKeys ())
        {
            properties.put (key, instance.getProperty (key));
        }

        smartbox.setValue (instance.getValue ());

        log.info ("*** deserializeObject: properties={}", properties);

        Object[] output = instance.getArrayProperty ("output");

        if (output != null)
        {
            for (Object object: output)
            {
                log.info ("*** output = {}", object);
                if (object != null)
                {
                    smartbox.getObjectManager ().showObject (object);
                }
            }
        }
        return (smartbox);
    }

    @Override
    public ManagedObject newObject (String clazz, ManagedObjectInstance instance)
    {
        CodeEngine engine = engineManager.getEngineByName ("beanshell");
        return (new SmartBox (engine, bundleRegistry, objectFactory));
    }
}

// EOF
