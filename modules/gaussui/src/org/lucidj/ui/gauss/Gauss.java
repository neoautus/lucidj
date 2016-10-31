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

package org.lucidj.ui.gauss;

import org.lucidj.api.DesktopInterface;
import org.lucidj.api.MenuEntry;
import org.lucidj.api.MenuInstance;
import org.lucidj.api.MenuManager;
import org.lucidj.renderer.treemenu.TreeMenuRenderer;
import org.lucidj.vaadinui.FancyEmptyView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.navigator.Navigator;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.navigator.ViewDisplay;
import com.vaadin.navigator.ViewProvider;
import com.vaadin.server.FontAwesome;
import com.vaadin.server.Resource;
import com.vaadin.server.Sizeable;
import com.vaadin.server.Sizeable.Unit;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.AbstractComponent;
import com.vaadin.ui.Accordion;
import com.vaadin.ui.Button;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.HorizontalSplitPanel;
import com.vaadin.ui.Label;
import com.vaadin.ui.Layout;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.EventObject;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.TreeSet;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.ConfigurationException;
import org.apache.felix.ipojo.Factory;
import org.apache.felix.ipojo.InstanceManager;
import org.apache.felix.ipojo.MissingHandlerException;
import org.apache.felix.ipojo.Pojo;
import org.apache.felix.ipojo.PrimitiveInstanceDescription;
import org.apache.felix.ipojo.UnacceptableConfiguration;
import org.apache.felix.ipojo.annotations.Bind;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Unbind;
import org.apache.felix.ipojo.architecture.PropertyDescription;

@Component (immediate = true)
@Instantiate
@Provides (specifications = DesktopInterface.class)
public class Gauss implements DesktopInterface, ViewProvider, MenuInstance.EventListener
{
    private final static transient Logger log = LoggerFactory.getLogger (Gauss.class);

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

    private VerticalLayout vAppLayout = new VerticalLayout ();
    private HorizontalSplitPanel hsMenuContents = new HorizontalSplitPanel ();
    private HorizontalSplitPanel hsContentsSidebar = new HorizontalSplitPanel ();
    private HorizontalLayout hToolbarArea = new HorizontalLayout();
    private CssLayout hToolbarPlaceholder = new CssLayout ();
    private CssLayout emptySidebar = new FancyEmptyView ("Sidebar empty");
    private CssLayout navMenu = new CssLayout();
    private Accordion acMenu = new Accordion ();
    private int default_sidebar_width_pixels = 250;

    private String DAMN = "damage.report";
    private ErrorView damage_report_view = new ErrorView ();

    private TreeSet<ViewEntry> view_list = new TreeSet<>();

    private HashMap<String, View> reference_view_map = new HashMap<>();
    private HashMap<String, View> view_map = new HashMap<>();
    private HashMap<String, ComponentInstance> component_map = new HashMap<>();

    private UI attached_ui;
    private Navigator navigator;

    @Requires
    private MenuManager menu_manager;
    private MenuInstance main_menu;
    private TreeMenuRenderer main_menu_renderer;

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
            navMenu.setPrimaryStyleName ("valo-menuitems");
            navMenu.addStyleName ("valo-menu-part");
            //navMenu.addStyleName ("valo-menu");
            navMenu.setWidth (default_sidebar_width_pixels, Sizeable.Unit.PIXELS);
            navMenu.setHeightUndefined ();

            main_menu = menu_manager.newMenuInstance (null);
            main_menu.setEventListener (this);
            main_menu_renderer = new TreeMenuRenderer ();
            main_menu_renderer.objectLinked (main_menu);
            main_menu_renderer.objectUpdated ();
            com.vaadin.ui.Component main_menu_component = main_menu_renderer.renderingComponent ();
            main_menu_component.setWidth (100, Unit.PERCENTAGE);
            main_menu_component.setHeightUndefined ();

            acMenu.addStyleName ("borderless");
            acMenu.addTab (main_menu_component, "New Navigation");
            acMenu.addTab (navMenu, "Navigation"); // FontAwesome.COMPASS
            acMenu.setSizeFull ();
        }

        hsContentsSidebar.setFirstComponent (new FancyEmptyView ());
        hsContentsSidebar.setSecondComponent (emptySidebar);
        hsContentsSidebar.setSplitPosition (default_sidebar_width_pixels, Sizeable.Unit.PIXELS, true);
        hsMenuContents.setFirstComponent (acMenu);
        hsMenuContents.setSecondComponent (hsContentsSidebar);
        hsMenuContents.setSplitPosition (default_sidebar_width_pixels, Sizeable.Unit.PIXELS);
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
        home_buttons.setWidth (default_sidebar_width_pixels, Sizeable.Unit.PIXELS);
        home_buttons.setId ("_home_buttons");

        final Button toggle_menu = new Button ();
        toggle_menu.setIcon (FontAwesome.BARS);
        toggle_menu.addStyleName("tiny");
        toggle_menu.addStyleName ("quiet");
        toggle_menu.addStyleName("ui-toolbar-spacer");
        toggle_menu.setId("_toggle_menu");
        home_buttons.addComponent (toggle_menu);
        toggle_menu.addClickListener (new Button.ClickListener ()
        {
            @Override
            public void buttonClick (Button.ClickEvent clickEvent)
            {
                if (acMenu.isVisible ()) // or (hsMenuContents.getSplitPosition () != 0)
                {
                    acMenu.setVisible (false);
                    hsMenuContents.setSplitPosition (0, Sizeable.Unit.PIXELS);
                    hsMenuContents.setLocked (true);
                }
                else
                {
                    acMenu.setVisible (true);
                    hsMenuContents.setSplitPosition (default_sidebar_width_pixels, Unit.PIXELS);
                    hsMenuContents.setLocked (false);
                }
            }
        });

        final Button home = new Button ("Home");
        home.setIcon (FontAwesome.HOME);
        home.addStyleName("tiny");
        home.addStyleName("ui-toolbar-spacer");
        home.setId("_home");
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
        compose.addStyleName("tiny");
        compose.addStyleName("primary");
        compose.setId("_new");
        home_buttons.addComponent (compose);
        compose.addClickListener (new Button.ClickListener ()
        {
            @Override
            public void buttonClick (Button.ClickEvent clickEvent)
            {
            }
        });

        hToolbarArea.addComponent (home_buttons);

        hToolbarPlaceholder = new CssLayout ();
        hToolbarPlaceholder.setSizeFull ();
        hToolbarArea.addComponent (hToolbarPlaceholder);
        hToolbarArea.setExpandRatio (hToolbarPlaceholder, 1.0f);

        final Button eject_view = new Button ();
        eject_view.setIcon (FontAwesome.EXTERNAL_LINK);
        eject_view.addStyleName("tiny");
        eject_view.addStyleName ("quiet");
        //eject_view.addStyleName("ui-toolbar-spacer");
        eject_view.setId("_eject_view");
        hToolbarArea.addComponent (eject_view);
        eject_view.addClickListener (new Button.ClickListener ()
        {
            @Override
            public void buttonClick (Button.ClickEvent clickEvent)
            {
                if (hsContentsSidebar.isLocked ())
                {
                    hsContentsSidebar.setSplitPosition (default_sidebar_width_pixels, Unit.PIXELS, true);
                    hsContentsSidebar.setLocked (false);
                }
                else
                {
                    hsContentsSidebar.setSplitPosition (0, Unit.PIXELS, true);
                    hsContentsSidebar.setLocked (true);
                }
            }
        });

        final Button toggle_sidebar = new Button ();
        toggle_sidebar.setIcon (FontAwesome.BARS);
        toggle_sidebar.addStyleName("tiny");
        toggle_sidebar.addStyleName ("quiet");
        toggle_sidebar.addStyleName("ui-toolbar-spacer");
        toggle_sidebar.setId("_toggle_sidebar");
        hToolbarArea.addComponent (toggle_sidebar);
        toggle_sidebar.addClickListener (new Button.ClickListener ()
        {
            @Override
            public void buttonClick (Button.ClickEvent clickEvent)
            {
                if (hsContentsSidebar.isLocked ())
                {
                    hsContentsSidebar.setSplitPosition (default_sidebar_width_pixels, Unit.PIXELS, true);
                    hsContentsSidebar.setLocked (false);
                }
                else
                {
                    hsContentsSidebar.setSplitPosition (0, Unit.PIXELS, true);
                    hsContentsSidebar.setLocked (true);
                }
            }
        });

    }

    protected void initAllLayouts ()
    {
        initAppLayout ();
        updateNavMenu();
    }

    //=========================================================================================
    // NAVIGATION MENU RENDERING
    //=========================================================================================

    protected void updateNavMenu ()
    {
        int layout_index = 0;

        for (ViewEntry entry: view_list)
        {
            if (!entry.visible)
            {
                continue;
            }

            AbstractComponent layout_component = null;
            ViewEntry layout_entry = null;

            // Get the current component on CssLayout
            if (layout_index < navMenu.getComponentCount())
            {
                layout_component = (AbstractComponent) navMenu.getComponent(layout_index);
                layout_entry = (ViewEntry)layout_component.getData();
            }

            // Should we insert a new menu item?
            if (layout_entry == null || entry.compareTo(layout_entry) < 0)
            {
                String title = entry.title;

                if (!entry.badge.isEmpty())
                {
                    //title += " <span class=\"valo-menu-badge\">" + entry.badge + "</span>";
                }

                final Button item = new Button(title, new Button.ClickListener()
                {
                    @Override
                    public void buttonClick(Button.ClickEvent clickEvent)
                    {
                        ViewEntry menu_entry = (ViewEntry)clickEvent.getButton().getData();
                        log.info ("navigateTo: " + menu_entry);
                        navigator.navigateTo(menu_entry.navid);
                    }
                });

                item.setHtmlContentAllowed(true);
                item.setData(entry);

                if (entry.options.contains(":header:"))
                {
                    item.setPrimaryStyleName("valo-menu-item");
                    item.addStyleName("custom-menu-subtitle");
                }
                else
                {
                    item.setPrimaryStyleName("valo-menu-item");
                    item.setIcon (entry.icon);
                }

                // Insert at current position into layout
                navMenu.addComponent(item, layout_index);
                entry.menu_item = item;
            }
            else if (entry.compareTo(layout_entry) > 0)
            {
                // The component is unwanted, remove it
                navMenu.removeComponent (layout_component);
            }

            layout_index++;
        }

        // Remove remaining components at the end of layout
        while (layout_index < navMenu.getComponentCount())
        {
            navMenu.removeComponent (navMenu.getComponent(layout_index));
        }
    }

    protected String get_view_head (String navid)
    {
        if (navid.contains(":"))
        {
            // Extracts view head from 'viewhead[/viewbody]'
            return (navid.substring(0, navid.indexOf(':')));
        }

        return (navid);
    }

    protected String get_view_body (String navid)
    {
        if (navid.contains(":") && navid.indexOf(':') != navid.length() - 1)
        {
            return (navid.substring(navid.indexOf(':') + 1));
        }

        return (null);
    }

    protected void highlight_menu_item (String navid)
    {
        AbstractComponent selected_item = null;

        for (int i = 0; i < navMenu.getComponentCount(); i++)
        {
            AbstractComponent layout_item = (AbstractComponent) navMenu.getComponent(i);
            layout_item.removeStyleName("selected");

            ViewEntry menu_entry = (ViewEntry)layout_item.getData();

            if (menu_entry != null)
            {
                if (navid.equals(menu_entry.navid))
                {
                    // Always match full navid
                    selected_item = layout_item;
                }
                else if (selected_item == null && get_view_head(navid).equals(menu_entry.navid))
                {
                    // Match viewhead only if nothing else selected
                    selected_item = layout_item;
                }
            }
        }

        if (selected_item != null)
        {
            selected_item.addStyleName("selected");
        }
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
                    hsContentsSidebar.setFirstComponent ((com.vaadin.ui.Component)view);
                }
                else
                {
                    String msg = "Invalid component:\n" + view.getClass ().getCanonicalName ();
                    hsContentsSidebar.setFirstComponent (new FancyEmptyView (msg));
                }
            }
        });

        navigator.setErrorView (FancyEmptyView.class);
        navigator.addView (DAMN, damage_report_view);

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

                highlight_menu_item(navid);

                AbstractComponent sidebar = getComponentSidebar (navid);

                log.info ("*** Sidebar navid:{} = {}", navid, sidebar);

                if (sidebar != null)
                {
                    log.info ("Setting sidebar: {}", sidebar);
                    hsContentsSidebar.setSecondComponent (sidebar);

                    // Sidebar visible at default position
                    hsContentsSidebar.setSplitPosition (default_sidebar_width_pixels, Unit.PIXELS, true);
                    hsContentsSidebar.setLocked (false);
                }
                else
                {
                    // No contents
                    hsContentsSidebar.setSecondComponent (emptySidebar);

                    // Sidebar is hidden
                    hsContentsSidebar.setSplitPosition (0, Unit.PIXELS, true);
                    hsContentsSidebar.setLocked (true);
                }

                AbstractComponent toolbar = getComponentToolbar(navid);

                log.info("Toolbar navid:{} = {}", navid, toolbar);

                hToolbarPlaceholder.removeAllComponents();

                if (toolbar != null)
                {
                    // Attach a new toolbar
                    hToolbarPlaceholder.addComponent(toolbar);
                }

                //menu.removeStyleName ("valo-menu-visible");
            }
        });

        navigator.addProvider(this);

        // Start on default Home
        navigator.navigateTo ("home");
    }

    private Object getComponentProperty (String navid, String property)
    {
        ComponentInstance ci = component_map.get(navid);
        Object value = null;

        if (ci != null)
        {
            PrimitiveInstanceDescription pid = (PrimitiveInstanceDescription)ci.getInstanceDescription();
            PropertyDescription[] pd = pid.getProperties();

            for (PropertyDescription p: pd)
            {
                if (p.getName ().equals (property))
                {
                    value = p.getCurrentValue();
                    break;
                }
            }
        }

        return (value);
    }

    private AbstractComponent getComponentToolbar (String navid)
    {
        Object toolbar = getComponentProperty (navid, "View-Toolbar");

        if (toolbar instanceof AbstractComponent)
        {
            return ((AbstractComponent)toolbar);
        }

        return (null);
    }

    private AbstractComponent getComponentSidebar (String navid)
    {
        Object sidebar = getComponentProperty (navid, "View-Sidebar");

        if (sidebar instanceof AbstractComponent)
        {
            return ((AbstractComponent)sidebar);
        }

        return (null);
    }

    @Override
    public String getViewName(String view_spec)
    {
        if (view_spec.contains("/"))
        {
            log.info ("getViewName(): split [" + view_spec + "]");

            // Extracts view head from 'viewhead[;viewbody]/parameters/...'
            return (view_spec.substring(0, view_spec.indexOf('/')));
        }

        // Empty view is shortcut for default view
        if (view_spec.isEmpty() && !view_list.isEmpty())
        {
            view_spec = view_list.first().navid;
        }

        log.info ("getViewName(): full [" + view_spec + "]");
        return (view_spec);
    }

    public View getView(String navid)
    {
        log.info ("getView(): [" + navid + "]");

        if (view_map.get(navid) != null)
        {
            log.info("getView(): CACHE HIT:" + view_map.get(navid));

            // Returns view from cache
            return (view_map.get(navid));
        }

        String view_head = get_view_head(navid);

        // We need a reference view registered
        View ref_view = reference_view_map.get(view_head);

        String error_message = null;

        if (ref_view != null)
        {
            //===================================================================
            // SOME MAGIC: Create new View object backed on a ComponentInstance
            //             and returns it. These instances will be disposed when
            //             the UI get detached().
            //===================================================================
            Factory factory = ((Pojo)ref_view).getComponentInstance ().getFactory();

            try
            {
                Properties props = new Properties();

                // TODO: MOVE ALL THIS INTO TaskContext
                // Create a new instance...
                ComponentInstance new_comp = factory.createComponentInstance (props);
                component_map.put(navid, new_comp);

                String[] status = "-1/DISPOSED,0/STOPPED,1/INVALID,2/VALID".split("\\,");
                log.info ("@@@@@ is_valid => " + status [new_comp.getState() + 1]);

                if (new_comp.getState() == ComponentInstance.VALID)
                {
                    // ...and returns Pojo object from it
                    View new_view = (View) ((InstanceManager) new_comp).getPojoObject();
                    view_map.put(navid, new_view);

                    if (new_view instanceof com.vaadin.ui.AbstractComponent)
                    {
                        com.vaadin.ui.AbstractComponent c =
                                (com.vaadin.ui.AbstractComponent)new_view;

                        try
                        {
                            c.addListener(EventObject.class, this,
                                    this.getClass().getMethod("viewEvent", EventObject.class));
                        }
                        catch (Exception ignore) {};
                    }

                    log.info("getView(): NEW = " + new_view);
                    return (new_view);
                }
            }
            catch (UnacceptableConfiguration | MissingHandlerException | ConfigurationException e)
            {
                error_message =
                    "<h2>A strange unknown energy appeared when creating view: <b>" + view_head + "</b><br>" +
                        "Target: <b>" + navid + "</b>" +
                    "</h2>";

                damage_report_view.setMessage (error_message, e);
                return (damage_report_view);
            }
        }

        // The view just doesn't exists
        return (new FancyEmptyView ("View not found: " + navid));
    }


    //=========================================================================================
    // ASYNC EVENTS HANDLING
    //=========================================================================================

    private void async_menu_update (final ViewEntry bind, final ViewEntry unbind)
    {
        final UI ui = attached_ui;

        // TODO: UPDATE EVEN IF NOT ATTACHED, ONLY AVOID THE ASYNC ui.access()
        if (ui != null && ui.isAttached())
        {
            //=========================================================================
            // SOME MAGIC: Update the UI in background, the way Vaadin recommends:
            // https://vaadin.com/book/-/page/advanced.push.html#advanced.push.running
            //=========================================================================
            ui.access (new Runnable()
            {
                @Override
                public void run()
                {
                    // Check if we're losing the current view
                    if (navigator != null && unbind != null &&
                        get_view_head(navigator.getState()).equals (unbind.navid))
                    {
                        navigator.navigateTo("");
                    }

                    updateNavMenu();

                    // Check if we're gaining a fresh view
                    if (navigator != null && bind != null &&
                        navigator.getState().isEmpty())
                    {
                        navigator.navigateTo("");
                    }

                    // TODO: See why markAsDirtyRecursive() is needed (menu=ok,content=nok)
                    ui.markAsDirtyRecursive();
                }
            });
        }
    }

    private void dump_properties (String label, Map props)
    {
        Iterator it = props.entrySet().iterator();

        log.debug (label + " map size = " + props.size() + " object " + Objects.hashCode(props));

        while (it.hasNext())
        {
            Map.Entry property = (Map.Entry)it.next();
            log.debug (label + " name=[" + property.getKey() +
                    "] value=[" + property.getValue() + "]");
        }
    }

    private void dump_all_properties (String label)
    {
        label = "" + Objects.hashCode(this) + ": " + label;
        log.debug (label + "<reference_view_map> ---------------");
        dump_properties(label + "<reference_view_map>", reference_view_map);
        dump_properties(label + "<view_map>", view_map);
        dump_properties(label + "<component_map>", component_map);
        log.debug (label + "<reference_view_map> ---------------");
    }

    @Bind (aggregate=true, optional=true, specification = View.class, filter="(component=*)")
    private void bindView (View v)
    {
        log.info ("bindView: Adding " + v);

        dump_all_properties("IN bindView");

        ViewEntry entry = new ViewEntry(v);

        log.info ("bindView: entry=" + entry.toString ());

        log.info ("bindView: reference_view_map.size() = " + reference_view_map.size() + " *object " + Objects.hashCode(reference_view_map));

        // Stores the view as reference object
        reference_view_map.put(entry.getNavigationId(), v);

        view_list.add(entry);

        log.info("bindView: view_list.size() = " + view_list.size());

        boolean do_async_update = entry.visible;

        log.info("bindView: init_data = " + entry.init_data);

        if (entry.init_data != null)
        {
            for (HashMap data: entry.init_data)
            {
                if (data.containsKey("navid"))
                {
                    ViewEntry new_entry = new ViewEntry(data);
                    view_list.add(new_entry);
                    do_async_update |= new_entry.visible;
                }
            }
        }

        if (do_async_update)
        {
            async_menu_update(entry, null);
        }

        dump_all_properties("OUT bindView");

        log.info("reference_view_map1 = " + Objects.hash(reference_view_map));
        log.info("reference_view_map2 = " + Objects.hashCode(reference_view_map));
        if (reference_view_map == null)
        {
            log.info("reference_view_map NULL");
        }
        dump_properties("reference_view_map", reference_view_map);
    }

    @Unbind
    private void unbindView (View v)
    {
        log.info ("unbindView: " + v);

        dump_all_properties("IN UNbindView");

        String navid = null;

        for (Map.Entry<String, View> entry: reference_view_map.entrySet())
        {
            if (entry.getValue() == v)
            {
                log.info("***> Removing " + entry.getKey() + ": " + entry.getValue());
                navid = entry.getKey();
                break;
            }
        }

        if (navid != null)
        {
            reference_view_map.remove(navid);
            component_map.remove(navid);

            Iterator<Map.Entry<String, View>> itr = view_map.entrySet().iterator();

            while(itr.hasNext())
            {
                Map.Entry<String, View> entry = itr.next();

                log.info("view_head=" + get_view_head(entry.getKey()) + ", navid=" + navid);

                if (get_view_head(entry.getKey()).equals (navid))
                {
                    // TODO: VERIFICAR SE É O VIEW ATUAL E SE FOR REMOVER
                    component_map.remove(entry.getKey());
                    itr.remove();
                }
            }

            for (ViewEntry entry: view_list)
            {
                if (navid.equals(entry.getNavigationId()))
                {
                    log.info("unbindView: Found " + entry.toString());
                    view_list.remove(entry);
                    async_menu_update(null, entry);
                    break;
                }
            }
        }

        dump_all_properties("OUT UNbindView");
    }

    public void viewEvent (EventObject event_data)
    {
        log.info("@@@LISTENER@@@ event_data = " + event_data.getSource());

        if (event_data.getSource() instanceof HashMap)
        {
            log.info("@@@LISTENER@@@ HashMap = " + event_data.getSource());

            ViewEntry new_entry = new ViewEntry ((HashMap)event_data.getSource());

            view_list.add(new_entry);

            if (new_entry.visible)
            {
                async_menu_update(new_entry, null);
            }
        }
    }

    @Override // DesktopInterface
    public void init (UI ui)
    {
        attached_ui = ui;
        initAllLayouts ();
        initNavigator (ui);
    }

    @Override // DesktopInterface
    public Layout getMainLayout ()
    {
        return (vAppLayout);
    }

    @Override // DesktopInterface
    public void attach ()
    {
        // Nothing
    }

    @Override // DesktopInterface
    public void detach()
    {
        log.info("detach() " + this);

        dump_all_properties("IN detach");

        //================================================================
        // HALF-MAGIC: Since we know for sure that UI is almost gone, now
        //             it's safe to dispose all the ComponentInstance(s)
        //             that we instantiated on getView().
        //================================================================
        for (Map.Entry<String, ComponentInstance> entry: component_map.entrySet())
        {
            if (entry.getValue() != null)
            {
                log.info("***> Disposing " + entry.getKey() + ": " + entry.getValue());
                entry.getValue().dispose();
            }
        }

        dump_all_properties("OUT detach");
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

    //=========================================================================================
    // INTERNAL MENU-SUPPORT CLASS
    //=========================================================================================

    private class ViewEntry implements Comparable<ViewEntry>
    {
        String title;
        int weight;
        Resource icon;
        String navid;
        String options;
        String badge;
        boolean visible;
        View ref_view;
        List<HashMap> init_data;
        AbstractComponent menu_item;

        public void parseEntryProperties (Map properties)
        {
            Iterator it = properties.entrySet().iterator();

            while (it.hasNext())
            {
                Map.Entry p = (Map.Entry)it.next();
                String clazz = "";

                if (p.getValue() != null)
                {
                    clazz = " (" + p.getValue().getClass().getName() + ")";
                }

                log.info ("===>> " + p.getKey() + " = " + p.getValue() + clazz);

                switch ((String)p.getKey())
                {
                    case "title":     title   = (String)p.getValue(); break;
                    case "weight":    weight  = (Integer)p.getValue(); break;
                    case "icon":      icon    = (Resource)p.getValue(); break;
                    case "navid":     navid   = (String)p.getValue(); break;
                    case "options":   options = ":" + p.getValue() + ":"; break;
                    case "badge":     badge   = (String)p.getValue(); break;
                    case "visible":   visible = (Boolean)p.getValue(); break;
                    case "Init-Data": init_data = (List<HashMap>)p.getValue(); break;
                }
            }

            dump_properties("ViewEntry", properties);
        }

        public ViewEntry(Map properties)
        {
            // Menu item instance from Vaadin
            menu_item = null;

            // Defaults for View init
            ref_view = null;

            // Defaults for menu item
            title = "Untitled";
            navid = "undefined";
            weight = 500;
            icon = FontAwesome.SQUARE_O;
            options = "";
            badge = "";
            visible = true;

            parseEntryProperties(properties);

            log.info ("ViewEntry(Map): Created " + toString());
        }

        public ViewEntry(View ref_view)
        {
            // Menu item instance from Vaadin
            menu_item = null;

            // Defaults for View init
            this.ref_view = ref_view;

            // Defaults for menu item
            title = ref_view.getClass().getName();
            navid = title.replace('.', '-');
            weight = 500;
            icon = FontAwesome.SLIDERS;
            options = "";
            badge = "";
            visible = true;

            //=================================================================
            // MORE MAGIC: Extract the properties from the iPojo services.
            //             All you need to do is insert @Property on the
            //             variables you want to map from inside your service.
            //=================================================================
            ComponentInstance ci = ((Pojo)ref_view).getComponentInstance();
            PrimitiveInstanceDescription pid = ((PrimitiveInstanceDescription)ci.getInstanceDescription());
            final PropertyDescription[] pd = pid.getProperties();

            HashMap properties = new HashMap()
            {{
                for (PropertyDescription p: pd)
                {
                    put(p.getName(), p.getCurrentValue());
                }
            }};

            parseEntryProperties(properties);

            log.info ("ViewEntry(View): Created " + toString());
        }

        public String getNavigationId ()
        {
            return (navid);
        }

        public String toString ()
        {
            return (String.format ("%05d:%s:", weight, title));
        }

        public int compareTo (ViewEntry o)
        {
            return (toString().compareTo(o.toString()));
        }
    }
}

// EOF
