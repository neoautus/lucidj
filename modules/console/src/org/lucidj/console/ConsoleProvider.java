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

import org.lucidj.api.EventHelper;
import org.lucidj.api.Serializer;
import org.lucidj.api.SerializerEngine;
import org.lucidj.api.SerializerInstance;
import org.lucidj.api.ServiceContext;
import org.lucidj.api.ServiceObject;
import org.lucidj.api.Stdio;

import java.util.Map;

import org.osgi.framework.BundleContext;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Context;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;

@Component (immediate = true, publicFactory = false)
@Instantiate
@Provides
public class ConsoleProvider implements Serializer, ServiceObject.Provider
{
    @Context
    private BundleContext bundleContext;

    @Requires
    private ServiceContext serviceContext;

    @Requires
    private SerializerEngine serializer;

    @Requires
    private EventHelper.Factory eventHelperFactory;

    @Validate
    private void validate ()
    {
        serviceContext.register (Console.class, this);
        serviceContext.register (Stdio.class, this);
        serializer.register (Console.class, this);
    }

    @Override // Serializer
    public boolean serializeObject (SerializerInstance instance, Object object)
    {
        // Complete content log including timestamps and tags
        instance.setValue (((Console)object).getRawBuffer ());
        instance.setObjectClass (Console.class);
        return (true);
    }

    @Override // Serializer
    public Object deserializeObject (SerializerInstance instance)
    {
        Console console = serviceContext.newServiceObject (Console.class);
        console.setRawBuffer (instance.getValue ());
        return (console);
    }

    @Override
    public Object newObject (String objectClassName, Map<String, Object> properties)
    {
        EventHelper new_event_helper = eventHelperFactory.newInstance ();
        return (serviceContext.wrapObject (Console.class, new Console (new_event_helper)));
    }
}

// EOF
