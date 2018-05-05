/*
 * Copyright 2018 NEOautus Ltd. (http://neoautus.com)
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

package org.lucidj.runtime;

import org.lucidj.api.stddef.Aggregate;
import org.lucidj.api.core.ManagedObject;
import org.lucidj.api.core.ManagedObjectFactory;
import org.lucidj.api.core.ManagedObjectInstance;
import org.lucidj.api.core.ManagedObjectProvider;
import org.lucidj.api.core.Serializer;
import org.lucidj.api.core.SerializerEngine;
import org.lucidj.api.core.SerializerInstance;

import java.util.List;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;

@Component (immediate = true, publicFactory = false)
@Instantiate
@Provides
public class CompositeTaskSerializer implements Serializer, ManagedObjectProvider
{
    @Requires
    private SerializerEngine serializer;

    @Requires
    private ManagedObjectFactory objectFactory;

    @Override
    public boolean serializeObject (SerializerInstance instance, Object object)
    {
        instance.setObjectClass (object.getClass ());
        for (Object item: Aggregate.adapt (List.class, object))
        {
            instance.addObject (item);
        }
        return (true);
    }

    @Override
    public Object deserializeObject (SerializerInstance instance)
    {
        ManagedObjectInstance object_instance = objectFactory.newInstance (CompositeTask.class, null);
        CompositeTask composite_task = object_instance.adapt (CompositeTask.class);

        List obj_list = Aggregate.adapt (List.class, composite_task);
        for (Object object: instance.getObjects ())
        {
            obj_list.add (object);
        }
        return (composite_task);
    }

    @Validate
    private void validate ()
    {
        objectFactory.register (CompositeTask.class, this, null);
        serializer.register (CompositeTask.class, this);
    }

    @Override
    public ManagedObject newObject (String clazz, ManagedObjectInstance instance)
    {
        return (new CompositeTask ());
    }
}

// EOF
