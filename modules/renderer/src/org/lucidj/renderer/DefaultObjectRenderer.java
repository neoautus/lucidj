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

import org.lucidj.api.EventHelper;
import org.lucidj.api.ObjectRenderer;
import org.lucidj.api.Renderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.server.Sizeable;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.HasComponents;
import com.vaadin.ui.Label;

public class DefaultObjectRenderer extends CustomComponent implements ObjectRenderer, EventHelper.Subscriber
{
    private final static Logger log = LoggerFactory.getLogger (DefaultObjectRenderer.class);

    private Object current_object;
    private Renderer current_renderer;
    private Component current_component;
    private Label default_component;

    private DefaultRendererFactory renderer_factory;

    public DefaultObjectRenderer (DefaultRendererFactory renderer_factory)
    {
        this.renderer_factory = renderer_factory;

        default_component = new Label ("void");
        default_component.addStyleName ("custom-empty-component");
        default_component.setSizeUndefined ();
    }

    @Override // ObjectRenderer
    public boolean isRendered ()
    {
        return (current_component != null && current_renderer != null);
    }

    // TODO: ADD LINK/UNLINK NOTIFICATIONS
    @Override // ObjectRenderer
    public <A> A adapt (Class<A> type)
    {
        if (isRendered () &&
            current_renderer.getClass ().isAssignableFrom (type))
        {
            return ((A)current_renderer);
        }
        return (null);
    }

    private void apply_renderer (Object obj)
    {
        Component new_component = null;

        log.info ("apply_renderer: obj={}", obj);
        current_object = obj;

        if (obj == null)
        {
            // Will use the default component to display 'null'
            current_renderer = null;
        }
        else if ((current_renderer = renderer_factory.locateAndBindRenderer (this, obj)) != null)
        {
            // Issue initial update
            current_renderer.objectUpdated ();
            new_component = current_renderer.renderingComponent ();
        }

        // Fallback to default renderer if we fail to get proper renderer or its instance
        if (current_renderer == null)
        {
            default_component.setValue (obj == null? "null": obj.getClass ().getSimpleName ());
            new_component = default_component;
        }

        if (current_component != null)
        {
            // Copy current component dimensions
            new_component.setWidth (current_component.getWidth (), current_component.getWidthUnits ());
            new_component.setHeight (current_component.getHeight (), current_component.getHeightUnits ());
        }

        current_component = new_component;
        setCompositionRoot (new_component);

        log.info ("apply_renderer: current_renderer={} current_component={}", current_renderer, current_component);
    }

    public void refreshRenderer ()
    {
        apply_renderer (current_object);
    }

    @Override // ObjectRenderer
    public void link (Object object)
    {
        // TODO: WHEN OBJECT IS UNKNOWN, PUBLISH "BINARY" RENDERER AND WAIT FOR RENDERER ACTIVATION
        apply_renderer (object);

        if (object instanceof Renderer.Observable)
        {
            // The object can notify us for changes
            ((Renderer.Observable)object).addObserver (this);
        }

        // TODO: ADD CALLBACK custom_renderer.objectLinked ()

        log.info ("link: returning {}", current_component);
    }

    @Override // ObjectRenderer
    public void unlink ()
    {
        apply_renderer (null);

        // No more source
        if (current_renderer != null)
        {
            // The component was unlinked from UI
            current_renderer.objectUnlinked ();
        }
    }

    @Override // ObjectRenderer
    public void updateComponent ()
    {
        // This tells the component to update it's contents using data object
        if (current_renderer != null)
        {
            current_renderer.objectUpdated ();
        }
    }

    @Override // EventHelper.Subscriber
    public void event (Object event)
    {
        updateComponent ();
    }

    @Override // CustomComponent
    protected void setCompositionRoot (Component compositionRoot)
    {
        // Avoid the greedy CustomComponent problem
        if (compositionRoot != null)
        {
            HasComponents old_parent = compositionRoot.getParent();

            if (old_parent instanceof DefaultObjectRenderer)
            {
                DefaultObjectRenderer greedy_parent = (DefaultObjectRenderer)old_parent;

                if (greedy_parent.getCompositionRoot () == compositionRoot)
                {
                    // Makes the greedy CustomComponent drop the compositionRoot we need to
                    // assign to another CustomComponent, otherwise we get
                    // IllegalArgumentException: Content is already attached to another parent
                    greedy_parent.setCompositionRoot (null);
                }
            }
        }

        // Proceed as intended
        super.setCompositionRoot (compositionRoot);
    }

    @Override // CustomComponent
    public void setWidth (float width, Sizeable.Unit unit)
    {
        super.setWidth (width, unit);

        if (current_component != null)
        {
            current_component.setWidth (width, unit);
        }
    }

    @Override // CustomComponent
    public void setWidth (String width)
    {
        super.setWidth (width);

        if (current_component != null)
        {
            current_component.setWidth (width);
        }
    }

    @Override // CustomComponent
    public void setWidthUndefined ()
    {
        super.setWidthUndefined ();

        if (current_component != null)
        {
            current_component.setWidthUndefined ();
        }
    }
}

// EOF
