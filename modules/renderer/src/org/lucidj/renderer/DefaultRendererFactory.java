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

package org.lucidj.renderer;

import org.lucidj.api.Aggregate;
import org.lucidj.api.ManagedObjectFactory;
import org.lucidj.api.ManagedObjectInstance;
import org.lucidj.api.ObjectRenderer;
import org.lucidj.api.Renderer;
import org.lucidj.api.RendererFactory;
import org.lucidj.api.RendererProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.ui.Component;
import com.vaadin.ui.Label;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.apache.felix.ipojo.annotations.Bind;
import org.apache.felix.ipojo.annotations.Context;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Unbind;
import org.apache.felix.ipojo.annotations.Validate;

@org.apache.felix.ipojo.annotations.Component (immediate = true, publicFactory = false)
@Instantiate
@Provides
public class DefaultRendererFactory implements RendererFactory
{
    private final transient static Logger log = LoggerFactory.getLogger (DefaultObjectRenderer.class);

    private Object source;
    private ServiceReference<Renderer> current_service;
    private Renderer current_renderer;
    private Component current_component;
    private Label default_component;

    private HashMap<String, RendererProvider> renderer_providers = new HashMap<> ();
    private HashMap<ManagedObjectInstance, Renderer> renderer_instances = new HashMap<> ();

    @Context
    private BundleContext ctx;

    @Requires
    private ManagedObjectFactory object_factory;

//    @Override // ServiceTrackerCustomizer
//    public Renderer addingService (ServiceReference<Renderer> serviceReference)
//    {
//        Renderer renderer = ctx.getService (serviceReference);
//        renderer_map.put (serviceReference, renderer);
//
//        log.info ("addingService: {}: {}", serviceReference, renderer);
//
//        if (source != null && renderer.compatibleObject (source))
//        {
//            safe_apply_renderer (serviceReference, source);
//        }
//
//        // We need to return the object in order to track it
//        return (renderer);
//    }
//
//    @Override // ServiceTrackerCustomizer
//    public void modifiedService (ServiceReference<Renderer> serviceReference, Renderer renderer)
//    {
//        log.info ("modifiedService: {}: {}", serviceReference, renderer);
//        renderer_map.put (serviceReference, renderer);
//        // TODO: REPLACE OLD RENDERER
//    }
//
//    @Override // ServiceTrackerCustomizer
//    public void removedService (ServiceReference<Renderer> serviceReference, Renderer renderer)
//    {
//        // TODO: CLEAR OBSERVERS
//        ctx.ungetService (serviceReference);
//        renderer_map.remove (serviceReference);
//
//        if (source != null && renderer.compatibleObject (source))
//        {
//            safe_apply_renderer (null, source);
//        }
//
//        log.info ("removedService: {}: {}", serviceReference, renderer);
//    }

    @Override // RendererFactory
    public ObjectRenderer newRenderer ()
    {
        ManagedObjectInstance view_instance = object_factory.wrapObject (new DefaultObjectRenderer (this));
        return (view_instance.adapt (ObjectRenderer.class));
    }

    @Override // RendererFactory
    public Renderer getCompatibleRenderer (Object object)
    {
        log.info ("getCompatibleRenderer ({})", object);

        for (Map.Entry<String, RendererProvider> provider: renderer_providers.entrySet ())
        {
            for (Object aspect: Aggregate.get (object))
            {
                Renderer renderer = provider.getValue ().getCompatibleRenderer (aspect);

                if (renderer != null)
                {
                    // Link the aspect to the renderer
                    renderer.objectLinked (aspect);
                    return (renderer);
                }
            }
        }
        return (null);
    }

    @Bind (aggregate=true, optional=true, specification = RendererProvider.class)
    private void bindRenderer (RendererProvider provider)
    {
        log.info ("Adding renderer: {}", provider);
        renderer_providers.put (provider.toString (), provider);
    }

    private void clear_renderer_provider_by_bundle (Bundle bnd)
    {
        Iterator<Map.Entry<String, RendererProvider>> it = renderer_providers.entrySet ().iterator ();

        while (it.hasNext ())
        {
            Map.Entry<String, RendererProvider> entry = it.next ();

            if (FrameworkUtil.getBundle (entry.getValue ().getClass ()) == bnd)
            {
                // TODO: UPDATE ALL RENDERERS

                log.info ("Removing renderer provider: {} for {}", entry.getValue (), entry.getKey ());
                it.remove ();
            }
        }
    }

    @Unbind
    private void unbindRenderer (RendererProvider provider)
    {
        clear_renderer_provider_by_bundle (FrameworkUtil.getBundle (provider.getClass ()));
        log.info ("Removed renderer provider: {}", provider);
    }

    @Validate
    private void validate ()
    {
        log.info ("ObjectRenderer started");
    }

    @Invalidate
    private void invalidate ()
    {
        log.info ("ObjectRenderer stopped");
    }
}

// EOF
