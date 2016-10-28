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

import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import org.lucidj.renderer.ObjectRenderer;
import org.rationalq.editor.ComponentInterface;
import org.rationalq.editor.ComponentState;
import org.rationalq.editor.EditorInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.event.LayoutEvents;
import com.vaadin.event.Transferable;
import com.vaadin.event.dd.DragAndDropEvent;
import com.vaadin.event.dd.DropHandler;
import com.vaadin.event.dd.DropTarget;
import com.vaadin.event.dd.TargetDetails;
import com.vaadin.event.dd.acceptcriteria.AcceptAll;
import com.vaadin.event.dd.acceptcriteria.AcceptCriterion;
import com.vaadin.server.FontAwesome;
import com.vaadin.server.Sizeable;
import com.vaadin.shared.ui.dd.VerticalDropLocation;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.AbstractLayout;
import com.vaadin.ui.Component;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.DragAndDropWrapper;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.UI;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

public abstract class AbstractCell implements DropHandler, LayoutEvents.LayoutClickListener, ComponentState.ChangeListener
{
    private final transient static Logger log = LoggerFactory.getLogger (ObjectRenderer.class);

    private String bundle_symbolic_name;
    private Object source_object;
    private ObjectRenderer object_renderer;
    private Component rendered_object;
    private DragAndDropWrapper cell_wrap;
    private CssLayout left_panel;
    private CssLayout right_panel;
    private HorizontalLayout decorated_cell;
    private int task_state;
    private Label running;
    private Label component_icon;

    public AbstractCell (BundleContext ctx, Object object)
    {
        log.info("Cell: object = {}", object);

        bundle_symbolic_name = ctx.getBundle ().getSymbolicName ();

        if (object != null)
        {
            // Register and render the source object
            source_object = object;
            object_renderer = new ObjectRenderer (ctx);
            rendered_object = object_renderer.link (source_object);
        }
        else
        {
            rendered_object = build_insert_here ();
        }

        log.info ("Cell: rendered_object = {}", rendered_object);

        // Create and set drag and drop wrapper
        cell_wrap = new DragAndDropWrapper (rendered_object);
        cell_wrap.addStyleName ("component-cell-wrapper");

        if (object == null) // Insert here cell
        {
            cell_wrap.addStyleName ("no-horizontal-drag-hints");
            cell_wrap.addStyleName ("no-vertical-drag-hints");
        }
        else // Normal cell
        {
            cell_wrap.addStyleName ("no-horizontal-drag-hints");
            cell_wrap.addStyleName ("no-box-drag-hints");
        }

        // Setup DD handlers for component insertion
        cell_wrap.setData (this);
        cell_wrap.setDropHandler(this);

        decorated_cell = new HorizontalLayout ();
        decorated_cell.setWidth(100, Sizeable.Unit.PERCENTAGE);

        // Left panel
        decorated_cell.addComponent (build_left_panel ());

        // TODO: NOTIFY ObjectRenderer COMPONENT SWAP SO WE CAN UPDATE LISTENERS!
        decorated_cell.addComponent (cell_wrap);
        decorated_cell.setExpandRatio (cell_wrap, 1.0f);

        // Right panel
        decorated_cell.addComponent (build_right_panel ());

        // Uses clicks to set where the focus is
        decorated_cell.addLayoutClickListener (this);
    }

    private Component build_insert_here ()
    {
        Label message = new Label ("Drag or double-click any component to add one or more here");
        message.addStyleName ("formula-insert-here");
        message.setHeight (64, Sizeable.Unit.PIXELS);
        message.setWidth (100, Sizeable.Unit.PERCENTAGE);
        return (message);
    }

    private Component build_left_panel ()
    {
        left_panel = new CssLayout ();
        left_panel.setWidth (32, Sizeable.Unit.PIXELS);
        left_panel.addStyleName ("cell-panel-left");
        left_panel.setHeight (100, Sizeable.Unit.PERCENTAGE);

        String icon_url = "/VAADIN/formulas/impossible.png";
        String icon_title = "The Unknown";

        if (source_object instanceof ComponentInterface)
        {
            // If it is a valid component, displays its icon on the top left corner of the cell
            ComponentInterface ci = (ComponentInterface)source_object;
            Bundle bnd = FrameworkUtil.getBundle (ci.getClass ());

            if (bnd != null)
            {
                icon_url = "/VAADIN/" + bnd.getSymbolicName () + "/component-icon.png";
                icon_title = ci.getIconTitle ();
            }
        }

        String component_icon_html =
            "<img class='component-icon' src='" + icon_url + "' title='" + SafeHtmlUtils.htmlEscape (icon_title) + "'/>";
        component_icon = new Label (component_icon_html, ContentMode.HTML);
        left_panel.addComponent (component_icon);

        // Put the component in a D&D wrapper and allow dragging it
        final DragAndDropWrapper panel_dd_wrap = new DragAndDropWrapper (left_panel);
        panel_dd_wrap.setDragStartMode(DragAndDropWrapper.DragStartMode.COMPONENT_OTHER);
        panel_dd_wrap.setDragImageComponent (component_icon);
        panel_dd_wrap.addStyleName("no-horizontal-drag-hints");
        panel_dd_wrap.addStyleName("no-box-drag-hints");

        // Set the wrapper to wrap tightly around the component
        panel_dd_wrap.setHeight (100, Sizeable.Unit.PERCENTAGE);
        panel_dd_wrap.setWidthUndefined ();
        panel_dd_wrap.setId ("test");

        // Setup DD handlers for component insertion
        panel_dd_wrap.setData (this);
        panel_dd_wrap.setDropHandler(this);

        // While left_panel is kept in order to be customized, here we return D&D wrapper
        return (panel_dd_wrap);
    }

    private CssLayout build_right_panel ()
    {
        right_panel = new CssLayout ();
        right_panel.addStyleName ("cell-panel-right");
        right_panel.setHeight (100, Sizeable.Unit.PERCENTAGE);
        right_panel.setWidth (32, Sizeable.Unit.PIXELS);

        String running_html = "*";
            //"<img src='/VAADIN/formulas/running.gif'/>";
        running = new Label (running_html, ContentMode.HTML);
        running.addStyleName ("component-task-state");
        running.setWidth (32, Sizeable.Unit.PIXELS);
        running.setVisible (false);
        right_panel.addComponent (running);

        if (source_object instanceof ComponentState)
        {
            ComponentState source_state = (ComponentState)source_object;
            source_state.addStateListener (this);
            setRunning (true, source_state);
        }
        else
        {
            setRunning (true, null);
        }

        return (right_panel);
    }

    public void setRunning (boolean visible, ComponentState state)
    {
        if (visible)
        {
            if (state != null)
            {
                task_state = state.getState ();

                if (task_state == ComponentState.ACTIVE || task_state == ComponentState.TERMINATED)
                {
                    // Ready to run visible only with hover or selection
                    running.removeStyleName ("component-task-state-visible");
                }
                else
                {
                    // All other states are always visible
                    running.addStyleName ("component-task-state-visible");
                }

                String color = "inherit";
                String html = "S" + String.valueOf (state.getState ());
                String title = "State " + String.valueOf (state.getState ());

                switch (task_state) // Also record the rendered state
                {
                    case ComponentState.INIT:
                    {
                        html = FontAwesome.CLOCK_O.getHtml ();
                        title = "Task awaiting initialization";
                        break;
                    }
                    case ComponentState.ABORTED:
                    {
                        color = "red";
                        html = "<span class='component-task-runnable'>" + FontAwesome.WARNING.getHtml () + "</span>";
                        title = "Task aborted, can try to run";
                        break;
                    }
                    case ComponentState.ACTIVE:
                    {
                        color = "green";
                        html = FontAwesome.PLAY.getHtml ();
                        title = "Run task";
                        break;
                    }
                    case ComponentState.TERMINATED:
                    {
                        color = "green";
                        html = FontAwesome.PLAY.getHtml ();
                        title = "Task finished, can run again";
                        break;
                    }
                    case ComponentState.INTERRUPTED:
                    {
                        color = "blue";
                        html = "<span class='component-task-runnable'>" + FontAwesome.PAUSE.getHtml () + "</span>";
                        title = "Task interrupted by user, can run again";
                        break;
                    }
                    case ComponentState.RUNNING:
                    {
                        color = "red";
                        html = FontAwesome.STOP.getHtml ();
                        title = "Task running";
                        break;
                    }
                }

                running.setValue ("<span style='color:" + color + ";' title='" + title + "'>" + html + "</span>");
            }
            else
            {
                running.setValue ("");
            }
        }
        running.setVisible (visible);
    }

    @Override
    public void stateChanged (ComponentState ref)
    {
        setRunning (true, ref);
    }

    public Component getDecoratedCell ()
    {
        return (decorated_cell);
    }

    public Object getSourceObject ()
    {
        return (source_object);
    }

    public void setFocus ()
    {
        if (rendered_object instanceof EditorInterface)
        {
            EditorInterface ei = (EditorInterface)rendered_object;

            setToolbar (ei.toolbar());

            if (ei.getFocusComponent () != null)
            {
                // TODO: BETTER HANDLING OF SCROLL INTO
                ei.getFocusComponent ().focus();
                UI.getCurrent().scrollIntoView (ei.getFocusComponent ());
            }

        }
        else // No toolbar interface
        {
            setToolbar (null);
        }

        cell_wrap.addStyleName ("component-cell-wrapper-selected");  //rendered_object
        left_panel.addStyleName ("cell-panel-selected");
        right_panel.addStyleName ("cell-panel-selected");
    }

    public void removeFocus ()
    {
        cell_wrap.removeStyleName ("component-cell-wrapper-selected");
        left_panel.removeStyleName ("cell-panel-selected");
        right_panel.removeStyleName ("cell-panel-selected");
    }

    @Override // DropHandler
    public void drop (DragAndDropEvent dragAndDropEvent)
    {
        log.info ("**** DROP! {}", dragAndDropEvent);

        final Transferable transferable = dragAndDropEvent.getTransferable();
        final Component sourceComponent = transferable.getSourceComponent();

        log.info ("sourceComponent = {}", sourceComponent);

        final TargetDetails dropTargetData = dragAndDropEvent.getTargetDetails ();
        final DropTarget target = dropTargetData.getTarget ();

        log.info ("DROP: source={} target={}", sourceComponent, target);

        String pos = (String)dropTargetData.getData ("verticalLocation");
        String canonical_name = sourceComponent.getId ();
        Object source_cell = ((DragAndDropWrapper)sourceComponent).getData ();
        Object source_object = (source_cell instanceof AbstractCell)? ((AbstractCell)source_cell).getSourceObject (): null;
        Object target_cell = ((DragAndDropWrapper)target).getData ();
        Object target_object = (target_cell instanceof AbstractCell)? ((AbstractCell)target_cell).getSourceObject (): null;

        log.info ("D&D: source=[{}, {}] => target=[{}, {}]", source_cell, source_object, target_cell, target_object);

        if (target_object == null)
        {
            insertNewObjectBefore (canonical_name, null);
        }
        else if (pos.equals (VerticalDropLocation.BOTTOM.toString ()))
        {
            if (source_cell instanceof AbstractCell)
            {
                if (source_object != target_object)
                {
                    log.info ("Move AFTER component #{}: {} {}", target_object, source_cell, canonical_name);
                    moveObjectAfter (source_object, target_object);
                }
            }
            else
            {
                log.info ("Drop AFTER component #{}: {} {}", target_object, source_cell, canonical_name);
                insertNewObjectAfter (canonical_name, target_object);
            }
        }
        else if (pos.equals (VerticalDropLocation.TOP.toString ()))
        {
            if (source_cell instanceof AbstractCell)
            {
                if (source_object != target_object)
                {
                    log.info ("Move BEFORE component #{}: {} {}", target_object, source_cell, canonical_name);
                    moveObjectBefore (source_object, target_object);
                }
            }
            else
            {
                log.info ("Drop BEFORE component #{}: {} {}", target_object, source_cell, canonical_name);
                insertNewObjectBefore (canonical_name, target_object);
            }
        }
    }

    @Override // DropHandler
    public AcceptCriterion getAcceptCriterion ()
    {
        return (AcceptAll.get ());
    }

    @Override // LayoutEvents.LayoutClickListener
    public void layoutClick (LayoutEvents.LayoutClickEvent layoutClickEvent)
    {
        Component clicked = layoutClickEvent.getClickedComponent();

        log.info("{} component={}", layoutClickEvent.isDoubleClick()? "layoutDoubleClick": "layoutClick", clicked);

        if (clicked != null)
        {
            if (layoutClickEvent.isDoubleClick())
            {
                layoutDoubleClick (layoutClickEvent.getClickedComponent());
            }
            else
            {
                if (layoutClickEvent.getClickedComponent () == running)
                {
                    taskStateClick (task_state);
                }
                else
                {
                    layoutClick (layoutClickEvent.getClickedComponent());
                }
            }
        }
    }

    public abstract Object insertNewObjectBefore (String obj_canonical_name, Object ref_obj);
    public abstract Object insertNewObjectAfter (String obj_canonical_name, Object ref_obj);
    public abstract void moveObjectBefore (Object source_object, Object target_object);
    public abstract void moveObjectAfter (Object source_object, Object target_object);
    public abstract void taskStateClick (int task_state);
    public abstract void layoutClick (Component component);
    public abstract void layoutDoubleClick (Component component);
    public abstract void setToolbar (AbstractLayout toolbar);
}

// EOF
