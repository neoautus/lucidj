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

package org.lucidj.newview;

import org.lucidj.api.ObjectRenderer;
import org.lucidj.api.RendererFactory;
import org.lucidj.api.SecurityEngine;
import org.lucidj.api.ServiceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.event.ItemClickEvent;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.ui.Button;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

import java.io.File;
import java.nio.file.Path;

import org.osgi.framework.BundleContext;

public class NewView extends VerticalLayout implements View
{
    private final static Logger log = LoggerFactory.getLogger (NewView.class);

    public final static String NAVID = "new";

    // TODO: INIT THESE VARIABLES WITH @ServiceContext.requires/depends
    private RendererFactory rendererFactory;
    private SecurityEngine securityEngine;

    private ObjectRenderer directories;

    public NewView (ServiceContext serviceContext, BundleContext bundleContext)
    {
        rendererFactory = serviceContext.getService (bundleContext, RendererFactory.class);
        securityEngine = serviceContext.getService (bundleContext, SecurityEngine.class);
    }

    private void buildView ()
    {
        setMargin (true);
        setSpacing (true);

        Label h1 = new Label ("New");
        h1.addStyleName ("h1");
        addComponent (h1);

        addComponent (location_panel ());
        addComponent (select_type_panel ());
    }

    private Panel location_panel ()
    {
        Panel p = new Panel ("Location");
        VerticalLayout content = new VerticalLayout ();
        p.setContent (content);
        content.setSpacing (true);
        content.setMargin (true);
        HorizontalLayout directory_and_browse = new HorizontalLayout ();
        directory_and_browse.setSpacing (true);
        directory_and_browse.setWidth (100, Unit.PERCENTAGE);
        final Label directory_label = new Label ("/home/marcond/My Lab/lucidj-dev/stage/system/");
        directory_and_browse.addComponent (directory_label);
        Button confirm = new Button ("Save");
        confirm.addStyleName (ValoTheme.BUTTON_SMALL);
        confirm.addStyleName (ValoTheme.BUTTON_PRIMARY);
        confirm.setVisible (false);
        directory_and_browse.addComponent (confirm);
        Button change_directory = new Button ("Change location...");
        change_directory.addStyleName (ValoTheme.BUTTON_SMALL);
        directory_and_browse.addComponent (change_directory);
        directory_and_browse.setExpandRatio (directory_label, 1.0f);
        content.addComponent (directory_and_browse);

        Path projects_home = securityEngine.getSubject ().getDefaultUserDir ();
        directories = rendererFactory.newRenderer (projects_home);
        directories.setVisible (false);
        content.addComponent (directories);

        change_directory.addClickListener (new Button.ClickListener ()
        {
            @Override
            public void buttonClick (Button.ClickEvent clickEvent)
            {
                if (directories.isVisible ())
                {
                    change_directory.setCaption ("Change location...");
                    directories.setVisible (false);
                    confirm.setVisible (false);
                }
                else
                {
                    change_directory.setCaption ("Cancel change");
                    directories.setVisible (true);
                    confirm.setVisible (true);
                }
            }
        });

        directories.addListener (new Listener ()
        {
            @Override
            public void componentEvent (Event event)
            {
                if (event instanceof ItemClickEvent)
                {
                    ItemClickEvent itemClickEvent = (ItemClickEvent)event;
                    File item_id = ((File)itemClickEvent.getItemId ());

                    log.info ("CLICK item_path={}", item_id);
                    directory_label.setValue (item_id.getPath ());
                }
            }
        });

        return (p);
    }

    private Panel select_type_panel ()
    {
        Panel p = new Panel ("Artifact type");
        VerticalLayout content = new VerticalLayout ();
        p.setContent (content);
        content.setSpacing (true);
        content.setMargin (true);
        HorizontalLayout directory_and_browse = new HorizontalLayout ();
        directory_and_browse.setWidth (100, Unit.PERCENTAGE);
        Label directory_label = new Label ("/home/marcond/My Lab/lucidj-dev/stage/system/");
        directory_and_browse.addComponent (directory_label);
        Button change_directory = new Button ("Change location...");
        change_directory.setStyleName (ValoTheme.BUTTON_SMALL);
        directory_and_browse.addComponent (change_directory);
        directory_and_browse.setExpandRatio (directory_label, 1.0f);
        content.addComponent (directory_and_browse);


        final Label peekaboo = new Label ("Hello world!");
        peekaboo.setVisible (false);
        content.addComponent (peekaboo);

        change_directory.addClickListener (new Button.ClickListener ()
        {
            @Override
            public void buttonClick (Button.ClickEvent clickEvent)
            {
                if (peekaboo.isVisible ())
                {
                    change_directory.setCaption ("Change location...");
                    peekaboo.setVisible (false);
                }
                else
                {
                    change_directory.setCaption ("Cancel change");
                    peekaboo.setVisible (true);
                }
            }
        });

        return (p);
    }

    @Override
    public void enter (ViewChangeListener.ViewChangeEvent event)
    {
        if (getComponentCount () == 0)
        {
            buildView ();
        }
    }
}

// EOF
