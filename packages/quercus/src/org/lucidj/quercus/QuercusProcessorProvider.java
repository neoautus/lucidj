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

package org.lucidj.quercus;

import org.lucidj.api.Aggregate;
import org.lucidj.api.CodeEngine;
import org.lucidj.api.CodeEngineManager;
import org.lucidj.api.ComponentDescriptor;
import org.lucidj.api.ComponentInterface;
import org.lucidj.api.ComponentManager;
import org.lucidj.api.Serializer;
import org.lucidj.api.SerializerEngine;
import org.lucidj.api.SerializerInstance;
import org.lucidj.api.ServiceContext;
import org.lucidj.api.ServiceObject;

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
public class QuercusProcessorProvider implements ServiceObject.Provider, Serializer
{
    private ComponentDescriptor descriptor;

    @Context
    private BundleContext context;

    @Requires
    private ServiceContext serviceContext;

    @Requires
    private CodeEngineManager engineManager;

    @Requires
    private ComponentManager componentManager;

    @Requires
    private SerializerEngine serializerEngine;

    private void register_component_descriptor ()
    {
        descriptor = componentManager.newComponentDescriptor ();
        descriptor.setIconUrl ("/VAADIN/~/" + context.getBundle ().getSymbolicName () + "/icons/quercus-icon-64x64.png");
        descriptor.setIconTitle ("Quercus");
        descriptor.setComponentClass ("org.lucidj.quercus.QuercusProcessor");
        componentManager.register (context, descriptor);
    }

    @Override // ServiceObject.Provider
    public Object newObject (String objectClassName, Map<String, Object> properties)
    {
        // TODO: CodeEngineManager CAN HAVE AN ObjectFactory ASPECT
        CodeEngine code_engine = engineManager.getEngineByName ("quercus");
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
        return (new QuercusProcessor (code_container, code_engine));
    }

    @Override // Serializer
    public boolean serializeObject (SerializerInstance instance, Object object)
    {
        return (instance.serializeAs (Aggregate.identity (object), QuercusProcessor.class.getName ()));
    }

    @Override // Serializer
    public Object deserializeObject (SerializerInstance instance)
    {
        // Use another deserializer to build our object
        ComponentInterface code_container = (ComponentInterface)instance.deserializeAs ("org.lucidj.smartbox.SmartBox");

        // Create the new processor providing the code_container
        Map<String, Object> props = new HashMap<> ();
        props.put (ComponentInterface.class.getName (), code_container);
        return (serviceContext.newServiceObject (QuercusProcessor.class, props));
    }

    @Validate
    private void validate ()
    {
        serviceContext.register (QuercusProcessor.class, this);
        serializerEngine.register (QuercusProcessor.class, this);
        register_component_descriptor ();
    }
}

// EOF
