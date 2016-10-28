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

package org.rationalq.editor;

import com.vaadin.ui.AbstractComponent;
import com.vaadin.ui.AbstractOrderedLayout;
import com.vaadin.ui.VerticalLayout;

import org.lucidj.objectmanager.ObjectManager;
import org.lucidj.renderer.ObjectRenderer;
import org.lucidj.renderer.Renderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

@Component (immediate = true)
@Instantiate
@Provides
public class LayoutManager implements Renderer, ObjectManager.ObjectEventListener
{
    private final transient static Logger log = LoggerFactory.getLogger (LayoutManager.class);

    private Map<Object, ObjectRenderer> active_renderers = new HashMap<> ();
    private AbstractOrderedLayout layout;

    public LayoutManager (AbstractOrderedLayout base_layout)
    {
        layout = base_layout;
        layout.addStyleName ("renderer-layout");
        log.info ("new LayoutManager () base_layout={}", base_layout);
    }

    public LayoutManager ()
    {
        this (new VerticalLayout ());
    }

    private String get_object_hash (Object obj)
    {
        return (obj.getClass().getName() + "#" + Integer.toHexString (obj.hashCode()));
    }

    @Override // ObjectEventListener
    public Object addingObject (Object obj, int index)
    {
        ObjectRenderer or = new ObjectRenderer ();
        // TODO: VAADIN SESSION HANDLING
        layout.addComponent (or.link (obj), index);
        active_renderers.put (get_object_hash (obj), or);

        log.info ("Add new renderer {}: obj={} or={} /// active_renderers={}", this, get_object_hash (obj), or, active_renderers);
        return (obj);
    }

    @Override // ObjectEventListener
    public void changingObject (Object obj)
    {
        ObjectRenderer or = active_renderers.get (get_object_hash (obj));

        log.info ("changingObject: active_renderers={} obj={} or={} /// active_renderers={}", get_object_hash (obj), or, active_renderers);

        or.updateComponent ();
    }

    @Override // ObjectEventListener
    public void removingObject (Object obj, int index)
    {
        String hash = get_object_hash (obj);
        ObjectRenderer or = active_renderers.get (hash);

        log.info ("removingObject: obj={} or={} layout={} /// active_renderers={}",
                hash, or, layout, active_renderers);

        // Only deal with valid renderers
        if (or != null)
        {
            // TODO: VAADIN SESSION HANDLING
            layout.removeComponent (or.renderingComponent ());
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
}

// EOF
