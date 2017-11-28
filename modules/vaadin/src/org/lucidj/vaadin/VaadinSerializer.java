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

package org.lucidj.vaadin;

import org.lucidj.api.core.Serializer;
import org.lucidj.api.core.SerializerEngine;
import org.lucidj.api.core.SerializerInstance;
import org.lucidj.api.core.ServiceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.ui.Component;
import com.vaadin.ui.declarative.Design;
import com.vaadin.ui.declarative.DesignContext;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;

@org.apache.felix.ipojo.annotations.Component (immediate = true, publicFactory = false)
@Instantiate
@Provides
public class VaadinSerializer implements Serializer
{
    private final static Logger log = LoggerFactory.getLogger (VaadinSerializer.class);

    private VaadinComponentFactory vcf = new VaadinComponentFactory ();

    @Requires
    private ServiceContext serviceContext;

    @Requires
    private SerializerEngine serializer;

    @Validate
    private void validate ()
    {
        serviceContext.register (Vaadin.class);
        serializer.register (Vaadin.class, this);
        serializer.register (Component.class, this);
    }

    @Override // Serializer
    public boolean serializeObject (SerializerInstance instance, Object object)
    {
        try
        {
            ByteArrayOutputStream baos = new ByteArrayOutputStream ();

            // TODO: BETTER WAY TO SET COMPONENT FACTORY
            Design.setComponentFactory (vcf);
            Design.write ((Component)object, baos);
            instance.setValue (baos.toString ("UTF-8"));
            instance.setObjectClass (Vaadin.class);
            return (true);
        }
        catch (Exception e)
        {
            log.error ("Exception serializing object", e);
            return (false);
        }
    }

    @Override // Serializer
    public Object deserializeObject (SerializerInstance instance)
    {
        Design.setComponentFactory (vcf);
        // TODO: WRAP MANAGED OBJECT
        return (Design.read (new ByteArrayInputStream (instance.getValue ().getBytes ())));
    }

    class VaadinComponentFactory extends Design.DefaultComponentFactory
    {
        @Override
        protected Class<? extends Component> resolveComponentClass (String qualifiedClassName, DesignContext context)
        {
            if (Vaadin.class.getCanonicalName ().equals (qualifiedClassName))
            {
                return (Vaadin.class);
            }

            return (super.resolveComponentClass (qualifiedClassName, context));
        }
    }
}

// EOF
