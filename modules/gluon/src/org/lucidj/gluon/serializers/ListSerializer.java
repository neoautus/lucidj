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

package org.lucidj.gluon.serializers;

import org.lucidj.api.ManagedObjectFactory;
import org.lucidj.api.ManagedObjectInstance;
import org.lucidj.api.Serializer;
import org.lucidj.api.SerializerEngine;
import org.lucidj.api.SerializerInstance;
import org.lucidj.runtime.CompositeTask;

import java.util.List;

import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;

@org.apache.felix.ipojo.annotations.Component (immediate = true)
@Instantiate
@Provides
public class ListSerializer implements Serializer
{
    @Requires
    private SerializerEngine serializer;

    @Requires
    private ManagedObjectFactory objectFactory;

    @Override
    public boolean serializeObject (SerializerInstance instance, Object object)
    {
        instance.setObjectClass (object.getClass ());
        for (Object item: (List)object)
        {
            instance.addObject (item);
        }
        return (true);
    }

    @Override
    public Object deserializeObject (SerializerInstance instance)
    {
        ManagedObjectInstance object_instance = objectFactory.wrapObject (new CompositeTask ());
        CompositeTask composite_task = object_instance.adapt (CompositeTask.class);

        for (Object object: instance.getObjects ())
        {
            composite_task.add (object);
        }
        return (composite_task);
    }
    @Validate
    private void validate ()
    {
        serializer.register (CompositeTask.class, this);
    }
}

// EOF
