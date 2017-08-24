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
import org.lucidj.api.ServiceObject;

import com.vaadin.event.LayoutEvents;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.AbstractComponent;
import com.vaadin.ui.Component;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.DragAndDropWrapper;
import com.vaadin.ui.Label;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.osgi.framework.BundleContext;

public class IconListRenderer extends CssLayout implements Renderer, LayoutEvents.LayoutClickListener
{
    private List<Map<String, Object>> source;

    private LayoutEvents.LayoutClickListener layout_click_listener;

    private Map<Object, AbstractComponent> component_to_vaadin = new HashMap<> ();

    private BundleContext bundleContext;

    public IconListRenderer (BundleContext bundleContext)
    {
        this.bundleContext = bundleContext;
        addLayoutClickListener (this);
    }

    private AbstractComponent create_icon (Map<String, Object> component)
    {
        String canonical_name = (String)component.get ("class");

        if (canonical_name == null)
        {
            canonical_name = "missing_canonical_name";
        }

        String icon_title = (String)component.get ("iconTitle");

        if (icon_title == null)
        {
            icon_title = "No title";
        }

        String icon_url = (String)component.get ("iconUrl");

        if (icon_url == null)
        {
            icon_url = "/VAADIN/~/" + bundleContext.getBundle ().getSymbolicName () + "/tangram-icon-512x512.png";
        }

        int base_width = 6;
        int margin_h_size_px = base_width / 2;
        int margin_v_size_px = base_width;
        int icon_size_px = base_width * 6;
        int font_size_px = 2 * base_width + 2;
        int icon_box_width_px = base_width * 12;

        String icon_html =
            "<div style='text-align: center; height:auto; display:inline-block; " +
            "margin:" + margin_v_size_px + "px " + margin_h_size_px + "px;" +
            "width:" + icon_box_width_px + "px; line-height:1.1em;'>" +
            "<img src='" + icon_url + "' " +
            "width='" + icon_size_px + "px' height='" + icon_size_px + "px' />" +
            "<div style='white-space:normal; word-wrap:break-word; font-weight: 400;" +
            "font-size:" + font_size_px + "px;'>" + icon_title + "</div>" +
            "</div>";

        Label icon_label = new Label (icon_html, ContentMode.HTML);
        icon_label.setWidthUndefined ();

        // Put the component in a D&D wrapper and allow dragging it
        final DragAndDropWrapper icon_dd_wrap = new DragAndDropWrapper (icon_label);
        icon_dd_wrap.setDragStartMode (DragAndDropWrapper.DragStartMode.COMPONENT);

        // Set the wrapper to wrap tightly around the component
        icon_dd_wrap.setSizeUndefined ();
        icon_dd_wrap.setData (component);

        // Set canonical_name for drag-drop AND on the Label for double-click
        icon_dd_wrap.setId (canonical_name);
        icon_label.setId (canonical_name);

        // Remember this association
        component_to_vaadin.put (component, icon_dd_wrap);
        return (icon_dd_wrap);
    }

    private void update_components ()
    {
        // Delete all excess vaadin components
        for (int index = getComponentCount () - 1; index >= 0; index--)
        {
            AbstractComponent c = (AbstractComponent)getComponent (index);

            if (!source.contains (c.getData ()))
            {
                removeComponent (c);
            }
        }

        // Add all missing vaadin components
        for (int index = 0; index < source.size (); index++)
        {
            Map<String, Object> component_object = source.get (index);

            if (!component_to_vaadin.containsKey (component_object))
            {
                addComponent (create_icon (component_object), index);
            }
        }
    }

    @Override
    public void layoutClick (LayoutEvents.LayoutClickEvent layoutClickEvent)
    {
        if (layout_click_listener != null)
        {
            layout_click_listener.layoutClick (layoutClickEvent);
        }
    }

//    public void setPaletteClickListener (LayoutEvents.LayoutClickListener listener)
//    {
//        layout_click_listener = listener;
//    }

    public static boolean isCompatible (Object object)
    {
        return (object instanceof List);
    }

    @Override // Renderer
    public void objectLinked (Object obj)
    {
        source = (List<Map<String, Object>>)obj;
        update_components ();
    }

    @Override // Renderer
    public void objectUnlinked ()
    {
        source = null;
        component_to_vaadin.clear ();
        removeAllComponents ();
    }

    @Override // Renderer
    public Component renderingComponent ()
    {
        return (this);
    }

    @Override // Renderer
    public void objectUpdated ()
    {
        update_components ();
    }

    @ServiceObject.Validate
    private void validate ()
    {
        // Nop
    }

    @ServiceObject.Invalidate
    private void invalidate ()
    {
        // Nop
    }
}

// EOF
