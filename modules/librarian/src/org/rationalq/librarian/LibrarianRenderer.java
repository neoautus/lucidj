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

package org.rationalq.librarian;

import org.lucidj.api.EditorInterface;
import org.lucidj.api.Renderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.event.ShortcutAction;
import com.vaadin.ui.AbstractField;
import com.vaadin.ui.AbstractLayout;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

@org.apache.felix.ipojo.annotations.Component (immediate = true)
@Instantiate
@Provides
public class LibrarianRenderer extends VerticalLayout implements Renderer, EditorInterface
{
    private final transient Logger log = LoggerFactory.getLogger (Librarian.class);

    private Librarian source;

    private CssLayout cell_toolbar;
    private TextField jar_file;
    private VerticalLayout results;

    public LibrarianRenderer ()
    {
        super ();
        init_main();
        init_toolbar();
    }

    public void init_main()
    {
        setWidth ("100%");

        HorizontalLayout fields = new HorizontalLayout ();

        fields.setSpacing (true);
        fields.setDefaultComponentAlignment (Alignment.MIDDLE_LEFT);
        fields.addComponent (new Label ("Jar filename:"));

        jar_file = new TextField ();
        fields.addComponent (jar_file);

        Button load = new Button ("Load");
        fields.addComponent (load);

        load.addClickListener (new Button.ClickListener ()
        {
            @Override
            public void buttonClick (Button.ClickEvent clickEvent)
            {
                // load_jar (jar_file.getValue ());
            }
        });

        Button clear = new Button ("Clear");
        fields.addComponent (clear);

        clear.addClickListener (new Button.ClickListener ()
        {
            @Override
            public void buttonClick (Button.ClickEvent clickEvent)
            {
                results.removeAllComponents ();
            }
        });

        addComponent (fields);

        results = new VerticalLayout ();
        results.addStyleName (ValoTheme.PANEL_BORDERLESS);
        results.setHeightUndefined();
        results.setWidth("100%");

        addComponent (results);
    }

    private void action_run()
    {
        log.info("RUN!!! from " + this);
    }

    private void init_toolbar()
    {
        cell_toolbar = new CssLayout();

        Button run = new Button ("Librarian");
        run.addStyleName("tiny");

        run.addClickListener(new Button.ClickListener()
        {
            @Override
            public void buttonClick(Button.ClickEvent clickEvent)
            {
                action_run();
            }
        });

        run.addShortcutListener (new AbstractField.FocusShortcut (run,
            ShortcutAction.KeyCode.ENTER, ShortcutAction.ModifierKey.SHIFT)
        {
            @Override
            public void handleAction (Object sender, Object target)
            {
                action_run();
            }
        });

        cell_toolbar.addComponent(run);
    }

    @Override // EditorInterface
    public boolean isModified ()
    {
        return (false);
    }

    @Override // EditorInterface
    public AbstractLayout toolbar ()
    {
        return (cell_toolbar);
    }

    @Override // EditorInterface
    public Focusable getFocusComponent ()
    {
        return (jar_file);
    }

    @Override // Renderer
    public boolean compatibleObject (Object obj_to_check)
    {
        return (obj_to_check instanceof Librarian);
    }

    @Override // Renderer
    public void objectLinked (Object obj)
    {
        source = (Librarian)obj;
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
        source.setValue (jar_file.getValue ());
    }
}

// EOF
