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

import org.lucidj.api.vui.IconHelper;
import org.lucidj.api.vui.Renderer;
import org.lucidj.api.ServiceContext;
import org.lucidj.api.ServiceObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.event.LayoutEvents;
import com.vaadin.server.Resource;
import com.vaadin.ui.AbstractComponent;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.DragAndDropWrapper;
import com.vaadin.ui.themes.ValoTheme;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.osgi.framework.BundleContext;

public class IconListRenderer extends CssLayout implements
    Renderer, LayoutEvents.LayoutClickListener, Button.ClickListener
{
    private final static Logger log = LoggerFactory.getLogger (IconListRenderer.class);

    private List<Map<String, Object>> source;

    private LayoutEvents.LayoutClickListener layout_click_listener;
    private volatile Button.ClickEvent click_event;
    private Timer debouncer_timer = new Timer ();

    private Map<Object, AbstractComponent> component_to_vaadin = new HashMap<> ();

    private IconHelper iconHelper;

    public IconListRenderer (ServiceContext serviceContext, BundleContext bundleContext)
    {
        iconHelper = serviceContext.getService (bundleContext, IconHelper.class);
        setWidth (100, Unit.PERCENTAGE);
        addLayoutClickListener (this);
    }

    private void button_caption_wrap (Button b)
    {
        String caption = b.getCaption ();
        int wrap_len = 12;

        if (caption.length () > wrap_len)
        {
            String[] words = caption.split ("\\s");
            String twoliner = "";
            int space_left = 0;
            int lines = 0;
            caption = "";

            // Simple greedy wrapping
            for (String word: words)
            {
                int len = word.length ();

                if (len + 1 > space_left)
                {
                    if (lines++ == 2)
                    {
                        twoliner = caption + "\u2026"; // Unicode ellipsis
                    }
                    caption += caption.isEmpty ()? word: "<br/>" + word;
                    space_left = wrap_len - len;
                }
                else
                {
                    caption += " " + word;
                    space_left -= len + 1;
                }
            }
            b.setCaptionAsHtml (true);
            b.setCaption (twoliner.isEmpty ()? caption: twoliner);
        }
        b.setDescription (caption);
    }

    private AbstractComponent create_icon (Map<String, Object> component)
    {
        String icon_title = (String)component.get ("iconTitle");

        if (icon_title == null)
        {
            icon_title = "No title";
        }

        Resource icon_resource = iconHelper.getIcon ((String)component.get ("iconUrl"), 32);

        Button button_icon = new Button (icon_title);
        button_icon.setIcon (icon_resource);
        button_icon.addStyleName (ValoTheme.BUTTON_BORDERLESS);
        button_icon.addStyleName (ValoTheme.BUTTON_SMALL);
        button_icon.addStyleName (ValoTheme.BUTTON_ICON_ALIGN_TOP);
        button_icon.addStyleName ("x-icon-button");
        button_icon.addStyleName ("icon-size-32");
        button_icon.addClickListener (this);
        button_icon.setWidthUndefined ();
        button_caption_wrap (button_icon);

        // Put the component in a D&D wrapper and allow dragging it
        final DragAndDropWrapper icon_dd_wrap = new DragAndDropWrapper (button_icon);
        icon_dd_wrap.setDragStartMode (DragAndDropWrapper.DragStartMode.COMPONENT);

        // Set the wrapper to wrap tightly around the component
        icon_dd_wrap.setSizeUndefined ();
        icon_dd_wrap.setData (component);

        // Set ID for drag-drop AND on the Label for double-click
        if (component.containsKey ("entryId"))
        {
            String entryId = (String)component.get ("entryId");
            icon_dd_wrap.setId (entryId);
            button_icon.setId (entryId);
        }

        // Component data is the map itself
        button_icon.setData (component);

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

    @Override // Button.ClickListener
    public void buttonClick (Button.ClickEvent clickEvent)
    {
        if (click_event == null)
        {
            // Click arrived, fire it in 5ms if it's alone
            click_event = clickEvent;
            debouncer_timer.schedule (new TimerTask ()
            {
                @Override
                public void run ()
                {
                    // Debounce click-click to double-click
                    if (click_event != null)
                    {
                        // Bubble-up the click event
                        fireEvent (clickEvent);
                        click_event = null;
                    }
                }
            }, 200/*ms*/);
        }
        else
        {
            // Click-click cancel each other, double-click is comming
            click_event = null;
        }
    }

    @Override // LayoutEvents.LayoutClickListener
    public void layoutClick (LayoutEvents.LayoutClickEvent layoutClickEvent)
    {
        // There's no need to bubble-up the event since the foreign
        // listeners are already attached directly to this component
        // via bypass. We only need to do local housekeeping.

        if (layoutClickEvent.isDoubleClick ())
        {
            // Back to default state
            click_event = null;
        }
    }

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
