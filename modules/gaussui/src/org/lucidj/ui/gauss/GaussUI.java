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

package org.lucidj.ui.gauss;

import org.lucidj.api.ApplicationInterface;
import org.lucidj.api.DesktopInterface;
import org.lucidj.api.ManagedObject;
import org.lucidj.api.ManagedObjectInstance;
import org.lucidj.api.MenuEntry;
import org.lucidj.api.MenuInstance;
import org.lucidj.api.MenuManager;
import org.lucidj.api.NavigatorManager;
import org.lucidj.api.ObjectRenderer;
import org.lucidj.api.RendererFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.event.LayoutEvents;
import com.vaadin.navigator.Navigator;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.navigator.ViewDisplay;
import com.vaadin.server.FontAwesome;
import com.vaadin.server.Page;
import com.vaadin.server.Sizeable;
import com.vaadin.server.Sizeable.Unit;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.AbstractComponent;
import com.vaadin.ui.Accordion;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.HorizontalSplitPanel;
import com.vaadin.ui.Label;
import com.vaadin.ui.Layout;
import com.vaadin.ui.Notification;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;

import java.io.PrintWriter;
import java.io.StringWriter;

public class GaussUI implements DesktopInterface, MenuInstance.EventListener, ManagedObject
{
    private final static transient Logger log = LoggerFactory.getLogger (GaussUI.class);

    //  Layout structure:
    //  +----------------------------------------------------------+
    //  | vAppLayout                                               |
    //  | +------------------+-----------------------------------+ |
    //  | | menuArea         | hToolbarArea                      | |
    //  | | +--------------+ +--------------------+--------------+ |
    //  | | | menuHeader   | | vContentArea       | vSidebarArea | |
    //  | | |              | |                    |              | |
    //  | | |              | |                    |              | |
    //  | | | +----------+ | |                    |              | |
    //  | | | |navMenu   | | |                    |              | |
    //  | | | |          | | |                    |              | |
    //  | | | |          | | |                    |              | |
    //  | | | |          | | |                    |              | |
    //  | | | |          | | |                    |              | |
    //  | | | +----------+ | |                    |              | |
    //  | | +--------------+ |                    |              | |
    //  | +------------------+--------------------+--------------+ |
    //  +----------------------------------------------------------+

    private final static int MIN_DEF_COLUMN_WIDTH_PX = 240;
    private final static int MIN_LEFT_PANEL_WIDTH_PX = 120;
    private final static int MIN_RIGHT_PANEL_WIDTH_PX = 120;
    private int default_left_panel_width_px;
    private int default_right_panel_width_px;

    private VerticalLayout vAppLayout = new VerticalLayout ();
    private HorizontalSplitPanel hsMenuContents = new HorizontalSplitPanel ();
    private HorizontalSplitPanel hsContentsSidebar = new HorizontalSplitPanel ();
    private HorizontalLayout hToolbarArea = new HorizontalLayout();
    private CssLayout hToolbarPlaceholder = new CssLayout ();
    private CssLayout emptyContents = new CssLayout ();
    private CssLayout emptySidebar = new CssLayout ();
    private HorizontalLayout hSecurityArea = new HorizontalLayout ();
    private Button toggle_sidebar;
    private Accordion acMenu = new Accordion ();

    private String DAMN = "damage.report";
    private ErrorView damage_report_view = new ErrorView ();

    private Navigator navigator;

    private MenuManager menu_manager;
    private NavigatorManager nav_manager;

    private MenuInstance main_menu;
    private RendererFactory rendererFactory;
    private ObjectRenderer objectRenderer;

    //=========================================================================================
    // DEFAULTS
    //=========================================================================================

    private int get_def_column_width_px ()
    {
        Page page = UI.getCurrent ().getPage ();
        int page_width = page.getBrowserWindowWidth ();
        int column_width = page_width / 6;

        if (column_width < MIN_DEF_COLUMN_WIDTH_PX)
        {
            return (MIN_DEF_COLUMN_WIDTH_PX);
        }
        return (column_width);
    }

    private int get_default_left_panel_width ()
    {
        if (default_left_panel_width_px == 0)
        {
            default_left_panel_width_px = get_def_column_width_px ();
        }
        return (default_left_panel_width_px);
    }

    private int get_default_right_panel_width ()
    {
        if (default_right_panel_width_px == 0)
        {
            default_right_panel_width_px = get_def_column_width_px ();
        }
        return (default_right_panel_width_px);
    }

    //=========================================================================================
    // RIGHT SIDEBAR
    //=========================================================================================

    private boolean sidebar_visible ()
    {
        return (!hsContentsSidebar.isLocked ());
    }

    private void show_sidebar (boolean visible)
    {
        if (visible == sidebar_visible ())
        {
            // Nothing changed
            return;
        }

        if (visible)
        {
            hsContentsSidebar.setLocked (false);
            hsContentsSidebar.setMinSplitPosition (MIN_RIGHT_PANEL_WIDTH_PX, Unit.PIXELS);
            hsContentsSidebar.setSplitPosition (get_default_right_panel_width (), Unit.PIXELS, true);
        }
        else
        {
            if (default_right_panel_width_px != 0) // Record size only after proper init
            {
                default_right_panel_width_px = (int)hsContentsSidebar.getSplitPosition ();
            }
            hsContentsSidebar.setMinSplitPosition (0, Unit.PIXELS);
            hsContentsSidebar.setSplitPosition (0, Unit.PIXELS, true);
            hsContentsSidebar.setLocked (true);
        }
    }

    private void set_contents (Component contents)
    {
        hsContentsSidebar.setFirstComponent (contents);
    }

    private void set_sidebar (Component sidebar)
    {
        if (sidebar != null)
        {
            // Sidebar visible and toggle button enabled
            hsContentsSidebar.setSecondComponent (sidebar);
            toggle_sidebar.setEnabled (true);
            show_sidebar (true);
        }
        else
        {
            // No contents, sidebar hidden and toggle button disabled
            show_sidebar (false);
            hsContentsSidebar.setSecondComponent (emptySidebar);
            toggle_sidebar.setEnabled (false);
        }
    }

    //=========================================================================================
    // LAYOUTS
    //=========================================================================================

    private void initAppLayout ()
    {
        vAppLayout.setSizeFull();
        vAppLayout.setWidth("100%");

        {
            initToolbarArea ();
            vAppLayout.addComponent (hToolbarArea);
        }

        {
            hsMenuContents.setSizeFull ();
            vAppLayout.addComponent (hsMenuContents);
            vAppLayout.setExpandRatio (hsMenuContents, 1.0f);
        }

        // Accordion menu
        {
            // Create the logical menu
            main_menu = menu_manager.newMenuInstance (null);
            main_menu.setEventListener (this);

            // Create the menu renderer
            objectRenderer = rendererFactory.newRenderer ();
            com.vaadin.ui.Component main_menu_component = objectRenderer.link (main_menu);
            main_menu_component.setWidth (100, Unit.PERCENTAGE);
            main_menu_component.setHeightUndefined ();

            // Add the rendered component into navigation panel
            acMenu.addStyleName ("borderless");
            acMenu.addTab (main_menu_component, "Navigation");
            acMenu.setSizeFull ();
        }

        emptyContents = new CssLayout ();
        emptyContents.addStyleName ("fancy-grid-background");
        set_contents (emptyContents);

        emptySidebar.addStyleName ("fancy-grid-background");
        hsContentsSidebar.setSecondComponent (emptySidebar);
        show_sidebar (false);

        hsMenuContents.setFirstComponent (acMenu);
        hsMenuContents.setSecondComponent (hsContentsSidebar);
        hsMenuContents.setMinSplitPosition (MIN_LEFT_PANEL_WIDTH_PX, Unit.PIXELS);
        hsMenuContents.setSplitPosition (get_default_left_panel_width (), Sizeable.Unit.PIXELS);
    }

    @Override // MenuInstance.EventListener
    public void entrySelectedEvent (MenuEntry entry)
    {
        navigator.navigateTo (entry.getNavId ());
    }

    private void initToolbarArea ()
    {
        hToolbarArea.setStyleName ("ui-toolbar-area");
        hToolbarArea.setSizeUndefined ();
        hToolbarArea.setWidth ("100%");

        CssLayout home_buttons = new CssLayout ();
        home_buttons.setStyleName ("ui-toolbar-area-home");
        home_buttons.setWidth (get_default_left_panel_width (), Sizeable.Unit.PIXELS);
        home_buttons.setId ("_home_buttons");

        final Button toggle_menu = new Button ();
        toggle_menu.setIcon (FontAwesome.BARS);
        toggle_menu.addStyleName ("tiny");
        toggle_menu.addStyleName ("link");
        toggle_menu.addStyleName ("ui-toolbar-spacer");
        toggle_menu.addStyleName ("ui-toggle-button");
        toggle_menu.setId ("_toggle_menu");
        home_buttons.addComponent (toggle_menu);

        toggle_menu.addClickListener (new Button.ClickListener ()
        {
            @Override
            public void buttonClick (Button.ClickEvent clickEvent)
            {
                if (!hsMenuContents.isLocked ())
                {
                    default_left_panel_width_px = (int)hsMenuContents.getSplitPosition ();
                    acMenu.setVisible (false);
                    hsMenuContents.setMinSplitPosition (0, Unit.PIXELS);
                    hsMenuContents.setSplitPosition (0, Sizeable.Unit.PIXELS);
                    hsMenuContents.setLocked (true);
                }
                else
                {
                    hsMenuContents.setLocked (false);
                    acMenu.setVisible (true);
                    hsMenuContents.setMinSplitPosition (MIN_LEFT_PANEL_WIDTH_PX, Unit.PIXELS);
                    hsMenuContents.setSplitPosition (get_default_left_panel_width (), Unit.PIXELS);
                }
            }
        });

        final Button home = new Button ("Home");
        home.setIcon (FontAwesome.HOME);
        home.addStyleName ("tiny");
        home.addStyleName ("ui-toolbar-spacer");
        home.addStyleName ("ui-toggle-button");
        home.setId ("_home");
        home_buttons.addComponent (home);

        home.addClickListener (new Button.ClickListener ()
        {
            @Override
            public void buttonClick (Button.ClickEvent clickEvent)
            {
                navigator.navigateTo ("home");
            }
        });

        final Button compose = new Button ("New");
        compose.setIcon (FontAwesome.EDIT);
        compose.addStyleName ("tiny");
        compose.addStyleName ("primary");
        compose.addStyleName ("ui-toggle-button");
        compose.setId ("_new");
        home_buttons.addComponent (compose);
        compose.addClickListener (new Button.ClickListener ()
        {
            @Override
            public void buttonClick (Button.ClickEvent clickEvent)
            {
                navigator.navigateTo ("compose");
            }
        });

        hToolbarArea.addComponent (home_buttons);

        hToolbarPlaceholder = new CssLayout ();
        hToolbarPlaceholder.setSizeFull ();
        hToolbarArea.addComponent (hToolbarPlaceholder);
        hToolbarArea.setExpandRatio (hToolbarPlaceholder, 1.0f);

        final Button eject_view = new Button ();
        eject_view.setIcon (FontAwesome.EXTERNAL_LINK);
        eject_view.addStyleName ("tiny");
        eject_view.addStyleName ("link");
        eject_view.addStyleName ("ui-toggle-button");
        eject_view.setId ("_eject_view");
        hToolbarArea.addComponent (eject_view);

        eject_view.addClickListener (new Button.ClickListener ()
        {
            @Override
            public void buttonClick (Button.ClickEvent clickEvent)
            {
                Notification.show ("Not implemented", Notification.Type.HUMANIZED_MESSAGE);
            }
        });

        toggle_sidebar = new Button ();
        toggle_sidebar.setIcon (FontAwesome.BARS);
        toggle_sidebar.addStyleName ("tiny");
        toggle_sidebar.addStyleName ("link");
        toggle_sidebar.addStyleName ("ui-toolbar-spacer");
        toggle_sidebar.addStyleName ("ui-toggle-button");
        toggle_sidebar.setId ("_toggle_sidebar");
        hToolbarArea.addComponent (toggle_sidebar);

        toggle_sidebar.addClickListener (new Button.ClickListener ()
        {
            @Override
            public void buttonClick (Button.ClickEvent clickEvent)
            {
                show_sidebar (!sidebar_visible ());
            }
        });
    }

    private void initSecurityArea ()
    {
        HorizontalLayout click_catcher = new HorizontalLayout ();
        {
            click_catcher.setDefaultComponentAlignment (Alignment.MIDDLE_LEFT);

            String fancy_css = "background-color: white; vertical-align:middle; width: 32px; height: 32px; border-radius: 50%;";
            String userinfo_html =
                "<span style='vertical-align:middle;'>LucidJ Admin</span>" +
                "&nbsp;&nbsp;" +
                "<img style='" + fancy_css + "' src='/VAADIN/~/vaadinui_libraries/user-frank-128x128.png'>";
            Label userinfo = new Label (userinfo_html, ContentMode.HTML);
            click_catcher.addComponent (userinfo);

            click_catcher.addLayoutClickListener (new LayoutEvents.LayoutClickListener ()
            {
                @Override
                public void layoutClick (LayoutEvents.LayoutClickEvent layoutClickEvent)
                {
                    navigator.navigateTo ("accounts");
                }
            });
        }
        hSecurityArea.addComponent (click_catcher);
    }

    protected void initAllLayouts ()
    {
        initAppLayout ();
    }

    //=========================================================================================
    // VAADIN NAVIGATOR SYSTEM
    //=========================================================================================

    protected void initNavigator (UI ui)
    {
        navigator = new SafeNavigator (ui, new ViewDisplay ()
        {
            @Override
            public void showView (View view)
            {
                if (view instanceof com.vaadin.ui.Component)
                {
                    set_contents ((com.vaadin.ui.Component)view);
                }
                else
                {
                    String msg = "Invalid component:\n" + view.getClass ().getCanonicalName ();
                    set_contents (emptyContents);
                }
            }
        });

//        navigator.setErrorView (FancyEmptyView.class);  ---> make it better
        navigator.addView (DAMN, damage_report_view);
        nav_manager.configureNavigator (navigator, null);

        navigator.addViewChangeListener(new ViewChangeListener ()
        {
            @Override
            public boolean beforeViewChange (final ViewChangeEvent event)
            {
                if (false /* go somewhere else */)
                {
                    navigator.navigateTo ("neverland");
                    return (false);
                }

                // Go ahead
                return (true);
            }

            @Override
            public void afterViewChange (final ViewChangeEvent event)
            {
                String navid = event.getViewName();

                // TODO: BUBBLE EVENT INTO PROPER MenuInstance
//                highlight_menu_item(navid);

                View new_view = event.getNewView ();
                AbstractComponent sidebar = null;
                AbstractComponent toolbar = null;

                if (new_view instanceof ApplicationInterface)
                {
                    ApplicationInterface app_view = (ApplicationInterface)new_view;

                    sidebar = app_view.getSidebar ();
                    toolbar = app_view.getToolbar ();
                }

                log.info ("Sidebar navid:{} = {}", navid, sidebar);
                log.info ("Toolbar navid:{} = {}", navid, toolbar);

                //---------------
                // Place sidebar
                //---------------

                set_sidebar (sidebar);

                //---------------
                // Place toolbar
                //---------------

                hToolbarPlaceholder.removeAllComponents();

                if (toolbar != null)
                {
                    // Attach a new toolbar
                    hToolbarPlaceholder.addComponent(toolbar);
                }

                // TODO: FIGURE OUT THE DESTINY OF THIS
                // This exists to autohide responsive sandwich menu.
                //menu.removeStyleName ("valo-menu-visible");
            }
        });

        // Start on default Home
        navigator.navigateTo ("home");
    }

    @Override // DesktopInterface
    public void init (UI ui)
    {
        initAllLayouts ();
        initSecurityArea ();
        initNavigator (ui);
    }

    @Override // DesktopInterface
    public Layout getMainLayout ()
    {
        return (vAppLayout);
    }

    @Override
    public Layout getSecurityLayout ()
    {
        return (hSecurityArea);
    }

    @Override // DesktopInterface
    public void attach ()
    {
        // Nothing
    }

    @Override // DesktopInterface
    public void detach()
    {
        // TODO: COMPONENT CLEANUP!
        log.info ("detach() " + this);
    }

    @Override // ManagedObject
    public void validate (ManagedObjectInstance instance)
    {
        menu_manager = instance.getObject (MenuManager.class);
        nav_manager = instance.getObject (NavigatorManager.class);
        rendererFactory = instance.getObject (RendererFactory.class);
    }

    @Override // ManagedObject
    public void invalidate (ManagedObjectInstance instance)
    {
        if (navigator != null && navigator.getUI ().isAttached ())
        {
            // TODO: INVALIDATE THE DESTKTOP, _NOT_ THE WHOLE SESSION AND UI
            UI attached_ui = navigator.getUI();
            attached_ui.getSession ().getSession ().invalidate ();
            attached_ui.getPage ().reload ();
        }
    }

    class SafeNavigator extends Navigator
    {
        public SafeNavigator (UI ui, ViewDisplay viewDisplay)
        {
            super (ui, viewDisplay);
        }

        @Override
        public void navigateTo (String navigationState)
        {
            try
            {
                super.navigateTo (navigationState);
            }
            catch (Exception e)
            {
                String html_message =
                    "<h2>An unknown form of exception was found navigating to:<br>" +
                        "<b>" + navigationState + "</b>" +
                    "</h2>";

                damage_report (html_message, e);
            }
        }
    }

    class ErrorView extends VerticalLayout implements View
    {
        private String err_message;
        private String err_details;

        public ErrorView ()
        {
            addStyleName ("damage-report");
            setMargin (true);
        }

        public void setMessage (String html_message, String text_details)
        {
            err_message = html_message;
            err_details = text_details;
        }

        public void setMessage (String html_message, Exception e)
        {
            StringWriter errors = new StringWriter ();
            e.printStackTrace(new PrintWriter (errors));
            setMessage (html_message, errors.toString ());
        }

        @Override
        public void enter (ViewChangeListener.ViewChangeEvent viewChangeEvent)
        {
            removeAllComponents ();
            addComponent (new Label ("<h1>Damage Report<h1>" + err_message, ContentMode.HTML));
            addComponent (new Label (err_details, ContentMode.PREFORMATTED));
        }
    }

    private void damage_report (String html_message, String text_details)
    {
        damage_report_view.setMessage (html_message, text_details);
        navigator.navigateTo (DAMN);
    }

    private void damage_report (String html_message, Exception e)
    {
        damage_report_view.setMessage (html_message, e);
        navigator.navigateTo (DAMN);
    }
}

// EOF
