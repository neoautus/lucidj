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

import org.lucidj.api.core.ManagedObject;
import org.lucidj.api.core.ManagedObjectInstance;
import org.lucidj.api.vui.Renderer;

import com.vaadin.ui.Component;

public class VaadinRenderer implements Renderer, ManagedObject
{
    // TODO: VAADIN CANNOT BE PIPED
    private Component vaadin_component;

    public static boolean isCompatible (Object object)
    {
        if (object instanceof Vaadin)
        {
            return (true);
        }
        else if (object instanceof Component)
        {
            return (true);
        }
        return (false);
    }

    @Override // Renderer
    public void objectLinked (Object obj)
    {
        if (obj instanceof Component)
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

    @Override // ManagedObject
    public void validate (ManagedObjectInstance instance)
    {
        // Nop
    }

    @Override // ManagedObject
    public void invalidate (ManagedObjectInstance instance)
    {
        // Nop
    }
}

// EOF
