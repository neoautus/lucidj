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

package org.lucidj.html_renderer;

import org.lucidj.api.vui.EditorInterface;
import org.lucidj.api.vui.Renderer;
import org.lucidj.html.Html;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.data.Property;
import com.vaadin.event.ContextClickEvent;
import com.vaadin.event.LayoutEvents;
import com.vaadin.event.ShortcutAction;
import com.vaadin.server.Sizeable;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.AbstractField;
import com.vaadin.ui.AbstractLayout;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.RichTextArea;
import com.vaadin.ui.VerticalLayout;

import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

// TODO: REFACTOR!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
@org.apache.felix.ipojo.annotations.Component (immediate = true, publicFactory = false)
@Instantiate
@Provides
public class HtmlRenderer extends VerticalLayout implements Renderer, EditorInterface
{
    private final static Logger log = LoggerFactory.getLogger (HtmlRenderer.class);

    private Html source;
    private Label html_output = new Label ();

    private RichTextArea rich_text;
    private CssLayout editor_toolbar;

    public HtmlRenderer ()
    {
        super ();

        html_output.setWidth (100, Sizeable.Unit.PERCENTAGE);
        html_output.setContentMode (ContentMode.HTML);
        html_output.setHeightUndefined ();

        init_main ();
        init_toolbar ();
    }

    private void show_hide_rich_text ()
    {
        rich_text.setVisible (!rich_text.isVisible ());
    }

    private void render_html_from_rich_text ()
    {
        html_output.setValue ((String)source.getValue ());
    }

    private void init_toolbar()
    {
        editor_toolbar = new CssLayout();

        CssLayout group = new CssLayout();
        group.addStyleName("v-component-group");

        Button output_view = new Button ();
        output_view.setHtmlContentAllowed(true);
        String ico2 = "<path d=\"M249.649 792.806l-107.776 166.4 11.469 54.426 54.272-11.622 107.725-166.298c-11.469-6.144-22.835-12.698-33.843-19.968-11.162-7.219-21.811-14.95-31.846-22.938zM705.943 734.694c0.717-1.485 1.178-3.123 1.843-4.71 2.714-5.99 5.12-11.981 7.066-18.278 0.307-1.126 0.461-2.253 0.819-3.277 1.997-6.963 3.686-13.824 5.018-20.89 0-0.358 0-0.614 0-1.075 9.984-59.853-7.424-126.618-47.258-186.931l56.832-87.757c65.485 8.346 122.112-8.141 149.35-50.278 47.258-72.858-10.24-194.15-128.256-271.002-118.118-76.902-252.058-80.128-299.213-7.373-27.341 42.189-19.354 100.71 15.002 157.338l-56.934 87.757c-71.117-11.93-139.059-0.819-189.594 32.768-0.307 0.102-0.666 0.205-0.87 0.41-5.888 3.994-11.622 8.397-16.998 13.005-0.87 0.717-1.894 1.382-2.611 2.099-5.018 4.301-9.523 9.114-13.875 13.926-1.024 1.229-2.458 2.304-3.43 3.584-5.427 6.195-10.445 12.749-14.848 19.712-70.861 109.21-10.394 274.483 134.81 369.101 145.306 94.618 320.512 82.637 391.219-26.573 4.454-6.912 8.55-14.131 11.93-21.555zM664.215 224.845c-45.414-29.542-67.584-76.134-49.408-104.243 18.125-28.006 69.683-26.726 114.995 2.816 45.517 29.542 67.482 76.237 49.408 104.243s-69.53 26.726-114.995-2.816z\"></path>";
        output_view.setCaption("<svg style=\"fill: currentColor; width: 1.5em; margin-top:0.3em;\" viewBox=\"0 0 1024 1024\">" + ico2 + "</svg>");
        output_view.addStyleName("tiny");
        group.addComponent (output_view);

        output_view.addClickListener (new Button.ClickListener ()
        {
            @Override
            public void buttonClick (Button.ClickEvent clickEvent)
            {
                show_hide_rich_text ();
            }
        });

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
                render_html_from_rich_text ();
            }
        });

        run.addShortcutListener (new AbstractField.FocusShortcut (run,
                ShortcutAction.KeyCode.ENTER, ShortcutAction.ModifierKey.SHIFT)
        {
            @Override
            public void handleAction (Object sender, Object target)
            {
                render_html_from_rich_text ();
            }
        });

        editor_toolbar.addComponent(group);
    }

    private void init_main()
    {
        // Full width
        setWidth("100%");

        rich_text = new RichTextArea ();
        rich_text.setWidth ("100%");
        rich_text.setImmediate (true);

        addContextClickListener (new ContextClickEvent.ContextClickListener ()
        {
            @Override
            public void contextClick (ContextClickEvent contextClickEvent)
            {
                log.info ("CONTEXTCLICK: {}", contextClickEvent);
            }
        });

        addLayoutClickListener (new LayoutEvents.LayoutClickListener ()
        {
            @Override
            public void layoutClick (LayoutEvents.LayoutClickEvent layoutClickEvent)
            {
                if (layoutClickEvent.isDoubleClick())
                {
                    show_hide_rich_text ();
                }
            }
        });

        rich_text.addValueChangeListener (new Property.ValueChangeListener ()
        {
            @Override
            public void valueChange (Property.ValueChangeEvent valueChangeEvent)
            {
                source.setValue (rich_text.getValue ());
            }
        });

        addComponent (rich_text);
        addComponent (html_output);
    }

    @Override // EditorInterface
    public Component.Focusable getFocusComponent ()
    {
        return (rich_text);
    }

    @Override // EditorInterface
    public boolean isModified()
    {
        return false;
    }

    @Override // EditorInterface
    public AbstractLayout toolbar()
    {
        return (editor_toolbar);
    }

    public static boolean isCompatible (Object object)
    {
        return (object instanceof Html);
    }

    @Override // Renderer
    public void objectLinked (Object obj)
    {
        source = (Html)obj;
    }

    @Override // Renderer
    public void objectUnlinked ()
    {
        source = null;
    }

    @Override // Renderer
    public Component renderingComponent ()
    {
        return (this);
    }

    @Override // Renderer
    public void objectUpdated ()
    {
        rich_text.setValue ((String)source.getValue ());
    }
}

// EOF
