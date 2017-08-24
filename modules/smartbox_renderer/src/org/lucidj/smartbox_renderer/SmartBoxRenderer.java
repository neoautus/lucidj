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

package org.lucidj.smartbox_renderer;

import com.vaadin.event.FieldEvents;
import com.vaadin.event.LayoutEvents;
import com.vaadin.event.ShortcutAction;
import com.vaadin.server.FontAwesome;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.AbstractComponent;
import com.vaadin.ui.AbstractField;
import com.vaadin.ui.AbstractLayout;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import org.lucidj.api.EditorInterface;
import org.lucidj.api.Renderer;
import org.lucidj.api.RendererFactory;
import org.lucidj.api.ServiceContext;
import org.lucidj.smartbox.SmartBox;
import org.lucidj.aceeditor.AceEditor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.osgi.framework.BundleContext;

public class SmartBoxRenderer extends VerticalLayout implements Renderer, EditorInterface
{
    private static final transient Logger log = LoggerFactory.getLogger (SmartBoxRenderer.class);

    private SmartBox source;

    private Label pragmas_label;
    private AceEditor commands;
    private Component output_layout;

    private CssLayout cell_toolbar;

    private RendererFactory rendererFactory;

    public SmartBoxRenderer (ServiceContext serviceContext, BundleContext bundleContext)
    {
        rendererFactory = serviceContext.getService (bundleContext, RendererFactory.class);
    }

    private void init ()
    {
        output_layout = rendererFactory.newRenderer (source.getObjectManager ());
        init_main ();
        init_toolbar ();
    }

    private String render_pragmas (String[] pragmas)
    {
        String html = "<div style='width: 100%; display: inline-block; margin: 0;'>";

        for (String pragma: pragmas)
        {
            String background_color, color;

            pragma = pragma.trim ();

            switch (pragma)
            {
                case "publish":  color = "#ffffff"; background_color = "#4986e7"; break;
                case "autoexec": color = "#ffffff"; background_color = "#cc0000"; break;
                case "anything": color = "#ffffff"; background_color = "#16a765"; break;
                default:         color = "#666666"; background_color = "#dddddd"; break;
            }

            html += "<div style='float: right; margin-left: 5px; width: auto; height: auto; " +
                    "font: 11px arial,sans-serif; padding: 1px 5px; " +
                    "background-color: " + background_color + "; " +
                    "color: " + color + ";'>" + pragma.trim () + "</div>";
        }

        html += "</div>";

        return (html);
    }

    private void update_pragmas ()
    {
        if (source.getProperty ("Pragmas") != null)
        {
            String pragmas = (String)source.getProperty ("Pragmas");
            String[] pragma_list = pragmas.split (",");

            pragmas_label.setValue (render_pragmas (pragma_list));
            pragmas_label.setVisible (true);
        }
        else
        {
            pragmas_label.setVisible (false);
        }
    }

    public void init_main()
    {
        // TODO: SESSION MODE: 1 COMMAND PER LINE, RUN ONE-BY-ONE $

        setWidth ("100%");

        addLayoutClickListener(new LayoutEvents.LayoutClickListener()
        {
            @Override
            public void layoutClick(LayoutEvents.LayoutClickEvent layoutClickEvent)
            {
                log.info ("layoutClick");
            }
        });

        pragmas_label = new Label ("", ContentMode.HTML);
        addComponent (pragmas_label);
        update_pragmas ();

        commands = new AceEditor();
        commands.setMode ("ace/mode/java");
        commands.setOption ("enableBasicAutocompletion", true);
        commands.setOption ("enableLiveAutocompletion", true);
        commands.addStyleName ("smartbox-code");
        commands.setWidth ("100%");
        commands.setValue ((String)source.getValue ());

        commands.addFocusListener(new FieldEvents.FocusListener()
        {
            @Override
            public void focus(FieldEvents.FocusEvent focusEvent)
            {
                //
            }
        });

        commands.addTextChangeListener (new FieldEvents.TextChangeListener ()
        {
            @Override
            public void textChange (FieldEvents.TextChangeEvent textChangeEvent)
            {
                // Update source component contents
                source.setValue (textChangeEvent.getText ());
            }
        });

        addComponent (commands);

        // TODO: CREATE A WAY TO HANDLE WHERE TO PUT OUTPUT LAYOUT/COMMAND EDITOR
        addComponent (output_layout);
    }

    private void init_toolbar()
    {
        cell_toolbar = new CssLayout();

        CssLayout group = new CssLayout();
        group.addStyleName("v-component-group");
        group.addStyleName("ui-toolbar-spacer");

        Button run = new Button ();
        run.setHtmlContentAllowed(true);
        String ico3 = "<path class=\"path1\" d=\"M192 128l640 384-640 384z\"></path>";
        run.setCaption("<svg style=\"fill: currentColor; width: 1.5em; margin-top:0.3em;\" viewBox=\"0 0 1024 1024\">" + ico3 + "</svg>");
        run.addStyleName("tiny");
        group.addComponent (run);

        run.addClickListener(new Button.ClickListener()
        {
            @Override
            public void buttonClick(Button.ClickEvent clickEvent)
            {
                source.fireEvent (this, "run");
            }
        });

        run.addShortcutListener (new AbstractField.FocusShortcut (run,
            ShortcutAction.KeyCode.ENTER, ShortcutAction.ModifierKey.CTRL)
        {
            @Override
            public void handleAction (Object sender, Object target)
            {
                source.fireEvent (this, "run");
            }
        });

        Button stop = new Button ();
        stop.setIcon (FontAwesome.STOP);
        stop.addStyleName("tiny");
        group.addComponent (stop);

        stop.addClickListener(new Button.ClickListener()
        {
            @Override
            public void buttonClick(Button.ClickEvent clickEvent)
            {
                source.fireEvent (this, "stop");
            }
        });

        cell_toolbar.addComponent (group);
    }

    @Override // EditorInterface
    public boolean isModified()
    {
        return false;
    }

    @Override // EditorInterface
    public AbstractLayout toolbar()
    {
        return (cell_toolbar);
    }

    @Override // EditorInterface
    public com.vaadin.ui.Component.Focusable getFocusComponent ()
    {
        return (commands);
    }

    @Override // ^VerticalLayout
    public void attach ()
    {
        super.attach ();
        log.info ("*** >>>> ATTACH: {} ui={} parent={}", this, getUI (), getParent ());
    }

    @Override // ^VerticalLayout
    public void detach ()
    {
        super.detach ();
        log.info ("*** <<<< DETACH: {} ui={} parent={}", this, getUI (), getParent ());
    }

    public static boolean isCompatible (Object object)
    {
        return (object instanceof SmartBox);
    }

    @Override // Renderer
    public void objectLinked (Object obj)
    {
        source = (SmartBox)obj;
        init ();
    }

    @Override // Renderer
    public void objectUnlinked ()
    {
        source = null;
    }

    @Override // Renderer
    public AbstractComponent renderingComponent ()
    {
        return (this);
    }

    @Override // Renderer
    public void objectUpdated ()
    {
        commands.setValue ((String)source.getValue ());
    }

}

// EOF
