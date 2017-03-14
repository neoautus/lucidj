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

package org.rationalq.vaadin;

import org.lucidj.api.ManagedObjectFactory;
import org.lucidj.api.ManagedObjectInstance;
import org.lucidj.api.Renderer;
import org.lucidj.api.RendererFactory;
import org.lucidj.api.RendererProvider;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

@Component (immediate = true, publicFactory = false)
@Instantiate
@Provides
public class VaadinRendererProvider implements RendererProvider
{
    private VaadinRenderer console_filter = new VaadinRenderer ();

    @Requires
    private ManagedObjectFactory objectFactory;

    @Requires
    private RendererFactory rendererFactory;

    @Override
    public Renderer getCompatibleRenderer (Object object)
    {
        if (console_filter.compatibleObject (object))
        {
            VaadinRenderer renderer = new VaadinRenderer ();
            ManagedObjectInstance object_instance = objectFactory.wrapObject (renderer);
            return (object_instance.adapt (VaadinRenderer.class));
        }
        return (null);
    }
}

// EOF
