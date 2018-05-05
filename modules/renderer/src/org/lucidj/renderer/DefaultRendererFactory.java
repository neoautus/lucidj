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

package org.lucidj.renderer;

import org.lucidj.api.stddef.Aggregate;
import org.lucidj.api.core.ServiceContext;
import org.lucidj.api.vui.ObjectRenderer;
import org.lucidj.api.vui.Renderer;
import org.lucidj.api.vui.RendererFactory;
import org.lucidj.api.vui.RendererProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.ui.UI;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.osgi.framework.BundleContext;
import org.apache.felix.ipojo.annotations.Bind;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Context;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Unbind;
import org.apache.felix.ipojo.annotations.Validate;

@Component (immediate = true, publicFactory = false)
@Instantiate
@Provides
public class DefaultRendererFactory implements RendererFactory
{
    private final static Logger log = LoggerFactory.getLogger (DefaultRendererFactory.class);

    private List<RendererProvider> renderer_providers = new ArrayList<> ();
    private Map<DefaultObjectRenderer, RendererProvider> renderer_mapping = new HashMap<> ();

    @Context
    private BundleContext ctx;

    @Requires
    private ServiceContext serviceContext;

    @Override // RendererFactory
    public ObjectRenderer newRenderer ()
    {
        return (newRenderer (null));
    }

    @Override // RendererFactory
    public ObjectRenderer newRenderer (Object object)
    {
        ObjectRenderer new_renderer = serviceContext.wrapObject (ObjectRenderer.class, new DefaultObjectRenderer (this));

        if (object != null)
        {
            new_renderer.link (object);
        }
        return (new_renderer);
    }

    public Renderer locateAndBindRenderer (DefaultObjectRenderer mapper, Object object)
    {
        log.info ("getCompatibleRenderer ({})", object);

        if (object instanceof UI)
        {
            // Never render Vaadin UI. Replace with something interesting.
            return (null);
        }

        for (RendererProvider provider: renderer_providers)
        {
            Renderer renderer = null;
            Object source = object;

            // First check if the renderer support aggregated objects
            if ((renderer = provider.getCompatibleRenderer (object)) == null)
            {
                // Or look for a suitable renderer for each aggregate object
                for (Object element: Aggregate.elements (object))
                {
                    if ((renderer = provider.getCompatibleRenderer (element)) != null)
                    {
                        source = element;
                        break;
                    }
                }
            }

            if (renderer != null)
            {
                // Link the element to the renderer
                renderer_mapping.put (mapper, provider);
                renderer.objectLinked (source);
                return (renderer);
            }
        }
        return (null);
    }

    @Bind (aggregate=true, optional=true, specification = RendererProvider.class)
    private void bindRenderer (RendererProvider provider)
    {
        log.info ("===> Adding renderer provider: {}", provider);
        renderer_providers.add (provider);

        DefaultObjectRenderer[] active_renderers =
            renderer_mapping.keySet ().toArray (new DefaultObjectRenderer [0]);

        for (DefaultObjectRenderer renderer: active_renderers)
        {
            if (renderer_mapping.get (renderer) == null)
            {
                log.info ("===> refreshing {}", renderer);
                renderer.refreshRenderer ();
            }
        }
    }

    @Unbind
    private void unbindRenderer (RendererProvider provider)
    {
        log.info ("===> Removing renderer provider: {}", provider);
        renderer_providers.remove (provider);

        DefaultObjectRenderer[] active_renderers =
                renderer_mapping.keySet ().toArray (new DefaultObjectRenderer [0]);

        for (DefaultObjectRenderer renderer: active_renderers)
        {
            if (renderer_mapping.get (renderer) == provider)
            {
                log.info ("===> refreshing {} / {}", renderer, renderer_mapping.get (renderer));
                renderer_mapping.replace (renderer, null);
                renderer.refreshRenderer ();
            }
        }
    }

    @Validate
    private void validate ()
    {
        log.info ("ObjectRenderer started");
        serviceContext.register (DefaultObjectRenderer.class);
    }

    @Invalidate
    private void invalidate ()
    {
        log.info ("ObjectRenderer stopped");
    }
}

// EOF
