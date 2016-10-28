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

package org.rationalq.explorer;

import org.lucidj.system.SystemAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.event.ShortcutAction;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.server.FontAwesome;
import com.vaadin.server.Resource;
import com.vaadin.ui.AbstractField;
import com.vaadin.ui.Button;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;

import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.osgi.framework.BundleContext;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Context;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Property;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.ServiceProperty;

@Component
@Instantiate
@Provides (specifications = com.vaadin.navigator.View.class)
public class Explorer extends VerticalLayout implements View
{
    @Property public String title = "Explorer";
    @Property public int weight = 100;
    @Property public Resource icon = FontAwesome.FOLDER_OPEN_O;
    @Property private String navid = "explorer";
    //@Property private String options = "header";

    @ServiceProperty (name="X-Buga-Munga")
    private String[] caption = new String[] { "Ugamunga", "Oingoboingo" };

    @Context
    transient BundleContext ctx;

    private final transient Logger log = LoggerFactory.getLogger (Explorer.class);

    @Property(name="Init-Data")
    private List<HashMap> init_data = new LinkedList<> ();

    @Property(name="View-Body")
    private String formula_name;

    @Property(name="View-Toolbar")
    private CssLayout toolbar = null;

    @Requires
    SystemAPI sapi;

    public Explorer ()
    {
        log.info ("sapi = {}", sapi);
    }

    private void build_toolbar ()
    {
        toolbar = new CssLayout();

        CssLayout group = new CssLayout();
        group.addStyleName("v-component-group");

        Button output_view = new Button ();
        output_view.setHtmlContentAllowed(true);
        String ico2 = "<path d=\"M249.649 792.806l-107.776 166.4 11.469 54.426 54.272-11.622 107.725-166.298c-11.469-6.144-22.835-12.698-33.843-19.968-11.162-7.219-21.811-14.95-31.846-22.938zM705.943 734.694c0.717-1.485 1.178-3.123 1.843-4.71 2.714-5.99 5.12-11.981 7.066-18.278 0.307-1.126 0.461-2.253 0.819-3.277 1.997-6.963 3.686-13.824 5.018-20.89 0-0.358 0-0.614 0-1.075 9.984-59.853-7.424-126.618-47.258-186.931l56.832-87.757c65.485 8.346 122.112-8.141 149.35-50.278 47.258-72.858-10.24-194.15-128.256-271.002-118.118-76.902-252.058-80.128-299.213-7.373-27.341 42.189-19.354 100.71 15.002 157.338l-56.934 87.757c-71.117-11.93-139.059-0.819-189.594 32.768-0.307 0.102-0.666 0.205-0.87 0.41-5.888 3.994-11.622 8.397-16.998 13.005-0.87 0.717-1.894 1.382-2.611 2.099-5.018 4.301-9.523 9.114-13.875 13.926-1.024 1.229-2.458 2.304-3.43 3.584-5.427 6.195-10.445 12.749-14.848 19.712-70.861 109.21-10.394 274.483 134.81 369.101 145.306 94.618 320.512 82.637 391.219-26.573 4.454-6.912 8.55-14.131 11.93-21.555zM664.215 224.845c-45.414-29.542-67.584-76.134-49.408-104.243 18.125-28.006 69.683-26.726 114.995 2.816 45.517 29.542 67.482 76.237 49.408 104.243s-69.53 26.726-114.995-2.816z\"></path>";
        output_view.setCaption("<svg style=\"fill: currentColor; width: 1.5em; margin-top:0.3em;\" viewBox=\"0 0 1024 1024\">" + ico2 + "</svg>");
        output_view.addStyleName("tiny");
        group.addComponent (output_view);

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
                //action_run();
            }
        });

        run.addShortcutListener (new AbstractField.FocusShortcut (run,
                ShortcutAction.KeyCode.ENTER, ShortcutAction.ModifierKey.SHIFT)
        {
            @Override
            public void handleAction (Object sender, Object target)
            {
                //action_run();
            }
        });

        toolbar.addComponent(group);
    }

    @Override
    public void detach ()
    {
        super.detach();
        //save_formulae(formula_name);
    }

    private void build_explorer_view ()
    {
        //setMargin (new MarginInfo(true, true, true, true));
        setMargin(true);

        Label private_caption = new Label ("Your Formulas");
        private_caption.addStyleName("h2");
        addComponent(private_caption);

        VerticalLayout content = new VerticalLayout ();
        content.setSpacing (true);
        content.setSizeFull();
        addComponent (content);

        Path userdir = sapi.getDefaultUserDir ();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(userdir))
        {
            for (Path p: stream)
            {
                if (Files.isRegularFile (p))
                {
                    log.info ("p={}", p);
                    if (!p.toString ().endsWith (".quark"))
                    {
                        continue;
                    }

                    String filename = p.getFileName ().toString ();

                    if (filename.contains("."))
                    {
                        filename = filename.substring (0, filename.lastIndexOf('.'));
                    }

                    final String formula = filename;
                    Button formulae = new Button (formula);

                    formulae.addClickListener(new Button.ClickListener()
                    {
                        @Override
                        public void buttonClick(Button.ClickEvent clickEvent)
                        {
                            UI.getCurrent().getNavigator().navigateTo ("formulas:" + formula);
                        }
                    });

                    content.addComponent(formulae);
                    formulae.setSizeFull();
                }
                else
                {
                    log.info ("Entry: {}", p.toString() + "/");
                }
            }
        }
        catch (Exception e)
        {
            log.error ("Exception listing files", e);
        }
    }

    @Override
    public void enter(ViewChangeListener.ViewChangeEvent event)
    {
        log.info ("Enter viewName=" + event.getViewName() + " parameters=" + event.getParameters());

        if (getComponentCount() == 0)
        {
            build_explorer_view ();
            build_toolbar ();
        }
        else // View already built
        {
            // TODO: PLACE A FILE CHANGE LISTENER
            // updateBrowserView ();
        }
    }
}

// EOF
