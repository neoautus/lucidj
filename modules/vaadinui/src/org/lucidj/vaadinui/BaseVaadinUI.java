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

package org.lucidj.vaadinui;

import com.vaadin.annotations.JavaScript;
import com.vaadin.annotations.PreserveOnRefresh;
import com.vaadin.annotations.Push;
import com.vaadin.annotations.StyleSheet;
import com.vaadin.annotations.Theme;
import com.vaadin.annotations.Title;
import com.vaadin.annotations.Widgetset;
import com.vaadin.data.Property;
import com.vaadin.event.FieldEvents;
import com.vaadin.event.ShortcutAction;
import com.vaadin.event.ShortcutListener;
import com.vaadin.server.FontAwesome;
import com.vaadin.server.Responsive;
import com.vaadin.server.Sizeable;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinServletRequest;
import com.vaadin.shared.communication.PushMode;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Layout;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;

import org.lucidj.api.ClassManager;
import org.lucidj.api.DesktopInterface;
import org.lucidj.api.DesktopUI;
import org.lucidj.api.SecurityEngine;
import org.lucidj.api.ServiceContext;
import org.lucidj.api.ServiceObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

@Theme ("valo")
@Title ("LucidJ Console")
@Widgetset ("xyz.kuori.CustomWidgetSet")
@JavaScript ("vaadin://~/vaadinui_libraries/lucidj-vaadin-helper.js")
@StyleSheet ({ "vaadin://~/vaadinui_libraries/styles.css", "vaadin://~/dynamic.css" })
@Push (PushMode.MANUAL)
@PreserveOnRefresh
public class BaseVaadinUI extends UI implements DesktopUI, Component.Listener
{
    private final static Logger log = LoggerFactory.getLogger (BaseVaadinUI.class);

    private DesktopInterface desktop;
    private SmartPush smart_push;
    private Map<String, Set<DesktopUI.Listener>> topic_map = new HashMap<> ();

    private VerticalLayout desktop_canvas = new VerticalLayout ();
    private HorizontalLayout ui_header;
    private Layout empty_desktop = new CssLayout ();
    private Layout user_component;
    // TODO: DEFINE A WAY TO SHARE GLOBAL DEFINES/CONFIGS
    private int default_sidebar_width_pixels = 240;

    // For some reason, Enter inside the combobox generates four events
    // focus/blur/focus/blur and this messes with proper Enter key handling
    private int nested_focus_blur_bug_count;

    // When the combobox have new text, clicking the search button (magnifier
    // glass) generates both changeEvent and clickEvent, but NOT Enter action.
    // So we can't just throw in the handleAction, we must use changeEvent too.
    // The flag below handles when to discard the click event in case we get
    // changeEvent+clickEvent in sequence.
    private boolean value_change_button_quirk;

    private SecurityEngine security;

    @ServiceObject.Context
    private ServiceContext serviceContext;

    public BaseVaadinUI (SecurityEngine security)
    {
        this.security = security;
    }

    //=========================================================================================
    // LAYOUTS
    //=========================================================================================

    private void initSystemToolbar ()
    {
        desktop_canvas.setSizeFull();
        desktop_canvas.setWidth("100%");

        ui_header = new HorizontalLayout ();
        {
            ui_header.setStyleName ("ui-header-area");
            ui_header.setWidth (100, Sizeable.Unit.PERCENTAGE);
            ui_header.setHeightUndefined ();
            ui_header.setDefaultComponentAlignment (Alignment.MIDDLE_LEFT);

            Label logo = new Label("&nbsp;", ContentMode.HTML);
            {
                logo.addStyleName ("ui-header-logo");
                logo.setWidth (default_sidebar_width_pixels, Sizeable.Unit.PIXELS);
            }
            ui_header.addComponent (logo);

            HorizontalLayout header_components = new HorizontalLayout ();
            {
                header_components.setWidth (100, com.vaadin.server.Sizeable.Unit.PERCENTAGE);
                header_components.setDefaultComponentAlignment (Alignment.MIDDLE_LEFT);
                header_components.setSpacing (true);

                // Search component
                CssLayout search_component = new CssLayout();
                {
                    search_component.setWidth (100, com.vaadin.server.Sizeable.Unit.PERCENTAGE);
                    search_component.setWidthUndefined ();
                    search_component.addStyleName ("v-component-group");
                    search_component.addStyleName ("ui-header-search");
                    final ComboBox search_text = new ComboBox ();
                    {
                        search_text.setInputPrompt ("Search or paste URL...");
                        //combo.setContainerDataSource(StringGenerator.generateContainer(200, false));
                        search_text.setNullSelectionAllowed (true);
                        search_text.setTextInputAllowed (true);
                        search_text.setNewItemsAllowed (true);
                        //combo.select(combo.getItemIds().iterator().next());
                        //combo.setItemCaptionPropertyId(StringGenerator.CAPTION_PROPERTY);
                        //combo.setItemIconPropertyId(StringGenerator.ICON_PROPERTY);

                        // TODO: SOMEDAY DISCOVER HOW TO EXPAND THIS GROUPED COMPONENT, AND THE CURE FOR CANCER
                        search_text.setWidth("480px");
                        search_text.addStyleName ("invisible-focus");
                        search_text.addValueChangeListener (new Property.ValueChangeListener ()
                        {
                            @Override
                            public void valueChange (Property.ValueChangeEvent valueChangeEvent)
                            {
                                String search_args = (String)search_text.getValue ();

                                if (search_args != null)
                                {
                                    fireEvent ("search", search_text.getValue ());
                                    value_change_button_quirk = true;
                                }
                            }
                        });

                        // Handles the Enter key by activating on focus and deactivating on blur
                        final ShortcutListener handle_enter = new ShortcutListener ("Enter",
                            ShortcutAction.KeyCode.ENTER, null)
                        {
                            @Override
                            public void handleAction (Object o, Object o1)
                            {
                                value_change_button_quirk = false;
                                fireEvent ("search", search_text.getValue ());
                            }
                        };

                        search_text.addFocusListener (new FieldEvents.FocusListener ()
                        {
                            @Override
                            public void focus (FieldEvents.FocusEvent focusEvent)
                            {
                                if (nested_focus_blur_bug_count++ == 0)
                                {
                                    search_text.addShortcutListener (handle_enter);
                                }
                            }
                        });

                        search_text.addBlurListener (new FieldEvents.BlurListener ()
                        {
                            @Override
                            public void blur (FieldEvents.BlurEvent blurEvent)
                            {
                                if (--nested_focus_blur_bug_count == 0)
                                {
                                    search_text.removeShortcutListener (handle_enter);
                                }
                            }
                        });

                    }
                    search_component.addComponent (search_text);

                    Button search_button = new Button ();
                    {
                        search_button.setIcon (FontAwesome.SEARCH);
                        search_button.addClickListener (new Button.ClickListener ()
                        {
                            @Override
                            public void buttonClick (Button.ClickEvent clickEvent)
                            {
                                if (!value_change_button_quirk)
                                {
                                    fireEvent ("search", search_text.getValue ());
                                }
                                value_change_button_quirk = false;
                            }
                        });
                        search_button.addStyleName ("invisible-focus");
                    }
                    search_component.addComponent (search_button);
                }
                header_components.addComponent (search_component);

                // User component
                user_component = new HorizontalLayout ();
                {
                    user_component.setStyleName ("ui-header-user");
                    user_component.setWidthUndefined ();
                }
                header_components.addComponent (user_component);

                // I swear someday I'll learn CSS, AFTER implementing my own distributed
                // operating system with augmented reality interface and a machine learning kernel,
                // all written in Z80 assembly, as a preparation for the task.
                Label spacer = new Label ();
                spacer.setWidthUndefined ();
                header_components.addComponent (spacer);

                // Search expands
                header_components.setExpandRatio (search_component, 1.0f);
            }
            ui_header.addComponent (header_components);
            ui_header.setExpandRatio (header_components, 1.0f);
        }

        desktop_canvas.addComponent (ui_header);
        desktop_canvas.addComponent (empty_desktop);
        desktop_canvas.setExpandRatio (empty_desktop, 1.0f);
        setContent (desktop_canvas);
    }

    //=========================================================================================
    // UI INITIALIZATION
    //=========================================================================================

    private void show_desktop ()
    {
        initSystemToolbar ();

        desktop = serviceContext.newServiceObject (DesktopInterface.class);

        log.info ("----------> desktop = {}", desktop);

        // TODO: HANDLE MISSING DESKTOPS
        if (desktop != null)
        {
            desktop.init (this);

            // Set the main desktop area
            desktop_canvas.replaceComponent (empty_desktop, desktop.getMainLayout ());

            // Clear old security layout and set/add the newer one
            user_component.removeAllComponents ();
            user_component.addComponent (desktop.getSecurityLayout ());
        }
    }

    private void init_desktop (VaadinRequest vaadinRequest)
    {
        Responsive.makeResponsive (this);
        setLocale (vaadinRequest.getLocale());

        if (vaadinRequest instanceof VaadinServletRequest)
        {
            VaadinServletRequest vsr = (VaadinServletRequest)vaadinRequest;
            InetAddress remote_addr = null;

            // TODO: USE AUTOMATIC KEY AUTHENTICATION FOR SINGLE MODE
            try
            {
                remote_addr = InetAddress.getByName (vsr.getRemoteAddr ());
            }
            catch (UnknownHostException ignore) {};

            // Login tokens may be used only when browsing from the same machine
            if (remote_addr != null && remote_addr.isLoopbackAddress ()
                && Login.isValidLoginToken (vsr.getParameter ("token")))
            {
                // Autologin into System when browsing from localhost
                log.warn ("Automatic login from localhost using login token");
                security.createSystemSubject ();

                // Erase the token from URL
                getPage ().getJavaScript().execute("window.lucidj_vaadin_helper.clearUrl ()");
            }
        }

        if (!security.getSubject ().isAuthenticated ())
        {
            setContent (new Login (security, new Login.LoginListener ()
            {
                @Override
                public void loginSuccessful()
                {
                    show_desktop ();
                }
            }));
        }
        else
        {
            show_desktop ();
        }
    }

    @Override
    protected void init (VaadinRequest vaadinRequest)
    {
        init_desktop (vaadinRequest);
        smart_push = new SmartPush (this);

        // Add custom UI event forwarding
        addListener (this);
    }

    @Override
    public void close ()
    {
        smart_push.stop ();
        super.close ();
    }

    //=========================================================================================
    // UI EVENTS
    //=========================================================================================

    class DesktopEvent extends Component.Event
    {
        private String topic;
        private Object eventObject;

        public DesktopEvent (String topic, Object eventObject)
        {
            super (BaseVaadinUI.this);
            this.topic = topic;
            this.eventObject = eventObject;
        }

        public String getTopic ()
        {
            return (topic);
        }

        public Object getEventObject ()
        {
            return (eventObject);
        }
    }

    @Override // DesktopUI
    public void addListener (String topic, DesktopUI.Listener listener)
    {
        Set<DesktopUI.Listener> listener_set = topic_map.get (topic);

        if (listener_set == null)
        {
            listener_set = new HashSet<> ();
            topic_map.put (topic, listener_set);
        }
        listener_set.add (listener);
    }

    @Override // DesktopUI
    public void fireEvent (String topic, Object eventObject)
    {
        fireEvent (new DesktopEvent (topic, eventObject));
    }

    @Override // Component.Listener
    public void componentEvent (Event event)
    {
        if (event instanceof DesktopEvent)
        {
            DesktopEvent desktop_event = (DesktopEvent)event;
            String topic = desktop_event.getTopic ();
            Set<DesktopUI.Listener> listener_set = topic_map.get (topic);

            if (listener_set == null)
            {
                return;
            }

            Object event_object = desktop_event.getEventObject ();
            Iterator<DesktopUI.Listener> itl = listener_set.iterator ();

            while (itl.hasNext ())
            {
                DesktopUI.Listener listener = itl.next ();

                if (!ClassManager.isZoombie (listener))
                {
                    listener.event (topic, event_object);
                }
                else
                {
                    // Auto-clean topic sets
                    log.info ("Removing zoombie listener: {}", listener);
                    itl.remove ();
                }
            }
        }
    }

    @Override
    public void attach ()
    {
        log.info ("UI attach: {}", this);

        // Normal detach for everybody
        super.attach ();

        if (desktop != null)
        {
            desktop.attach ();
        }
    }

    @Override
    public void detach ()
    {
        log.info ("UI detach: {}", this);

        if (desktop != null)
        {
            desktop.detach ();
        }

        // Normal detach for everybody
        super.detach();
    }

    @ServiceObject.Invalidate
    private void invalidate ()
    {
        getPushConnection ().disconnect ();
    }
}

// EOF
