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

import org.rationalq.quark.Quark;

import com.vaadin.ui.Component;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.declarative.Design;
import com.vaadin.ui.declarative.DesignContext;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

public class Vaadin extends VerticalLayout implements Quark
{
    private Map<String, Object> properties = new HashMap<> ();
    private VaadinComponentFactory vcf = new VaadinComponentFactory ();

    public Vaadin ()
    {
        setWidth (100, Unit.PERCENTAGE);
        setHeightUndefined ();
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
    public Map<String, Object> serializeObject ()
    {
        try
        {
            ByteArrayOutputStream baos = new ByteArrayOutputStream ();

            // TODO: BETTER WAY TO SET COMPONENT FACTORY
            Design.setComponentFactory (vcf);
            Design.write ((VerticalLayout)this, baos);
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
    public void deserializeObject (Map<String, Object> properties)
    {
        this.properties.putAll (properties);

        if (properties.containsKey ("/") && properties.get ("/") instanceof String)
        {
            String decls = (String)properties.get ("/");
            Design.setComponentFactory (vcf);
            Design.read (new ByteArrayInputStream (decls.getBytes ()), this);
        }
    }
}

// EOF
