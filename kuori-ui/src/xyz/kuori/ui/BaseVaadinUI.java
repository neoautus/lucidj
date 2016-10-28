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

package xyz.kuori.ui;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;
import java.util.logging.Logger;

import com.google.common.base.Throwables;
import com.vaadin.annotations.*;
import com.vaadin.navigator.Navigator;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.navigator.ViewProvider;
import com.vaadin.server.*;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.*;
import com.vaadin.ui.themes.ValoTheme;
import org.vaadin.teemu.VaadinIcons;

import library.shiro.Shiro;
import org.apache.felix.ipojo.*;
import org.apache.felix.ipojo.architecture.PropertyDescription;
import org.apache.felix.ipojo.annotations.*;
import org.apache.felix.ipojo.annotations.Component;

@Component(immediate = true)
@Instantiate
@Provides(specifications = UI.class)
@Theme("kuori")
//@Title("NEOautus Rationalℚ")
@Widgetset("xyz.kuori.CustomWidgetSet")
@Push
@PreserveOnRefresh
public class BaseVaadinUI extends UI implements ViewProvider
{
    private static final Logger log = Logger.getLogger ("VaadinUI");

    //  Layout structure:
    //  +---------------------------------------------------------+
    //  | rootLayout                                              |
    //  | +------------------+----------------------------------+ |
    //  | | menuArea         | contentArea                      | |
    //  | | +--------------+ |                                  | |
    //  | | | menuHeader   | |                                  | |
    //  | | |              | |                                  | |
    //  | | |              | |                                  | |
    //  | | | +----------+ | |                                  | |
    //  | | | |navMenu   | | |                                  | |
    //  | | | |          | | |                                  | |
    //  | | | |          | | |                                  | |
    //  | | | |          | | |                                  | |
    //  | | | |          | | |                                  | |
    //  | | | +----------+ | |                                  | |
    //  | | +--------------+ |                                  | |
    //  | +------------------+----------------------------------+ |
    //  +---------------------------------------------------------+

    private HorizontalLayout rootLayout = new HorizontalLayout();
    private VerticalLayout viewArea = new VerticalLayout();
    private HorizontalLayout viewCaption = new HorizontalLayout();
    private Label viewTitle;
    private HorizontalLayout toolbarArea = new HorizontalLayout();
    private VerticalLayout contentArea = new VerticalLayout();
    private CssLayout menuArea = new CssLayout();
    private CssLayout menuHeader = new CssLayout();
    private CssLayout navMenu = new CssLayout();

    private TreeSet<ViewEntry> view_list = new TreeSet<>();
    private Navigator navigator;

    @Requires Shiro shiro;

    //=========================================================================================
    // LAYOUTS
    //=========================================================================================

    private void initBaseLayouts ()
    {
        rootLayout.setSizeFull();
        rootLayout.setWidth("100%");

        menuArea.setPrimaryStyleName("valo-menu");

        viewArea.setPrimaryStyleName("valo-content");
        viewArea.setSizeFull();

        // Placeholder for view caption
        viewCaption.setVisible(false);
        viewArea.addComponent(viewCaption);

        // Placeholder for toolbar
        toolbarArea.setSizeUndefined();
        toolbarArea.setWidth("100%");
        toolbarArea.setVisible(false);
        toolbarArea.addStyleName("toolbar-area");
        viewArea.addComponent(toolbarArea);

        contentArea.setPrimaryStyleName("valo-content");
        contentArea.addStyleName("v-scrollable");
        contentArea.setSizeFull();
        viewArea.addComponent(contentArea);
        viewArea.setExpandRatio(contentArea, 1);

        rootLayout.addComponents(menuArea, viewArea);
        rootLayout.setExpandRatio(viewArea, 1);

        setContent(rootLayout);
        addStyleName (ValoTheme.UI_WITH_MENU);

        menuArea.addComponent(menuHeader);
    }

    private void initMenuHeader ()
    {
        final HorizontalLayout top = new HorizontalLayout();
        top.setWidth("100%");
        top.setDefaultComponentAlignment(Alignment.MIDDLE_LEFT);
        top.addStyleName("valo-menu-title");
        menuHeader.addComponent(top);

        // Hamburger menu
        final Button showMenu = new Button("Menu", new Button.ClickListener()
        {
            @Override
            public void buttonClick(final Button.ClickEvent event)
            {
                if (getStyleName().contains("valo-menu-visible"))
                {
                    removeStyleName("valo-menu-visible");
                }
                else
                {
                    addStyleName("valo-menu-visible");
                }
            }
        });
        showMenu.addStyleName(ValoTheme.BUTTON_PRIMARY);
        showMenu.addStyleName(ValoTheme.BUTTON_SMALL);
        showMenu.addStyleName("valo-menu-toggle");
        showMenu.setIcon(FontAwesome.LIST);
        menuHeader.addComponent(showMenu);

        // Logo + Control Center -- Q: &rationals;
        final Label title = new Label(
                "<h3>NEOautus <strong>Rational&rationals;</strong></h3>", ContentMode.HTML);
        title.setSizeUndefined();
        top.addComponent(title);
        top.setExpandRatio(title, 1);

        // User picture + user menu
        final MenuBar settings = new MenuBar();
        settings.addStyleName("user-menu");

        final MenuBar.MenuItem settingsItem = settings.addItem
        (
            "Willie Coyote",
            new ThemeResource("../neoautus-valo/img/willie-coyote.jpg"),
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

        menuHeader.addComponent (settings);

//        // Internet Exploder hack
//        if (getPage().getWebBrowser().isIE() &&
//            getPage().getWebBrowser().getBrowserMajorVersion() == 9)
//        {
            navMenu.setWidth("280px");
//        }

        navMenu.setPrimaryStyleName("valo-menuitems");
        navMenu.addStyleName("valo-menu-part");
        menuHeader.addComponent(navMenu);
    }

    private void initViewCaption ()
    {
        viewCaption.setDefaultComponentAlignment(Alignment.MIDDLE_CENTER);
        viewCaption.addStyleName("valo-menu-title");
        viewCaption.setWidth(100.0f, Unit.PERCENTAGE);

        Button pinView = new Button();
        pinView.addStyleName("view-control");
        pinView.setIcon(FontAwesome.THUMB_TACK);
        pinView.setSizeUndefined();
        pinView.addClickListener(new Button.ClickListener()
        {
            @Override
            public void buttonClick(Button.ClickEvent clickEvent)
            {
                if (clickEvent.getButton().getIcon().equals(FontAwesome.THUMB_TACK))
                {
                    clickEvent.getButton().setIcon(VaadinIcons.PIN_POST);
                }
                else
                {
                    clickEvent.getButton().setIcon(FontAwesome.THUMB_TACK);
                }
            }
        });
        viewCaption.addComponent(pinView);

        viewTitle = new Label("", ContentMode.HTML);
        viewTitle.addStyleName("view-caption");
        viewTitle.setSizeUndefined();
        viewCaption.addComponent(viewTitle);
        viewCaption.setExpandRatio(viewTitle, 1.0f);

        Button configView = new Button();
        configView.addStyleName("view-control");
        configView.setIcon(FontAwesome.ELLIPSIS_V);
        viewCaption.addComponent(configView);

        viewCaption.setVisible(false);
    }

    protected void initAllLayouts ()
    {
        initBaseLayouts();
        initMenuHeader();
        initViewCaption();
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
                    title += " <span class=\"valo-menu-badge\">" + entry.badge + "</span>";
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

    protected void initNavigator ()
    {
        navigator = new Navigator (this, contentArea);

        navigator.setErrorView (FancyEmptyView.class);

        navigator.addViewChangeListener(new ViewChangeListener()
        {
            @Override
            public boolean beforeViewChange (final ViewChangeEvent event)
            {
                boolean authenticated = shiro.getSubject().isAuthenticated();
                boolean unrestricted_area = event.getViewName().equals ("login");

                log.info ("beforeViewChange: authenticated = " + authenticated);
                log.info ("beforeViewChange: unrestricted_area = " + unrestricted_area);

                if (authenticated != menuArea.isVisible())
                {
                    // Menu is only visible when authenticated
                    menuArea.setVisible(authenticated);
                }

                if (authenticated || unrestricted_area)
                {
                    return (true);
                }

                log.info ("beforeViewChange: going to login");
                navigator.navigateTo("login");
                return (false);
            }

            @Override
            public void afterViewChange (final ViewChangeEvent event)
            {
                String navid = event.getViewName();

                highlight_menu_item(navid);

                String caption = getComponentCaption(navid);

                if (caption != null && !caption.isEmpty())
                {
                    viewTitle.setValue("<h3>" + caption + "</h3>");
                    viewCaption.setVisible(true);
                }
                else
                {
                    viewCaption.setVisible(false);
                }

                AbstractComponent toolbar = getComponentToolbar(navid);

                log.info("Toolbar navid:" + navid + " = " + toolbar);

                // When switching views the toolbar is always detached
                toolbarArea.removeAllComponents();

                if (toolbar != null)
                {
                    // Attach a new toolbar
                    toolbarArea.addComponent(toolbar);
                    toolbarArea.setVisible (true);
                }
                else
                {
                    // Keep toolbar area invisible
                    toolbarArea.setVisible(false);
                }

                //menu.removeStyleName ("valo-menu-visible");
            }
        });

        navigator.addProvider(this);

        // Start on default page
        navigator.navigateTo("");
    }

    private HashMap<String, View> reference_view_map = new HashMap<>();
    private HashMap<String, View> view_map = new HashMap<>();
    private HashMap<String, ComponentInstance> component_map = new HashMap<>();

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

    private String getComponentCaption (String navid)
    {
        return ((String)getComponentProperty(navid, "View-Caption"));
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
        String view_body = get_view_body(navid);

        // We need a reference view registered
        View ref_view = reference_view_map.get(view_head);

        log.info ("getView(): view_head=[" + view_head + "], view_body=[" + view_body +
                  "], ref_view=" + ref_view);

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

                if (view_body != null)
                {
                    // Only defines View-Body if we have one available
                    props.put("View-Body", view_body);
                    log.info ("View-Body: " + view_body);
                }

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
                StringWriter errors = new StringWriter();
                e.printStackTrace(new PrintWriter(errors));
                error_message = "Error creating view: " + navid + "\n" +
                                Throwables.getStackTraceAsString(e);
            }
        }

        if (error_message == null)
        {
            error_message = "Invalid View: " + navid;
        }

        log.info ("getView(): FancyEmptyView");
        return (new FancyEmptyView (error_message));
    }

    //=========================================================================================
    // UI INITIALISATION
    //=========================================================================================

    @Override
    protected void init (VaadinRequest vaadinRequest)
    {
        Responsive.makeResponsive (this);
        setLocale (vaadinRequest.getLocale());

        initAllLayouts ();
        initNavigator();
    }

    //=========================================================================================
    // ASYNC EVENTS HANDLING
    //=========================================================================================

    private void async_menu_update (ViewEntry bind, ViewEntry unbind)
    {
        final UI ui = getUI();

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
                    ui.push();
                }
            });
        }
    }

    private void dump_properties (String label, Map props)
    {
        Iterator it = props.entrySet().iterator();

        log.info(label + " map size = " + props.size() + " object " + Objects.hashCode(props));

        while (it.hasNext())
        {
            Map.Entry property = (Map.Entry)it.next();
            log.info(label + " name=[" + property.getKey() +
                     "] value=[" + property.getValue() + "]");
        }
    }

    private void dump_all_properties (String label)
    {
        label = "" + Objects.hashCode(this) + ": " + label;
        log.info(label + "<reference_view_map> ---------------");
        dump_properties(label + "<reference_view_map>", reference_view_map);
        dump_properties(label + "<view_map>", view_map);
        dump_properties(label + "<component_map>", component_map);
        log.info(label + "<reference_view_map> ---------------");
    }

    @Bind(aggregate=true, optional=true, specification = View.class, filter="(component=*)")
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

    @Override
    public void detach()
    {
        // Normal detach for everybody
        super.detach();

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
            PropertyDescription[] pd = pid.getProperties();

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

    //=========================================================================================
    // FANCY EMPTY VIEW, BECAUSE WHITE IS TOO EMPTY :)
    //=========================================================================================

    private class FancyEmptyView extends CssLayout implements View
    {
        public FancyEmptyView ()
        {
            super ();
            addStyleName ("custom-empty-view");
        }

        public FancyEmptyView (String message)
        {
            super ();
            addStyleName ("custom-empty-view");
            addComponent(new Label (message, ContentMode.PREFORMATTED));
        }

        @Override
        public void enter(ViewChangeListener.ViewChangeEvent event)
        {
            // Nothing to do
        }
    }
}

// EOF
