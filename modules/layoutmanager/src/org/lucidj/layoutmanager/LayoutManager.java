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

package org.lucidj.layoutmanager;

import com.vaadin.server.ClientConnector;
import com.vaadin.ui.AbstractComponent;
import com.vaadin.ui.AbstractOrderedLayout;
import com.vaadin.ui.VerticalLayout;

import com.ejt.vaadin.sizereporter.ComponentResizeEvent;
import com.ejt.vaadin.sizereporter.ComponentResizeListener;
import com.ejt.vaadin.sizereporter.SizeReporter;
import org.lucidj.api.ManagedObject;
import org.lucidj.api.ManagedObjectInstance;
import org.lucidj.api.ObjectManager;
import org.lucidj.api.ObjectRenderer;
import org.lucidj.api.Renderer;
import org.lucidj.api.RendererFactory;
import org.lucidj.uiaccess.UIAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.jouni.restrain.Restrain;

import java.util.HashMap;
import java.util.Map;

// LayoutManager is the default renderer for ObjectManager.

public class LayoutManager implements Renderer, ObjectManager.ObjectEventListener, ComponentResizeListener, ManagedObject
{
    private final transient static Logger log = LoggerFactory.getLogger (LayoutManager.class);

    private LayoutManager self = this;
    private Map<Object, ObjectRenderer> active_renderers = new HashMap<> ();
    private AbstractOrderedLayout layout;
    private SizeReporter sizeReporter;
    private Restrain restrain;
    private int current_height = -1;

    private RendererFactory rendererFactory;

    private LayoutManager (AbstractOrderedLayout base_layout)
    {
        layout = base_layout;
        layout.addStyleName ("renderer-layout");

        layout.addAttachListener (new ClientConnector.AttachListener ()
        {
            @Override
            public void attach (ClientConnector.AttachEvent attachEvent)
            {
                // Enable SizeReporter for the whole LayoutManager
                sizeReporter = new SizeReporter (layout);
                sizeReporter.addResizeListener (self);

                // Minimum size
                restrain = new Restrain (layout);
            }
        });

        layout.addDetachListener (new ClientConnector.DetachListener ()
        {
            @Override
            public void detach (ClientConnector.DetachEvent detachEvent)
            {
                sizeReporter.removeResizeListener (self);
            }
        });

        log.info ("new LayoutManager () base_layout={}", base_layout);
    }

    public LayoutManager (RendererFactory rendererFactory)
    {
        this (new VerticalLayout ());
        this.rendererFactory = rendererFactory;
    }

    private String get_object_hash (Object obj)
    {
        return (obj.getClass().getName() + "#" + Integer.toHexString (obj.hashCode()));
    }

    @Override // ObjectEventListener
    public void restrain ()
    {
        if (current_height != -1)
        {
            new UIAccess (layout)
            {
                @Override
                public void updateUI()
                {
                    log.info ("<<RESTRAIN>> height={}", current_height);
                    restrain.setMinHeight (current_height + "px");
                }
            };
        }
    }

    @Override // ObjectEventListener
    public void release ()
    {
        new UIAccess (layout)
        {
            @Override
            public void updateUI()
            {
                log.info ("<<RELEASE>> height={}", current_height);
                restrain.setMinHeight ("auto");
            }
        };
    }

    @Override // ObjectEventListener
    public Object addingObject (Object obj, int index)
    {
        ObjectRenderer or = rendererFactory.newRenderer ();
        // TODO: VAADIN SESSION HANDLING
        layout.addComponent (or.link (obj), index);
        active_renderers.put (get_object_hash (obj), or);

        log.info ("<<RENDERER>> addingObject() layout height = {} {}", layout.getHeight (), layout.getHeightUnits ().toString ());

        log.info ("Add new renderer {}: obj={} or={} /// active_renderers={}", this, get_object_hash (obj), or, active_renderers);
        return (obj);
    }

    @Override // ObjectEventListener
    public void changingObject (Object obj)
    {
        ObjectRenderer or = active_renderers.get (get_object_hash (obj));

        log.info ("changingObject: active_renderers={} obj={} or={} /// active_renderers={}", get_object_hash (obj), or, active_renderers);

        or.updateComponent ();

        log.info ("<<RENDERER>> changingObject() layout height = {} {}", layout.getHeight (), layout.getHeightUnits ().toString ());
    }

    @Override // ObjectEventListener
    public void removingObject (Object obj, int index)
    {
        String hash = get_object_hash (obj);
        ObjectRenderer or = active_renderers.get (hash);

        log.info ("removingObject: obj={} or={} layout={} /// active_renderers={}", hash, or, layout, active_renderers);

        // Only deal with valid renderers
        if (or != null)
        {
            // TODO: VAADIN SESSION HANDLING
            layout.removeComponent (or.renderingComponent ());

            log.info ("<<RENDERER>> removingObject() layout height = {} {}", layout.getHeight (), layout.getHeightUnits ().toString ());
        }

        active_renderers.remove (hash);
    }

    @Override // Renderer
    public boolean compatibleObject (Object obj_to_check)
    {
        return (obj_to_check instanceof ObjectManager);
    }

    @Override // Renderer
    public void objectLinked (Object obj)
    {
        ObjectManager om = (ObjectManager)obj;
        om.setObjectEventListener (this);
    }

    @Override // Renderer
    public void objectUnlinked ()
    {
        // Not used
    }

    @Override // Renderer
    public AbstractComponent renderingComponent ()
    {
        return (layout);
    }

    @Override // Renderer
    public void objectUpdated ()
    {
        // Not used
    }

    @Override // ComponentResizeListener
    public void sizeChanged (ComponentResizeEvent componentResizeEvent)
    {
        log.debug ("<<RESIZE>> width={} height={}", componentResizeEvent.getWidth(), componentResizeEvent.getHeight());
        current_height = componentResizeEvent.getHeight();
    }

    @Override
    public void validate (ManagedObjectInstance instance)
    {
        // Nop
    }

    @Override
    public void invalidate (ManagedObjectInstance instance)
    {
        // Nop
    }
}

// EOF
