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
import com.vaadin.server.ExternalResource;
import com.vaadin.server.FontAwesome;
import com.vaadin.server.Page;
import com.vaadin.server.Responsive;
import com.vaadin.server.Sizeable;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinServletRequest;
import com.vaadin.server.VaadinSession;
import com.vaadin.shared.communication.PushMode;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.ConnectorTracker;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Layout;
import com.vaadin.ui.MenuBar;
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
    private ConnectorTracker tracker;
    private SmartPush smart_push;

    private VerticalLayout system_toolbar = new VerticalLayout ();
    private HorizontalLayout hSearchArea = new HorizontalLayout ();
    private Layout empty_desktop = new FancyEmptyView ("Empty desktop");
    private int default_sidebar_width_pixels = 250;

    @Requires
    private SecurityEngine security;

    @Requires
    private ManagedObjectFactory object_factory;

    @Publishes (name = "searchbox", topics = "search", dataKey = "args")
    private Publisher search;

    //=========================================================================================
    // LAYOUTS
    //=========================================================================================

    private MenuBar userSettingsMenu ()
    {
        // User picture + user menu
        final MenuBar settings = new MenuBar();
        //settings.addStyleName("user-menu");

        final MenuBar.MenuItem settingsItem = settings.addItem
        (
            "Willie Coyote",
            new ExternalResource ("vaadin://~/vaadinui_libraries/willie-coyote-32.png"),
            null
        );
        settingsItem.addItem("Edit Profile", null, new MenuBar.Command()
        {
            @Override
            public void menuSelected (MenuBar.MenuItem selectedItem)
            {
                log.info ("Edit Profile");
            }
        });
        settingsItem.addItem("Preferences", null, new MenuBar.Command()
        {
            @Override
            public void menuSelected (MenuBar.MenuItem selectedItem)
            {
                log.info ("Preferences");
            }
        });
        settingsItem.addSeparator();
        settingsItem.addItem("Sign Out", FontAwesome.SIGN_OUT, new MenuBar.Command()
        {
            @Override
            public void menuSelected(MenuBar.MenuItem selectedItem)
            {
                VaadinSession.getCurrent().getSession().invalidate();
                Page.getCurrent().reload();
            }
        });

        return (settings);
    }

    private void initSystemToolbar ()
    {
        system_toolbar.setSizeFull();
        system_toolbar.setWidth("100%");

        {
            hSearchArea.setStyleName ("ui-header-area");
            hSearchArea.setWidth (100, Sizeable.Unit.PERCENTAGE);
            hSearchArea.setHeightUndefined ();
            hSearchArea.setDefaultComponentAlignment (Alignment.MIDDLE_LEFT);

            Label title = new Label("&nbsp;", ContentMode.HTML);
            title.addStyleName ("ui-header-logo");
            title.setWidth (default_sidebar_width_pixels, Sizeable.Unit.PIXELS);
            hSearchArea.addComponent (title);

            CssLayout group = new CssLayout();
            group.addStyleName ("v-component-group");
            group.addStyleName ("ui-header-search");
            final ComboBox combo = new ComboBox();
            combo.setInputPrompt("Search or paste URL...");
            //combo.setContainerDataSource(StringGenerator.generateContainer(200, false));
            combo.setNullSelectionAllowed (true);
            combo.setTextInputAllowed (true);
            combo.setNewItemsAllowed (true);
            //combo.select(combo.getItemIds().iterator().next());
            //combo.setItemCaptionPropertyId(StringGenerator.CAPTION_PROPERTY);
            //combo.setItemIconPropertyId(StringGenerator.ICON_PROPERTY);
            combo.setWidth("480px");
            group.addComponent(combo);
            Button search_button = new Button ();
            search_button.setIcon (FontAwesome.SEARCH);
            group.addComponent(search_button);
            hSearchArea.addComponent(group);
            hSearchArea.setExpandRatio (group, 1.0f);

            combo.addValueChangeListener (new Property.ValueChangeListener ()
            {
                @Override
                public void valueChange (Property.ValueChangeEvent valueChangeEvent)
                {
                    String search_args = (String)combo.getValue ();

                    if (search_args != null)
                    {
                        log.info ("SEARCH: {}", search_args);
                        search.sendData (combo.getValue ());
                    }
                }
            });

            MenuBar user = userSettingsMenu ();
            user.addStyleName ("ui-header-user");
            hSearchArea.addComponent (user);
            system_toolbar.addComponent (hSearchArea);
        }

        system_toolbar.addComponent (empty_desktop);
        system_toolbar.setExpandRatio (empty_desktop, 1.0f);
        setContent (system_toolbar);
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
                system_toolbar.replaceComponent (empty_desktop, desktop.getMainLayout ());
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
