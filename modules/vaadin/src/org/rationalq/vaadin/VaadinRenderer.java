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

import org.lucidj.api.Renderer;

import com.vaadin.ui.Component;
import com.vaadin.ui.HasComponents;

import java.lang.reflect.Field;
import java.util.Iterator;

import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

@org.apache.felix.ipojo.annotations.Component (immediate = true)
@Instantiate
@Provides
public class VaadinRenderer implements Renderer
{
    // TODO: VAADIN CANNOT BE PIPED
    private Component vaadin_component;

    @Override // Renderer
    public boolean compatibleObject (Object obj_to_check)
    {
        if (obj_to_check instanceof Vaadin)
        {
            return (true);
        }
        else if (obj_to_check instanceof Component)
        {
            return (true);
        }

        return (false);
    }

    // http://stackoverflow.com/questions/12485351/java-reflection-field-value-in-extends-class
    private Field find_underlying (Class<?> clazz, String fieldName)
    {
        Class<?> current = clazz;
        do
        {
            try
            {
                return (current.getDeclaredField(fieldName));
            }
            catch (Exception ignore) {};
        }
        while ((current = current.getSuperclass()) != null);

        return (null);
    }

    private boolean set_field (Object target, String field_name, Object field_value)
    {
        try
        {
            Field field = find_underlying (target.getClass(), field_name);

            if (field != null)
            {
                field.setAccessible(true);
                field.set(target, field_value);
            }
            return (true);
        }
        catch (Exception e)
        {
//            log.info("set_field: Exception {}", e);
        };
        return (false);
    }

    private void fix_vaadin_connectorId (Component root)
    {
        // Fix connectorId allowing it to be rebound
        set_field (root, "connectorId", null);

        if (root instanceof HasComponents)
        {
            Iterator<Component> iterate = ((HasComponents)root).iterator();

            while (iterate.hasNext())
            {
                fix_vaadin_connectorId (iterate.next ());
            }
        }
    }

    @Override // Renderer
    public void objectLinked (Object obj)
    {
        if (obj instanceof Vaadin)
        {
            vaadin_component = (Vaadin)obj;

            // HACK! HACK! HACK! BEWARE THE GREMLINS!!! DON'T POUR WATER!!!
            // We try to keep Vaadin components visible basically to allow things like
            // static images etc to be embedded and retrieved. This is not intended to
            // work with any dynamic components.
            fix_vaadin_connectorId (vaadin_component);
        }
        else if (obj instanceof Component)
        {
            vaadin_component = (Component)obj;
        }
    }

    @Override // Renderer
    public void objectUnlinked ()
    {
        vaadin_component = null;
    }

    @Override // Renderer
    public Component renderingComponent ()
    {
        return (vaadin_component);
    }

    @Override // Renderer
    public void objectUpdated ()
    {
        // Nothing needed, push enabled :)
    }
}

// EOF
