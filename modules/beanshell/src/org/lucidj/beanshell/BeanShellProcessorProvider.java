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

package org.lucidj.beanshell;

import org.lucidj.api.stddef.Aggregate;
import org.lucidj.api.core.CodeEngine;
import org.lucidj.api.core.CodeEngineManager;
import org.lucidj.api.core.ComponentDescriptor;
import org.lucidj.api.core.ComponentInterface;
import org.lucidj.api.core.ComponentManager;
import org.lucidj.api.core.Serializer;
import org.lucidj.api.core.SerializerEngine;
import org.lucidj.api.core.SerializerInstance;
import org.lucidj.api.core.ServiceContext;
import org.lucidj.api.core.ServiceObject;

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
@Provides (specifications = Serializer.class)
public class BeanShellProcessorProvider implements Serializer, ServiceObject.Provider
{
    private ComponentDescriptor descriptor;

    @Context
    private BundleContext context;

    @Requires
    private ServiceContext serviceContext;

    @Requires
    private ComponentManager componentManager;

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

    @Override // ServiceObject.Provider
    public Object newObject (String objectClassName, Map<String, Object> properties)
    {
        // TODO: CodeEngineManager CAN HAVE AN ObjectFactory ASPECT
        CodeEngine code_engine = engineManager.getEngineByName ("beanshell");
        ComponentInterface code_container;

        if (properties.containsKey (ComponentInterface.class.getName ()))
        {
            // Use the provided code container
            code_container = (ComponentInterface)properties.get (ComponentInterface.class.getName ());
        }
        else
        {
            // Provide a brand new code container
            code_container = (ComponentInterface)serviceContext.newServiceObject ("org.lucidj.smartbox.SmartBox");
        }

        // Add the UI descriptor
        code_container.setProperty (ComponentDescriptor.DESCRIPTOR, descriptor);
        code_container.setProperty (CodeEngine.CODE_ENGINE, code_engine);

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
        return (serviceContext.newServiceObject (BeanShellProcessor.class, props));
    }

    @Validate
    private void validate ()
    {
        serviceContext.register (BeanShellProcessor.class, this);
        serializerEngine.register (BeanShellProcessor.class, this);
        register_component_descriptor ();
    }
}

// EOF
