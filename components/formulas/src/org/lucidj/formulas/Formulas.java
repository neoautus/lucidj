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

import com.vaadin.event.FieldEvents;
import com.vaadin.event.LayoutEvents;
import com.vaadin.event.ShortcutAction;
import com.vaadin.event.ShortcutListener;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.server.*;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.*;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;

import org.apache.felix.ipojo.ConfigurationException;
import org.apache.felix.ipojo.annotations.Context;
import org.apache.felix.ipojo.annotations.Controller;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Property;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;

import com.vaadin.annotations.StyleSheet;
import org.osgi.framework.BundleContext;

import org.lucidj.task.CompositeTask;
import org.lucidj.task.TaskContext;
import org.lucidj.system.SystemAPI;
import org.rationalq.editor.ComponentState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@StyleSheet ("vaadin://formulas/styles.css")
@org.apache.felix.ipojo.annotations.Component
@Instantiate
@Provides (specifications = com.vaadin.navigator.View.class)
public class Formulas extends VerticalLayout implements View
{
    //==============================
    //==============================
    // THIS IS A TaskContext VIEWER
    //==============================
    //==============================

    @Property public String title = "Formulas";
    @Property public int weight = 500;
    @Property public Resource icon = FontAwesome.FILE_CODE_O;
    @Property private String navid = "formulas";
    @Property private String options = "header";
    @Property private String badge = "7";

    @Context transient BundleContext ctx;

    private final transient static Logger log = LoggerFactory.getLogger (Formulas.class);

    private static final String VM_NOTEBOOK = "view-mode-notebook";
    private static final String VM_SINGLE = "view-mode-single";
    private static final String VM_ZOOM_CODE = "view-mode-zoom-code";
    private static final String VM_ZOOM_EXEC = "view-mode-zoom-exec";

    private static final String PROP_FORMULAE_VERSION = "Formulae-Version";

    @Property(name="Init-Data")
    private List<HashMap> init_data = new LinkedList<> ();

    @Property(name="View-Body")
    private String formula_name;

    @Property(name="View-Caption")
    private String caption = "Formulas";

    @Property(name="View-Toolbar")
    private CssLayout current_toolbar = null;

    @Property(name="View-Sidebar")
    private Accordion acSidebar = null;
    private ComponentPalette sidebar = null;

    @Requires SystemAPI sapi;

    private TaskContext tctx;

    private long last_save = 0;
    private boolean formulae_changed = false;

    private CompositeTask object_list = null;
    private Object current_object;
    private Map<Object, Cell> active_cells = new ConcurrentHashMap<> ();

    private VerticalLayout content;
    private Cell insert_here_cell;

    public Formulas () throws ConfigurationException
    {
        log.info ("sapi = {}", sapi);
    }

    class Cell extends AbstractCell
    {
        public Cell (BundleContext ctx, Object object)
        {
            // Cell formatting, decoration and event handling is located at AbstractCell
            super (ctx, object);
        }

        @Override
        public Object insertNewObjectBefore (String obj_canonical_name, Object ref_obj)
        {
            Object new_object = insert_new_cell (obj_canonical_name, object_list.indexOf (ref_obj));
            update_cell_focus (new_object);
            return (new_object);
        }

        @Override
        public Object insertNewObjectAfter (String obj_canonical_name, Object ref_obj)
        {
            Object new_object = insert_new_cell (obj_canonical_name, object_list.indexOf (ref_obj) + 1);
            update_cell_focus (new_object);
            return (new_object);
        }

        @Override
        public void moveObjectBefore (Object source_object, Object target_object)
        {
            object_list.remove (source_object);
            object_list.add (object_list.indexOf (target_object), source_object);
            synchronize_cell_view ();
            update_cell_focus (source_object);
        }

        @Override
        public void moveObjectAfter (Object source_object, Object target_object)
        {
            object_list.remove (source_object);
            object_list.add (object_list.indexOf (target_object) + 1, source_object);
            synchronize_cell_view ();
            update_cell_focus (source_object);
        }

        @Override
        public void taskStateClick (int task_state)
        {
            if (getSourceObject () instanceof ComponentState)
            {
                ComponentState cs = (ComponentState)getSourceObject ();

                if (task_state == ComponentState.RUNNING)
                {
                    cs.signal (ComponentState.SIGTERM);
                }
                else // TODO: Check more states??
                {
                    cs.signal (ComponentState.SIGSTART);
                }
            }
        }

        @Override
        public void layoutClick (Component component)
        {
            update_cell_focus (getSourceObject ());
        }

        @Override
        public void layoutDoubleClick (Component component)
        {
            // Nothing for now
        }

        @Override
        public void setToolbar (AbstractLayout toolbar)
        {
            // Remove any existing toolbar
            for (Component c: current_toolbar)
            {
                if (Cell.class.getCanonicalName ().equals (c.getId ()))
                {
                    current_toolbar.removeComponent (c);
                    break;
                }
            }

            // Add new toolbar, if we have one
            if (toolbar != null)
            {
                toolbar.setId (Cell.class.getCanonicalName ());
                current_toolbar.addComponent (toolbar);
            }
        }
    }

    private void update_cell_focus (Object focus_object)
    {
        // Set focus to null selects the first object
        if (focus_object == null && object_list.size () > 0)
        {
            current_object = object_list.get (0);
        }
        else
        {
            current_object = focus_object;
        }

        for (Object object_ref: object_list)
        {
            Cell cell = active_cells.get (object_ref);

            if (object_ref == current_object)
            {
                cell.setFocus ();
            }
            else
            {
                cell.removeFocus ();
            }
        }
    }

    private void synchronize_cell_view ()
    {
        log.info ("synchronize_cell_view: START");

        //------------------------------------------------
        // Create, insert and move active cells on layout
        //------------------------------------------------
        int cell_index = 0;

        log.info ("synchronize_cell_view: will create/insert/move {} cells", object_list.size ());

        for (cell_index = 0; cell_index < object_list.size (); cell_index++)
        {
            Object source_object = object_list.get (cell_index);
            Cell cell = active_cells.get (source_object);

            log.info ("synchronize_cell_view: active_cells source_object={} cell={}", source_object, cell);

            if (cell == null)
            {
                cell = new Cell (ctx, source_object);
                active_cells.put (source_object, cell);
                log.info ("synchronize_cell_view: NEW cell source_object={} cell={}", source_object, cell);
            }

            log.info ("synchronize_cell_view: content[{}]={} decorated={}",
                cell_index,
                cell_index < content.getComponentCount ()? content.getComponent (cell_index): null,
                cell.getDecoratedCell ());

            if (cell_index >= content.getComponentCount () ||
                content.getComponent (cell_index) != cell.getDecoratedCell ())
            {
                log.info ("synchronize_cell_view: addComponent {} into #{}", cell.getDecoratedCell (), cell_index);
                content.addComponent (cell.getDecoratedCell (), cell_index);
            }
        }

        //--------------------------------
        // Remove extra cells from layout
        //--------------------------------
        while (content.getComponentCount () > object_list.size ())
        {
            log.info ("synchronize_cell_view: DELETE content.count={} object_list.size={}", content.getComponentCount (), object_list.size ());
            content.removeComponent (content.getComponent (object_list.size ()));
        }

        //-----------------------------------
        // Delete unused cells from cell map
        //-----------------------------------
        for (Object object_ref: active_cells.keySet ())
        {
            Cell cell = active_cells.get (object_ref);

            log.info ("synchronize_cell_view: ref={} cell={} decorated={} parent={}",
                object_ref, cell, cell.getDecoratedCell (), cell.getDecoratedCell ().getParent ());

            if (cell.getDecoratedCell ().getParent () == null)
            {
                log.info ("synchronize_cell_view: REMOVE ref={} cell={}", object_ref, cell);
                active_cells.remove (object_ref);
            }
        }

        //------------------------------------------
        // No cells? Leave an "insert here" message
        //------------------------------------------
        if (object_list.size () == 0)
        {
            content.addComponent (insert_here_cell.getDecoratedCell ());
        }
        else
        {
            content.removeComponent (insert_here_cell.getDecoratedCell ());
        }

        log.info ("synchronize_cell_view: COMPLETE");
    }

    private Object insert_new_cell (String canonical_name, int index)
    {
        Object new_object = insert_new_object (canonical_name, index);
        synchronize_cell_view ();
        return (new_object);
    }

    private Object insert_new_object (String canonical_name, int index)
    {
        log.info ("insert_new_object: canonical_name={} index={}", canonical_name, index);

        // TODO: ADD DEFAULT BASIC CELL, WHICH CAN CHANGE WITH PLUGINS
        Object new_object = null;

        try
        {
            tctx.bindThread ();
            Class cls = tctx.getClassLoader ().loadClass (canonical_name);
            new_object = cls.newInstance ();
        }
        catch (Exception e)
        {
            // TODO: ERROR NOTIFICATION
            log.error ("insert_new_object: Error creating {}", canonical_name, e);
        }

        log.info ("*** insert_new_object: new_object={}", new_object);

        if (index == -1)
        {
            object_list.add (new_object);
        }
        else
        {
            object_list.add (index, new_object);
        }

        return (new_object);
    }

    //-----------------------------------------------------------------------------------------------------------------
    // OLD METHODS....

    private int get_current_cell_index ()
    {
        if (current_object != null)
        {
            return (object_list.indexOf (current_object));
        }

        return (-1);
    }

    private void set_current_cell_index (int index)
    {
        if (index >= 0 && index < object_list.size())
        {
            current_object = object_list.get (index);
            update_cell_focus (current_object);
        }
    }

    private void handle_button_click (Button source)
    {
        switch (source.getId())
        {
            case "save":
            {
                save_formulae (formula_name);
                break;
            }
            case VM_NOTEBOOK:
            {
                UI.getCurrent().getNavigator().navigateTo(navid + ":" +
                                                          formula_name + "/" +
                                                          VM_NOTEBOOK);
                break;
            }
            case VM_SINGLE:
            {
                UI.getCurrent().getNavigator().navigateTo(navid + ":" +
                                                          formula_name + "/" +
                                                          VM_SINGLE + "/" +
                                                          get_current_cell_index());
                break;
            }
            case "prev-smartbox":
            {
                set_current_cell_index(get_current_cell_index() - 1);
                break;
            }
            case "next-smartbox":
            {
                set_current_cell_index(get_current_cell_index() + 1);
                break;
            }
            case "delete-cell":
            {
                int cell_index = get_current_cell_index ();

                if (cell_index != -1)
                {
                    log.info ("cell_index = {}", cell_index);
                    object_list.remove (cell_index);
                    synchronize_cell_view ();

                    if (cell_index >= object_list.size ())
                    {
                        cell_index = object_list.size () - 1;
                    }

                    set_current_cell_index (cell_index);
                    formulae_changed = true;
                }
                break;
            }
            case "output":
            {
                // TODO: FROM TOOLBAR
//                String current_view_mode = current_cell.getViewMode();
//                formulae_changed = true;
//
//                if (current_view_mode.equals ("default"))
//                {
//                    current_cell.setViewMode ("canvas-only");
//                }
//                else
//                {
//                    current_cell.setViewMode ("default");
//                }
                break;
            }
            default:
            {
                Notification.show("Not implemented", source.getId(),
                    Notification.Type.HUMANIZED_MESSAGE);
                break;
            }
        }
    }

    private Button createButton (Layout parent, String id, Resource icon, String caption, int kc, int mk)
    {
        final Button new_button = new Button ();

        if (caption != null)
        {
            new_button.setCaption (caption);
        }
        new_button.setIcon (icon);
        new_button.addStyleName("tiny");
        new_button.addStyleName("toolbar");
        new_button.setId(id);

        new_button.addClickListener(new Button.ClickListener()
        {
            @Override
            public void buttonClick(Button.ClickEvent clickEvent)
            {
                handle_button_click (new_button);
            }
        });

        parent.addComponent (new_button);

        if (kc != 0)
        {
            new_button.addShortcutListener (new AbstractField.FocusShortcut (new_button, kc, mk)
            {
                @Override
                public void handleAction (Object sender, Object target)
                {
                    handle_button_click (new_button);
                }
            });
        }

        return (new_button);
    }

    private Button createButton (Layout parent, String id, Resource icon, String caption)
    {
        return (createButton (parent, id, icon, caption, 0, 0));
    }

    private Button createButton (Layout parent, String id, Resource icon)
    {
        return (createButton (parent, id, icon, null, 0, 0));
    }

    private void build_toolbar()
    {
        current_toolbar = new CssLayout();

        CssLayout local_toolbar = new CssLayout();
        current_toolbar.addComponent(local_toolbar);

        CssLayout navigation = new CssLayout();
        navigation.addStyleName("v-component-group");
        navigation.addStyleName("ui-toolbar-spacer");
        createButton (navigation, "prev-smartbox", FontAwesome.CHEVRON_LEFT, null,
                ShortcutAction.KeyCode.ARROW_UP, ShortcutAction.ModifierKey.CTRL);
        createButton (navigation, "next-smartbox", FontAwesome.CHEVRON_RIGHT, null,
                ShortcutAction.KeyCode.ARROW_DOWN, ShortcutAction.ModifierKey.CTRL);
        local_toolbar.addComponent(navigation);

        createButton (local_toolbar, "save", FontAwesome.SAVE)
            .addStyleName("ui-toolbar-spacer");

        CssLayout edition = new CssLayout();
        edition.addStyleName("v-component-group");
        edition.addStyleName("ui-toolbar-spacer");
        createButton (edition, "undo-cell-edit", FontAwesome.UNDO, "Undo");
        createButton (edition, "redo-cell-edit", FontAwesome.REPEAT, null);
        createButton (edition, "delete-cell", FontAwesome.TRASH_O, null);
        local_toolbar.addComponent (edition);

        CssLayout view_controls = new CssLayout();
        view_controls.addStyleName("v-component-group");
        view_controls.addStyleName("ui-toolbar-spacer");
        createButton (view_controls, VM_NOTEBOOK,
            new ExternalResource("vaadin://formulas/notebook-view.png"));
        createButton (view_controls, VM_SINGLE,
            new ExternalResource("vaadin://formulas/single-view.png"), null,
                ShortcutAction.KeyCode.INSERT, ShortcutAction.ModifierKey.CTRL);
        local_toolbar.addComponent(view_controls);

        // TODO: CTRL+ENTER => RUN AND SKIP TO NEXT
        // TODO: SELECTION + SHIFT+ENTER => RUN ONLY SELECTED STATEMENTS

        final Button source_view = new Button ();
        source_view.setId("output");
        source_view.addStyleName("ui-toolbar-spacer");
        source_view.setHtmlContentAllowed(true);
        String ico = "<path class=\"path1\" d=\"M1088 128h-64v-64c0-35.2-28.8-64-64-64h-896c-35.2 0-64 28.8-64 64v768c0 35.2 28.8 64 64 64h64v64c0 35.2 28.8 64 64 64h896c35.2 0 64-28.8 64-64v-768c0-35.2-28.8-64-64-64zM128 192v640h-63.886c-0.040-0.034-0.082-0.076-0.114-0.116v-767.77c0.034-0.040 0.076-0.082 0.114-0.114h895.77c0.040 0.034 0.082 0.076 0.116 0.116v63.884h-768c-35.2 0-64 28.8-64 64v0zM1088 959.884c-0.034 0.040-0.076 0.082-0.116 0.116h-895.77c-0.040-0.034-0.082-0.076-0.114-0.116v-767.77c0.034-0.040 0.076-0.082 0.114-0.114h895.77c0.040 0.034 0.082 0.076 0.116 0.116v767.768z\"></path>\n" +
                "<path class=\"path2\" d=\"M960 352c0 53.020-42.98 96-96 96s-96-42.98-96-96 42.98-96 96-96 96 42.98 96 96z\"></path>\n" +
                "<path class=\"path3\" d=\"M1024 896h-768v-128l224-384 256 320h64l224-192z\"></path>";
        source_view.setCaption("<svg style=\"fill: currentColor; width: 1.5em; margin-top:0.3em;\" viewBox=\"0 0 1152 1024\">" + ico + "</svg>");
        source_view.addStyleName("tiny");
        source_view.addStyleName("toolbar");
        source_view.addClickListener(new Button.ClickListener()
        {
            @Override
            public void buttonClick(Button.ClickEvent clickEvent)
            {
                handle_button_click (source_view);
            }
        });
        local_toolbar.addComponent (source_view);


//        Button output_view = new Button ();
//        output_view.setHtmlContentAllowed(true);
//        String ico2 = "<path d=\"M249.649 792.806l-107.776 166.4 11.469 54.426 54.272-11.622 107.725-166.298c-11.469-6.144-22.835-12.698-33.843-19.968-11.162-7.219-21.811-14.95-31.846-22.938zM705.943 734.694c0.717-1.485 1.178-3.123 1.843-4.71 2.714-5.99 5.12-11.981 7.066-18.278 0.307-1.126 0.461-2.253 0.819-3.277 1.997-6.963 3.686-13.824 5.018-20.89 0-0.358 0-0.614 0-1.075 9.984-59.853-7.424-126.618-47.258-186.931l56.832-87.757c65.485 8.346 122.112-8.141 149.35-50.278 47.258-72.858-10.24-194.15-128.256-271.002-118.118-76.902-252.058-80.128-299.213-7.373-27.341 42.189-19.354 100.71 15.002 157.338l-56.934 87.757c-71.117-11.93-139.059-0.819-189.594 32.768-0.307 0.102-0.666 0.205-0.87 0.41-5.888 3.994-11.622 8.397-16.998 13.005-0.87 0.717-1.894 1.382-2.611 2.099-5.018 4.301-9.523 9.114-13.875 13.926-1.024 1.229-2.458 2.304-3.43 3.584-5.427 6.195-10.445 12.749-14.848 19.712-70.861 109.21-10.394 274.483 134.81 369.101 145.306 94.618 320.512 82.637 391.219-26.573 4.454-6.912 8.55-14.131 11.93-21.555zM664.215 224.845c-45.414-29.542-67.584-76.134-49.408-104.243 18.125-28.006 69.683-26.726 114.995 2.816 45.517 29.542 67.482 76.237 49.408 104.243s-69.53 26.726-114.995-2.816z\"></path>";
//        output_view.setCaption("<svg style=\"fill: currentColor; width: 1.5em; margin-top:0.3em;\" viewBox=\"0 0 1024 1024\">" + ico2 + "</svg>");
//        output_view.addStyleName("tiny");
//        view_controls.addComponent (output_view);
//
//        Button run = new Button ();
//        run.setHtmlContentAllowed(true);
//        String ico3 = "<path class=\"path1\" d=\"M192 128l640 384-640 384z\"></path>";
//        run.setCaption("<svg style=\"fill: currentColor; width: 1.5em; margin-top:0.3em;\" viewBox=\"0 0 1024 1024\">" + ico3 + "</svg>");
//        run.addStyleName("tiny");
//        view_controls.addComponent (run);

    }

    private void build_sidebar ()
    {
        acSidebar = new Accordion ();
        acSidebar.setSizeFull ();
        acSidebar.addStyleName ("borderless");

        sidebar = new ComponentPalette (ctx);
        sidebar.setWidth (100, Unit.PERCENTAGE);
        sidebar.setHeightUndefined ();
        sidebar.setPaletteClickListener (new LayoutEvents.LayoutClickListener ()
        {
            @Override
            public void layoutClick (LayoutEvents.LayoutClickEvent layoutClickEvent)
            {
                Component clicked = layoutClickEvent.getClickedComponent();
                String canonical_name = clicked != null? clicked.getId () : null;

                log.info ("layoutClick: DoubleClick clicked={} canonical_name={}", clicked, canonical_name);

                // Handle double-clicking a component inside the palette
                if (canonical_name != null && layoutClickEvent.isDoubleClick())
                {
                    log.info ("layoutClick: DoubleClick clicked={} canonical_name={}", clicked, canonical_name);
                    int cell_index = get_current_cell_index ();
                    Object new_object = insert_new_cell (canonical_name, cell_index + 1);
                    update_cell_focus (new_object);
                }
            }
        });

        acSidebar.addTab (sidebar, "Components");

        acSidebar.addTab (new Label ("Hello world"), "Visualization");
    }

//    @Override
//    public void changeContents(SmartBox source)
//    {
//        formulae_changed = true;
//
//        if (!source.getInstanceData().isEmpty() &&
//            cell_list.indexOf(source) == cell_list.size() - 1)
//        {
//            add_smartbox();
//        }
//
//        // Save every 10 seconds
//        if (last_save + 10000 < System.currentTimeMillis())
//        {
//            save_formulae(formula_name);
//            last_save = System.currentTimeMillis();
//        }
//    }

    @Override
    public void detach ()
    {
        super.detach();
        //save_formulae(formula_name);
    }

    public boolean save_formulae (String formulae_name)
    {
        if (formulae_name == null /*|| !formulae_changed*/)
        {
            log.info ("Nothing to save");
            return (false);
        }

        Path userdir = sapi.getDefaultUserDir ();

        if (userdir == null)
        {
            log.info ("Save failed");
            return (false);
        }

        Path formulae_path = userdir.resolve (formulae_name + ".quark");

        if (!tctx.save (formulae_path))
        {
            return (false);
        }

        formulae_changed = false;
        return (true);
    }

    public boolean load_formulae (String formulae_name)
    {
        // TODO: REGISTER COMPLETE FILE SOURCE INCLUDING SOURCE FILESYSTEM
        Path userdir = sapi.getDefaultUserDir ();

        if (userdir == null)
        {
            log.info("Load failed");
            return (false);
        }

        Path formulae_path = userdir.resolve (formulae_name + ".quark");

        formulae_changed = false;

        return (tctx.load (formulae_path));
    }

    private Label get_icon (String icon_name)
    {
        int icon_size_px = 48;

        String icon_html =
            "<div><img src='/VAADIN/themes/kuori/img/" + icon_name + "' " +
                "width='" + icon_size_px + "px' height='" + icon_size_px + "px' /></div>";

        Label icon_label = new Label (icon_html, ContentMode.HTML);
        icon_label.addStyleName ("formula-icon");
        icon_label.setWidthUndefined ();
        return (icon_label);
    }

    private Label get_title (String name)
    {
        String title_html =
            "<h1>" + name + "</h1>" +
            "<div class='formula-info'>Created on 19/Jul/1969</div>";

        return (new Label (title_html, ContentMode.HTML));
    }

    private void unfocus (Component component)
    {
        Component parent = component.getParent();

        while (parent != null)
        {
            if(parent instanceof Component.Focusable)
            {
                ((Component.Focusable) parent).focus();
                break;
            }
            else
            {
                parent = parent.getParent();
            }
        }
    }

    private void build_formula_view (String view_mode)
    {
        // TODO: VERIFICAR SE É MELHOR CHECAR A EXISTÊNCIA DA FORMULA AQUI OU NA CRIAÇÃO DO COMPONENTE
        caption = "Formula: <b>" + formula_name + "</b>";

        setMargin (new MarginInfo (true, false, true, false));

        build_toolbar();
        build_sidebar ();

        insert_here_cell = new Cell (ctx, null);

        HorizontalLayout header = new HorizontalLayout ();
        header.setWidth (100, Unit.PERCENTAGE);
        {
            CssLayout left_panel = new CssLayout ();
            left_panel.setWidth (40, Unit.PIXELS);
            header.addComponent (left_panel);

            final HorizontalLayout caption = new HorizontalLayout ();
            caption.setSpacing (true);
            caption.addStyleName ("formula-header");
            caption.setWidth (100, Unit.PERCENTAGE);
            {
                caption.addComponent (get_icon ("freepik-saturn.png"));

                VerticalLayout title_area = new VerticalLayout ();
                {
                    final TextField title = new TextField ();
                    title.setWidth (100, Unit.PERCENTAGE);
                    title.setValue ("Default");

                    final ShortcutListener handle_enter = new ShortcutListener
                        ("EnterShortcut", ShortcutAction.KeyCode.ENTER, null)
                    {
                        @Override
                        public void handleAction (Object o, Object o1)
                        {
                            Notification.show ("New title: " + title.getValue());
                            unfocus (title);

                        }
                    };

                    title.addFocusListener (new FieldEvents.FocusListener ()
                    {
                        @Override
                        public void focus (FieldEvents.FocusEvent focusEvent)
                        {
                            title.addShortcutListener (handle_enter);
                        }
                    });

                    title.addBlurListener (new FieldEvents.BlurListener ()
                    {
                        @Override
                        public void blur (FieldEvents.BlurEvent blurEvent)
                        {
                            title.removeShortcutListener (handle_enter);
                        }
                    });
                    title_area.addComponent (title);
                }

                caption.addComponent (title_area);
                caption.setExpandRatio (title_area, 1.0f);
            }
            header.addComponent (caption);

            CssLayout right_panel = new CssLayout ();
            right_panel.setWidth (36, Unit.PIXELS);
            header.addComponent (right_panel);
            header.setExpandRatio (caption, 1.0f);
        }
        addComponent (header);

        content = new VerticalLayout();
        content.addStyleName ("formula-cells");
        addComponent (content);

        // TODO: RETRIEVE RUNNING TASKS WITHOUT LOAD
        tctx = sapi.newTaskContext ();
        load_formulae(formula_name);
        object_list = tctx.currentTask (CompositeTask.class);

        log.info ("object_list: {}", object_list);

        synchronize_cell_view ();

        // Set focus to first available object
        update_cell_focus (null);
    }

    @Controller
    private boolean is_valid = true;

    @Validate
    private void validate ()
    {
        log.info ("*** VALIDATE ***: formula_name = {}", formula_name);

        if (System.getProperty("rq.home") == null)
        {
            log.info("FATAL: Missing rq.home");
            is_valid = false;
            return;
        }

        // TODO: CORRIGIR INSTANCIAÇÃO DO COMPONENTE PARA SINCRONIZAR COM O IPOJO
        if (formula_name != null && formula_name.contains("@"))
        {
            log.info("Service INVALIDATED");
            is_valid = false;
        }
        else
        {
            log.info("Service validated");
        }

        HashMap menu_item = new HashMap()
        {{
            put("title", "default");
            put("icon", icon);
            put("weight", weight);
            put("navid", navid + ":default");
        }};

        init_data.add(menu_item);
    }

    @Override
    public void enter(ViewChangeListener.ViewChangeEvent event)
    {
        log.info ("Enter viewName={} parameters={}", event.getViewName(), event.getParameters());

        if (getComponentCount() == 0)
        {
            if (event.getViewName().contains(":"))
            {
                build_formula_view (event.getParameters());
            }
            else
            {
                // NO MORE!
                ///buildBrowserView();
            }
        }
        else // View already built
        {
            if (event.getViewName().contains(":"))
            {
                //selectFormulaView(event.getParameters());
            }
        }
    }
}

// EOF
