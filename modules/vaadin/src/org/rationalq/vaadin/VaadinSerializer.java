/*
 * Copyright 2016 NEOautus Ltd. (http://neoautus.com)
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

package org.rationalq.vaadin;

import org.lucidj.api.QuarkSerializable;
import org.lucidj.api.Serializer;
import org.lucidj.api.SerializerEngine;
import org.lucidj.api.SerializerInstance;

import com.vaadin.ui.Component;
import com.vaadin.ui.declarative.Design;
import com.vaadin.ui.declarative.DesignContext;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;

@org.apache.felix.ipojo.annotations.Component (immediate = true)
@Instantiate
@Provides
public class VaadinSerializer implements Serializer, QuarkSerializable
{
    private VaadinComponentFactory vcf = new VaadinComponentFactory ();

    @Requires
    private SerializerEngine serializer;

    @Validate
    private void validate ()
    {
        serializer.register (Vaadin.class, this);
    }

    @Override
    public boolean compatibleClass (Class cls)
    {
        return (Component.class.isAssignableFrom (cls));
    }

    @Override
    public Map<String, Object> serializeObject (SerializerInstance engine, Object to_serialize)
    {
        return (serializeObject (to_serialize));
    }

    @Override
    public Object deserializeObject (SerializerInstance engine, Map<String, Object> properties)
    {
        return null;
    }

    class VaadinComponentFactory extends Design.DefaultComponentFactory
    {
        @Override
        protected Class<? extends Component> resolveComponentClass (String qualifiedClassName, DesignContext context)
        {
            // TODO: USE CLASSLOADER FROM QuarkContext
            if (Vaadin.class.getCanonicalName ().equals (qualifiedClassName))
            {
                return (Vaadin.class);
            }

            return (super.resolveComponentClass (qualifiedClassName, context));
        }
    }

    @Override
    public Map<String, Object> serializeObject (Object to_serialize)
    {
        Map<String, Object> properties = new HashMap<> ();

        try
        {
            ByteArrayOutputStream baos = new ByteArrayOutputStream ();

            // TODO: BETTER WAY TO SET COMPONENT FACTORY
            Design.setComponentFactory (vcf);
            Design.write ((Component)to_serialize, baos);
            properties.put ("/", baos.toString ("UTF-8"));
        }
        catch (Exception e)
        {
            // Insert exception into serialization, notify
            properties.put ("/", e.toString ());
        }

        return (properties);

    }

    @Override
    public Object deserializeObject (Map<String, Object> properties)
    {
        if (properties.containsKey ("/") && properties.get ("/") instanceof String)
        {
            String decls = (String)properties.get ("/");
            Design.setComponentFactory (vcf);
            return (Design.read (new ByteArrayInputStream (decls.getBytes ())));
        }

        return (null);
    }
}

// EOF
