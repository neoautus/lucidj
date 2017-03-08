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

package org.lucidj.console;

import org.lucidj.api.ManagedObjectFactory;
import org.lucidj.api.ManagedObjectInstance;
import org.lucidj.api.Serializer;
import org.lucidj.api.SerializerEngine;
import org.lucidj.api.SerializerInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;

@org.apache.felix.ipojo.annotations.Component (immediate = true)
@Instantiate
@Provides
public class ConsoleSerializer implements Serializer
{
    private final static transient Logger log = LoggerFactory.getLogger (ConsoleSerializer.class);

    @Requires
    private ManagedObjectFactory objectFactory;

    @Requires
    private SerializerEngine serializer;

    @Validate
    private void validate ()
    {
        serializer.register (Console.class, this);
    }

    @Override // Serializer
    public boolean serializeObject (SerializerInstance instance, Object object)
    {
        // Complete content log including timestamps and tags
        instance.setValue (((Console)object).getValue ());
        instance.setObjectClass (Console.class);
        return (true);
    }

    @Override // Serializer
    public Object deserializeObject (SerializerInstance instance)
    {
        ManagedObjectInstance console_instance = objectFactory.wrapObject (new Console ());
        Console console = console_instance.adapt (Console.class);
        console.setValue (instance.getValue ());
        return (console);
    }
}

// EOF
