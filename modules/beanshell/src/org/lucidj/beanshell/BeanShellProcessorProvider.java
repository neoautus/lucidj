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

package org.lucidj.beanshell;

import org.lucidj.api.Aggregate;
import org.lucidj.api.BundleRegistry;
import org.lucidj.api.CodeEngine;
import org.lucidj.api.CodeEngineManager;
import org.lucidj.api.ComponentDescriptor;
import org.lucidj.api.ComponentInterface;
import org.lucidj.api.ComponentManager;
import org.lucidj.api.ManagedObject;
import org.lucidj.api.ManagedObjectFactory;
import org.lucidj.api.ManagedObjectInstance;
import org.lucidj.api.ManagedObjectProvider;
import org.lucidj.api.Serializer;
import org.lucidj.api.SerializerEngine;
import org.lucidj.api.SerializerInstance;

import java.util.HashMap;
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
public class BeanShellProcessorProvider implements Serializer, ManagedObjectProvider
{
    private ComponentDescriptor descriptor;

    @Context
    private BundleContext context;

    @Requires
    private ManagedObjectFactory objectFactory;

    @Requires
    private ComponentManager componentManager;

    @Requires
    private BundleRegistry bundleRegistry;

    @Requires
    private CodeEngineManager engineManager;

    @Requires
    private SerializerEngine serializerEngine;

    private void register_component_descriptor ()
    {
        // Originally this had references to Vaadin packages. This is not the case anymore.
        // However, it's a good idea to split the component itself from it's UI side. At some
        // time it'll be interesting to place this descriptor into some kind of aggregation.
        // I found the previous approach (glue-factory) a bit cumbersome.
        descriptor = componentManager.newComponentDescriptor ();
        descriptor.setIconUrl ("/VAADIN/~/" + context.getBundle ().getSymbolicName () + "/component-icon.png");
        descriptor.setIconTitle ("BeanShell");
        descriptor.setComponentClass ("org.lucidj.beanshell.BeanShellProcessor");
        componentManager.register (context, descriptor);
    }

    @Override
    public ManagedObject newObject (String clazz, ManagedObjectInstance instance)
    {
        // TODO: CodeEngineManager CAN HAVE AN ObjectFactory ASPECT
        CodeEngine code_engine = engineManager.getEngineByName ("beanshell");
        ComponentInterface code_container;

        if (instance.containsKey (ComponentInterface.class.getName ()))
        {
            // Use the provided code container
            code_container = (ComponentInterface)instance.getProperty (ComponentInterface.class.getName ());
        }
        else
        {
            // Provide a brand new code container
            ManagedObjectInstance new_instance = objectFactory.newInstance ("org.lucidj.smartbox.SmartBox", null);
            code_container = new_instance.adapt (ComponentInterface.class);
        }

        // Add the UI descriptor
        code_container.setProperty (ComponentDescriptor.DESCRIPTOR, descriptor);
        code_container.setProperty (CodeEngine.class.getName (), code_engine);

        // Build the processor
        return (new BeanShellProcessor (code_container, code_engine));
    }

    @Override
    public boolean serializeObject (SerializerInstance instance, Object object)
    {
        return (instance.serializeAs (((Aggregate)object).identity (), BeanShellProcessor.class.getName ()));
    }

    @Override
    public Object deserializeObject (SerializerInstance instance)
    {
        // Use another deserializer to build our object
        ComponentInterface code_container = (ComponentInterface)instance.deserializeAs ("org.lucidj.smartbox.SmartBox");

        // Create the new processor providing the code_container
        Map<String, Object> props = new HashMap<> ();
        props.put (ComponentInterface.class.getName (), code_container);
        ManagedObjectInstance new_instance = objectFactory.newInstance (BeanShellProcessor.class, props);
        return (new_instance.adapt (BeanShellProcessor.class));
    }

    @Validate
    private void validate ()
    {
        objectFactory.register (BeanShellProcessor.class, this, null);
        serializerEngine.register (BeanShellProcessor.class, this);
        register_component_descriptor ();
    }
}

// EOF
