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

package org.lucidj.iconlist.renderer;

import org.lucidj.api.Renderer;
import org.lucidj.api.RendererProvider;
import org.lucidj.api.ServiceContext;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;

@Component (immediate = true, publicFactory = false)
@Instantiate
@Provides
public class IconListRendererProvider implements RendererProvider
{
    @Requires
    private ServiceContext serviceContext;

    @Override
    public Renderer getCompatibleRenderer (Object object)
    {
        if (IconListRenderer.isCompatible (object))
        {
            return (serviceContext.newServiceObject (IconListRenderer.class));
        }
        return (null);
    }

    @Validate
    private void validate ()
    {
        serviceContext.register (IconListRenderer.class);
    }
}

// EOF
