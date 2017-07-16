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

import com.vaadin.annotations.PreserveOnRefresh;
import com.vaadin.annotations.Push;
import com.vaadin.annotations.StyleSheet;
import com.vaadin.annotations.Theme;
import com.vaadin.annotations.Title;
import com.vaadin.annotations.Widgetset;
import com.vaadin.data.Property;
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
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Layout;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;

import org.lucidj.api.DesktopInterface;
import org.lucidj.api.ManagedObjectFactory;
import org.lucidj.api.ManagedObjectInstance;
import org.lucidj.api.SecurityEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.handlers.event.Publishes;
import org.apache.felix.ipojo.handlers.event.publisher.Publisher;

@Theme ("valo")
@Title ("LucidJ Console")
@Widgetset ("xyz.kuori.CustomWidgetSet")
@StyleSheet ("vaadin://~/vaadinui_libraries/styles.css")
@Component (immediate = true)
@Instantiate
@Provides (specifications = UI.class)
@Push (PushMode.MANUAL)
@PreserveOnRefresh
public class BaseVaadinUI extends UI
{
    private final static transient Logger log = LoggerFactory.getLogger (BaseVaadinUI.class);

    private DesktopInterface desktop;
    private SmartPush smart_push;

    private VerticalLayout desktop_canvas = new VerticalLayout ();
    private HorizontalLayout ui_header;
    private Layout empty_desktop = new CssLayout ();
    private Layout user_component;
    // TODO: DEFINE A WAY TO SHARE GLOBAL DEFINES/CONFIGS
    private int default_sidebar_width_pixels = 240;

    @Requires
    private SecurityEngine security;

    @Requires
    private ManagedObjectFactory object_factory;

    @Publishes (name = "searchbox", topics = "search", dataKey = "args")
    private Publisher search;

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
                    }
                    search_component.addComponent (search_text);

                    Button search_button = new Button ();
                    {
                        search_button.setIcon (FontAwesome.SEARCH);
                    }
                    search_component.addComponent (search_button);

                    search_text.addValueChangeListener (new Property.ValueChangeListener ()
                    {
                        @Override
                        public void valueChange (Property.ValueChangeEvent valueChangeEvent)
                        {
                            String search_args = (String)search_text.getValue ();

                            if (search_args != null)
                            {
                                log.info ("SEARCH: {}", search_args);
                                search.sendData (search_text.getValue ());
                            }
                        }
                    });
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
                // operating system with virtual reality interface and a machine learning kernel,
                // as a preparation for the task.
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

        ManagedObjectInstance[] desktops = object_factory.getManagedObjects (DesktopInterface.class, null);

        if (desktops.length > 0)
        {
            ManagedObjectInstance desktop_instance = object_factory.newInstance (desktops [0]);
            DesktopInterface desktop = desktop_instance.adapt (DesktopInterface.class);

            log.info ("----------> desktop = {}", desktop);

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
    }

    private void init_desktop (VaadinRequest vaadinRequest)
    {
        Responsive.makeResponsive (this);
        setLocale (vaadinRequest.getLocale());

        if (vaadinRequest instanceof VaadinServletRequest)
        {
            VaadinServletRequest vsr = (VaadinServletRequest)vaadinRequest;
            InetAddress remote_addr = null;

            // TODO: STILL CRAPPY, FIND A BETTER WAY
            try
            {
                remote_addr = InetAddress.getByName (vsr.getRemoteAddr ());
            }
            catch (UnknownHostException ignore) {};

            // TODO: CAVEATS??
            if (remote_addr != null && remote_addr.isLoopbackAddress ())
            {
                // Autologin into System when browsing from localhost
                security.createSystemSubject ();
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

    @Override
    public void attach ()
    {
        log.info ("UI attach: {}", this);

        // Normal detach for everybody
        super.attach ();

        if (desktop != null)
        {
            desktop.detach ();
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
}

// EOF
