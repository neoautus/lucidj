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

package org.lucidj.formulas;

import org.lucidj.api.ComponentInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.lucidj.uiaccess.UIAccess;

import com.vaadin.event.LayoutEvents;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.DragAndDropWrapper;
import com.vaadin.ui.Label;

import java.util.HashMap;
import java.util.Map;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public class ComponentPalette extends CssLayout implements LayoutEvents.LayoutClickListener,
    ServiceTrackerCustomizer<ComponentInterface, ComponentInterface>
{
    private final transient static Logger log = LoggerFactory.getLogger (ComponentPalette.class);
    private final ComponentPalette self = this;
    private LayoutEvents.LayoutClickListener layout_click_listener;

    private BundleContext ctx;
    private ServiceTracker<ComponentInterface, ComponentInterface> tracker;
    private Map<ServiceReference<ComponentInterface>, ComponentInterface> component_map = new HashMap<> ();

    public ComponentPalette (BundleContext ctx)
    {
        // Track all available components
        this.ctx = ctx;
        tracker = new ServiceTracker<> (ctx, ComponentInterface.class, this);
        tracker.open ();

        addLayoutClickListener (this);
    }

    private boolean add_component_to_palette (Bundle bnd, ComponentInterface component)
    {
        String canonical_name = component.getClass ().getCanonicalName();
        String icon_title = component.getIconTitle ();
        String bundle_symbolic_name = bnd.getSymbolicName ();

        log.info ("*** => ADDING component {} ({})", canonical_name, component);

        int base_width = 6;
        int margin_h_size_px = base_width / 2;
        int margin_v_size_px = base_width;
        int icon_size_px = base_width * 6;
        int font_size_px = 2 * base_width + 2;
        int icon_box_width_px = base_width * 12;

        String icon_html =
            "<div style='text-align: center; height:auto; display:inline-block; " +
                "margin:" + margin_v_size_px + "px " + margin_h_size_px + "px;" +
                "width:" + icon_box_width_px + "px;'>" +
            "<img src='/VAADIN/" + bundle_symbolic_name + "/component-icon.png' " +
                "width='" + icon_size_px + "px' height='" + icon_size_px + "px' />" +
            "<div style='white-space:normal; word-wrap:break-word; font-weight: 400;" +
                "font-size:" + font_size_px + "px;'>" + icon_title + "</div>" +
            "</div>";

        Label icon_label = new Label (icon_html, ContentMode.HTML);
        icon_label.setWidthUndefined ();

        // Put the component in a D&D wrapper and allow dragging it
        final DragAndDropWrapper icon_dd_wrap = new DragAndDropWrapper (icon_label);
        icon_dd_wrap.setDragStartMode(DragAndDropWrapper.DragStartMode.COMPONENT);

        // Set the wrapper to wrap tightly around the component
        icon_dd_wrap.setSizeUndefined();
        icon_dd_wrap.setData (component);

        // Set canonical_name for drag-drop AND on the Label for double-click
        icon_dd_wrap.setId (canonical_name);
        icon_label.setId (canonical_name);

        new UIAccess (self)
        {
            @Override
            public void updateUI()
            {
                // Add the wrapper, not the component, to the layout
                self.addComponent (icon_dd_wrap);
            }
        };

        return (true);
    }

    @Override
    public void layoutClick (LayoutEvents.LayoutClickEvent layoutClickEvent)
    {
        if (layout_click_listener != null)
        {
            layout_click_listener.layoutClick (layoutClickEvent);
        }
    }

    public void setPaletteClickListener (LayoutEvents.LayoutClickListener listener)
    {
        layout_click_listener = listener;
    }

    @Override // ServiceTrackerCustomizer
    public ComponentInterface addingService (ServiceReference<ComponentInterface> serviceReference)
    {
        ComponentInterface component = ctx.getService (serviceReference);
        component_map.put (serviceReference, component);

        log.info ("addingService Component: {}: {}", serviceReference, component);

        add_component_to_palette (serviceReference.getBundle (), component);

        // We need to return the object in order to track it
        return (component);

    }

    @Override // ServiceTrackerCustomizer
    public void modifiedService (ServiceReference<ComponentInterface> serviceReference, ComponentInterface component)
    {
        log.info ("modifiedService Component: {}: {}", serviceReference, component);
        component_map.put (serviceReference, component);
    }

    @Override // ServiceTrackerCustomizer
    public void removedService (ServiceReference<ComponentInterface> serviceReference, ComponentInterface component)
    {
        ctx.ungetService (serviceReference);
        component_map.remove (serviceReference);
        log.info ("removedService Component: {}: {}", serviceReference, component);
    }
}

// EOF
