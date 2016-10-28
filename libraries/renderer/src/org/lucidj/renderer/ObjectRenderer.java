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

package org.lucidj.renderer;

import org.lucidj.task.TaskContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.server.VaadinSession;
import com.vaadin.ui.Component;
import com.vaadin.ui.ComponentContainer;
import com.vaadin.ui.Label;

import java.util.HashMap;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public class ObjectRenderer implements ServiceTrackerCustomizer<Renderer, Renderer>, Observer
{
    private final transient static Logger log = LoggerFactory.getLogger (ObjectRenderer.class);

    private Object source;
    private ServiceReference<Renderer> current_service;
    private Renderer current_renderer;
    private Component current_component;
    private Label default_component;

    private BundleContext ctx;
    private ServiceTracker<Renderer, Renderer> tracker;
    private Map<ServiceReference<Renderer>, Renderer> renderer_map;

    public ObjectRenderer (BundleContext ctx)
    {
        renderer_map = new HashMap<> ();
        this.ctx = ctx;

        default_component = new Label ("void");
        default_component.addStyleName ("custom-empty-component");
        default_component.setSizeUndefined ();

        tracker = new ServiceTracker<> (ctx, Renderer.class, this);
        tracker.open ();
    }

    public ObjectRenderer ()
    {
        // Only valid inside TaskContexts
        this (TaskContext.currentTaskContext ().getBundleContext ());
    }

    public boolean isRendered ()
    {
        return (current_component != null && current_renderer != null);
    }

    public Component renderingComponent ()
    {
        return (current_component);
    }

    // TODO: ADD LINK/UNLINK NOTIFICATIONS

    public <A> A adapt (Class<A> type)
    {
        if (isRendered () &&
            current_renderer.getClass ().isAssignableFrom (type))
        {
            return ((A)current_renderer);
        }

        return (null);
    }

    private ServiceReference<Renderer> find_renderer (Object obj)
    {
        log.info ("find_renderer: obj={}", obj);

        // TODO: APPLY PROPER FILTERING TO SORT MULTIPLE COMPATIBLE RENDERERS
        for (ServiceReference<Renderer> sref : renderer_map.keySet ())
        {
            Renderer r = renderer_map.get (sref);

            log.debug ("Searching renderer: {} {}", sref, r);

            // TODO: IS IT VALID TO USE CACHED renderer LIKE THIS?
            if (r.compatibleObject (obj))
            {
                log.info ("Found: obj={} => {} / {}", obj, sref, r);
                return (sref);
            }
        }

        log.info ("Not Found: obj={}", obj);

        return (null);
    }

    private Renderer new_renderer_instance (ServiceReference<Renderer> renderer_service)
    {
        // TODO: IS THIS VALID? WE ARE SKIPPING ctx.getService() STUFF....
        Class renderer_class = renderer_map.get (renderer_service).getClass ();

        Renderer new_renderer = null;

        try
        {
            // For now, all Renderers MUST have a constructor without args
            new_renderer = (Renderer)renderer_class.newInstance ();
        }
        catch (Exception e)
        {
            log.error ("Error creating renderer", e);
        }

        return (new_renderer);
    }

    private void apply_renderer (ServiceReference<Renderer> renderer_service, Object obj)
    {
        Component new_component = null;

        log.info ("apply_renderer: renderer_service={} obj={}", renderer_service, obj);

        if (renderer_service != null)
        {
            current_service = renderer_service;

            if ((current_renderer = new_renderer_instance (renderer_service)) != null)
            {
                // Link and trigger initial update
                current_renderer.objectLinked (obj);
                current_renderer.objectUpdated ();
                new_component = current_renderer.renderingComponent ();
            }
        }

        // Fallback to default renderer if we fail to get proper renderer or its instance
        if (renderer_service == null || current_renderer == null)
        {
            current_service = null;
            current_renderer = null;
            default_component.setValue (obj == null? "null": obj.getClass ().getSimpleName ());
            new_component = default_component;
        }

        if (current_component != null)
        {
            // Copy current component dimensions
            new_component.setWidth (current_component.getWidth (), current_component.getWidthUnits ());
            new_component.setHeight (current_component.getHeight (), current_component.getHeightUnits ());

            // Replace the rendered component with default_component
            if (current_component.getParent () instanceof ComponentContainer)
            {
                ComponentContainer container = (ComponentContainer)current_component.getParent ();
                container.replaceComponent (current_component, new_component);
                current_component = new_component;
            }
        }

        current_component = new_component;

        log.info ("apply_renderer: current_renderer={} current_component={}", current_renderer, current_component);
    }

    private void safe_apply_renderer (ServiceReference<Renderer> serviceReference, Object obj)
    {
        VaadinSession current_session = VaadinSession.getCurrent ();

        if (current_session != null)
        {
            current_session.lock ();
        }

        try
        {
            // TODO: HANDLE RACING CONDITION WHEN A SESSION STARTS AT THIS POINT
            apply_renderer (serviceReference, source);
        }
        finally
        {
            if (current_session != null)
            {
                current_session.unlock ();
            }
        }
    }

    public Component link (Object source)
    {
        this.source = source;

        // TODO: WHEN OBJECT IS UNKNOWN, PUBLISH "BINARY" RENDERER AND WAIT FOR RENDERER ACTIVATION
        ServiceReference<Renderer> renderer_service = find_renderer (source);
        apply_renderer (renderer_service, source);       // TODO: safe_apply_renderer??

        if (source instanceof Renderer.Observable)
        {
            // The object can notify us for changes
            ((Renderer.Observable)source).addObserver (this);
        }

        // TODO: ADD CALLBACK custom_renderer.objectLinked ()

        log.info ("link: returning {}", current_component);
        return (current_component);
    }

    public void unlink ()
    {
        apply_renderer (current_service, null);     // TODO: safe_apply_renderer??

        // No more source
        if (current_renderer != null)
        {
            VaadinSession current_session = VaadinSession.getCurrent ();

            if (current_session != null)
            {
                current_session.lock ();
            }

            try
            {
                // The component was unlinked from UI
                current_renderer.objectUnlinked ();
            }
            finally
            {
                if (current_session != null)
                {
                    current_session.unlock ();
                }
            }
        }
        source = null;
    }

    public void updateComponent ()
    {
        if (current_renderer != null)
        {
            VaadinSession current_session = VaadinSession.getCurrent ();

            if (current_session != null)
            {
                current_session.lock ();
            }

            try
            {
                // This tells the component to update it's contents using data object
                current_renderer.objectUpdated ();
            }
            finally
            {
                if (current_session != null)
                {
                    current_session.unlock ();
                }
            }
        }
    }

    @Override // Observer
    public void update (Observable o, Object arg)
    {
        updateComponent ();
    }

    @Override // ServiceTrackerCustomizer
    public Renderer addingService (ServiceReference<Renderer> serviceReference)
    {
        Renderer renderer = ctx.getService (serviceReference);
        renderer_map.put (serviceReference, renderer);

        log.debug ("addingService: {}: {}", serviceReference, renderer);

        if (source != null && renderer.compatibleObject (source))
        {
            safe_apply_renderer (serviceReference, source);
        }

        // We need to return the object in order to track it
        return (renderer);
    }

    @Override // ServiceTrackerCustomizer
    public void modifiedService (ServiceReference<Renderer> serviceReference, Renderer renderer)
    {
        log.debug ("modifiedService: {}: {}", serviceReference, renderer);
        renderer_map.put (serviceReference, renderer);
        // TODO: REPLACE OLD RENDERER
    }

    @Override // ServiceTrackerCustomizer
    public void removedService (ServiceReference<Renderer> serviceReference, Renderer renderer)
    {
        // TODO: CLEAR OBSERVERS
        ctx.ungetService (serviceReference);
        renderer_map.remove (serviceReference);
        safe_apply_renderer (null, source);
        log.debug ("removedService: {}: {}", serviceReference, renderer);
    }
}

// EOF
