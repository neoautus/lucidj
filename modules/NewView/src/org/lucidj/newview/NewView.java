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

import org.lucidj.api.vui.NavigatorManager;
import org.lucidj.api.vui.ObjectRenderer;
import org.lucidj.api.vui.RendererFactory;
import org.lucidj.api.SecurityEngine;
import org.lucidj.api.ServiceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.data.Property;
import com.vaadin.event.FieldEvents;
import com.vaadin.event.ItemClickEvent;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.server.UserError;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.AbstractTextField;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.DateField;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Layout;
import com.vaadin.ui.OptionGroup;
import com.vaadin.ui.Panel;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.osgi.framework.BundleContext;

public class NewView extends FormLayout implements View
{
    private final static Logger log = LoggerFactory.getLogger (NewView.class);

    public final static String NAVID = "new";

    // TODO: INIT THESE VARIABLES WITH @ServiceContext.requires/depends
    private RendererFactory rendererFactory;
    private SecurityEngine securityEngine;
    private NavigatorManager navigatorManager;

    private TextField frm_project_name;
    private TextField frm_directory;
    private ObjectRenderer frm_directories;
    private Label frm_artifact_options;
    private HorizontalLayout frm_footer;
    private Button frm_create_project;
    private Label frm_messages;
    private boolean test_debug_artifact_options;

    private Path projects_dir;

    public NewView (ServiceContext serviceContext, BundleContext bundleContext)
    {
        rendererFactory = serviceContext.getService (bundleContext, RendererFactory.class);
        securityEngine = serviceContext.getService (bundleContext, SecurityEngine.class);
        navigatorManager = serviceContext.getService (bundleContext, NavigatorManager.class);
        projects_dir = securityEngine.getSubject ().getDefaultUserDir ();
    }

    private void buildView ()
    {
        setMargin (true);
        setSpacing (true);

        Label header = new Label ("New project");
        header.addStyleName ("h2");
        header.addStyleName ("colored");
        addComponent (header);

        frm_project_name = new TextField ("Name");
        frm_project_name.setImmediate (true);
        frm_project_name.setTextChangeEventMode (AbstractTextField.TextChangeEventMode.EAGER);
        frm_project_name.setRequired (true);
        frm_project_name.setInputPrompt ("Project name");
        frm_project_name.setWidth (100, Unit.PERCENTAGE);
        frm_project_name.addTextChangeListener (new FieldEvents.TextChangeListener ()
        {
            @Override
            public void textChange (FieldEvents.TextChangeEvent textChangeEvent)
            {
                validate (textChangeEvent.getText ());
            }
        });
        frm_project_name.addValueChangeListener (new Property.ValueChangeListener ()
        {
            @Override
            public void valueChange (Property.ValueChangeEvent valueChangeEvent)
            {
                validate ((String)valueChangeEvent.getProperty ().getValue ());
            }
        });
        addComponent (frm_project_name);

        addComponent (form_location_panel ());

        addComponent (form_type_panel ());

        // TODO: FILL FROM PLUGINS
        //fill_project_options (this);

        //------------------
        // ARTIFACT OPTIONS
        //------------------

        frm_artifact_options = new Label("Artifact options placeholder");
        frm_artifact_options.setId ("_artifact_options");
        frm_artifact_options.addStyleName ("h3");
        frm_artifact_options.addStyleName ("colored");
        addComponent (frm_artifact_options);

        //-------------
        // FORM FOOTER
        //-------------

        frm_create_project = new Button ("Create");
        frm_create_project.addStyleName (ValoTheme.BUTTON_PRIMARY);
        frm_create_project.setEnabled (false);
        frm_create_project.addClickListener (new Button.ClickListener ()
        {
            @Override
            public void buttonClick (Button.ClickEvent clickEvent)
            {
                create_project ();
            }
        });

        frm_messages = new Label ();

        frm_footer = new HorizontalLayout();
        frm_footer.setMargin (new MarginInfo (true, false, true, false));
        frm_footer.setSpacing (true);
        frm_footer.setDefaultComponentAlignment (Alignment.MIDDLE_LEFT);
        frm_footer.addComponent (frm_create_project);
        frm_footer.addComponent (frm_messages);
        addComponent (frm_footer);

        //---------------------------
        // INIT THE ARTIFACT OPTIONS
        //---------------------------

        // This is the last step since it will look for frm_artifact_options and frm_footer
        // to clear the form and fill in the options from the artifact
        fill_artifact_options ();
    }

    private void validate (String name_for_project)
    {
        name_for_project = name_for_project.trim ();

        boolean valid_name = !name_for_project.isEmpty ();
        boolean create_project = false;
        String messages = "";

        if (valid_name)
        {
            String artifact_name = name_for_project + ".leap";
            String project_dir = frm_directory.getValue ();
            Path project_path = Paths.get (project_dir);

            if (!Files.exists (project_path.resolve (artifact_name)))
            {
                create_project = true;
            }
            else
            {
                messages = "The artifact " + artifact_name + " already exists.";
            }
        }

        // Default is disabled
        frm_create_project.setEnabled (create_project);
        frm_messages.setValue (messages);
    }

    private void open_project (Path project_path)
    {
        String item_path = projects_dir.relativize (project_path).toString ();

        // TODO: CREATE BASIC SYSTEM NAVIGATION "actions" AND PROPERTIES
        Map<String, Object> properties = new HashMap<> ();
        properties.put ("artifactUrl", project_path.toUri ().toString ());

        navigatorManager.navigateTo ("open/" + item_path, properties);
    }

    private void create_project ()
    {
        String project_name = frm_project_name.getValue ();
        String project_dir = frm_directory.getValue ();
        log.info ("Create project '{}' into '{}'", project_name, project_dir);

        Path project_path = Paths.get (project_dir);

        if (!Files.exists (project_path))
        {
            try
            {
                Files.createDirectories (project_path);
            }
            catch (IOException e)
            {
                log.error ("Exception creating base directory: {}", project_path, e);
                frm_messages.setValue ("Exception creating directory: " + e.toString ());
                return;
            }
        }

        String artifact_name = project_name + ".leap";
        Path full_project_path = project_path.resolve (artifact_name);

        // Create project directory
        try
        {
            Files.createDirectory (full_project_path);
        }
        catch (IOException e) // java.nio.file.FileAlreadyExistsException
        {
            log.error ("Exception creating project directory: {}", project_path, e);
            frm_messages.setValue ("Exception creating project: " + e.toString ());
            return;
        }

        // Create default main
        try
        {
            Files.createFile (full_project_path.resolve ("main.gluon"));
        }
        catch (IOException e)
        {
            log.error ("Exception creating default main: {}", project_path, e);
            frm_messages.setValue ("Exception creating default main: " + e.toString ());
            return;
        }

        open_project (full_project_path);
    }

    private void fill_project_options (FormLayout form)
    {
        Label section = new Label("Project options");
        section.addStyleName("h4");
        section.addStyleName("colored");
        form.addComponent(section);

        DateField birthday = new DateField("Birthday");
        birthday.setValue(new Date ());
        form.addComponent(birthday);

        TextField username = new TextField("Username");
        username.setValue ("curiosity");
        form.addComponent (username);

        OptionGroup sex = new OptionGroup("Sex");
        sex.addItem("Female");
        sex.addItem("Male");
        sex.select("Male");
        sex.addStyleName("horizontal");
        form.addComponent(sex);
        sex.addValueChangeListener (new Property.ValueChangeListener ()
        {
            @Override
            public void valueChange (Property.ValueChangeEvent valueChangeEvent)
            {
                test_debug_artifact_options = ((String)valueChangeEvent.getProperty ().getValue ()).equals ("Female");
                fill_artifact_options ();
            }
        });
    }

    private void fill_artifact_options ()
    {
        // Remove all components from frm_artifact_options to frm_footer
        int index = getComponentIndex (frm_artifact_options);

        for (int pos = getComponentCount () - 1; pos > index; pos--)
        {
            removeComponent (getComponent (pos));
        }

        // Add the components from the artifact options
        if (component_fill_artifact_options (this))
        {
            frm_artifact_options.setValue ("LucidJ Application Options");
            frm_artifact_options.setVisible (true);
        }
        else
        {
            frm_artifact_options.setVisible (false);
        }

        // Add the footer again
        addComponent (frm_footer);
    }

    private boolean component_fill_artifact_options (FormLayout form)
    {
        if (test_debug_artifact_options)
        {
            TextField email = new TextField("Email");
            email.setValue("viking@surface.mars");
            email.setWidth("50%");
            email.setRequired(true);
            form.addComponent(email);

            TextField location = new TextField("Location");
            location.setValue("Mars, Solar System");
            location.setWidth("50%");
            location.setComponentError(new UserError ("This address doesn't exist"));
            form.addComponent(location);

            TextField phone = new TextField("Phone");
            phone.setWidth("50%");
            form.addComponent(phone);
        }
        return (test_debug_artifact_options);
    }

    private Component form_type_panel ()
    {
        HorizontalLayout group = new HorizontalLayout ();
        group.setWidth (100, Unit.PERCENTAGE);
        group.setCaption ("Artifact type");

        Map<String, Object> component = new HashMap<> ();
        component.put ("iconTitle", "LucidJ Application");
        component.put ("iconUrl", "apps/system-run");

        List<Map<String, Object>> components = new ArrayList<> ();
        components.add (component);

        ObjectRenderer component_renderer = rendererFactory.newRenderer (components);
        component_renderer.setWidth (100, Unit.PERCENTAGE);

        Panel field_panel = new Panel ();
        field_panel.setWidth (100, Unit.PERCENTAGE);
        field_panel.setContent (component_renderer);
        group.addComponent (field_panel);
        return (group);
    }

    private Layout form_location_panel ()
    {
        VerticalLayout rolldown = new VerticalLayout ();
        rolldown.setCaption ("Location");
        rolldown.setSpacing (true);

        HorizontalLayout directory_and_browse = new HorizontalLayout ();
        directory_and_browse.setSpacing (true);
        directory_and_browse.setWidth (100, Unit.PERCENTAGE);

        frm_directory = new TextField ();
        frm_directory.setValue (projects_dir.toString ());
        frm_directory.setWidth(100, Unit.PERCENTAGE);
        directory_and_browse.addComponent (frm_directory);
        Button confirm = new Button ("Save");
        confirm.addStyleName (ValoTheme.BUTTON_PRIMARY);
        confirm.setVisible (false);
        directory_and_browse.addComponent (confirm);
        Button change_directory = new Button ("Change location...");
        directory_and_browse.addComponent (change_directory);
        directory_and_browse.setExpandRatio (frm_directory, 1.0f);
        rolldown.addComponent (directory_and_browse);

        Path projects_home = securityEngine.getSubject ().getDefaultUserDir ();
        frm_directories = rendererFactory.newRenderer (projects_home);
        frm_directories.setVisible (false);
        rolldown.addComponent (frm_directories);

        change_directory.addClickListener (new Button.ClickListener ()
        {
            @Override
            public void buttonClick (Button.ClickEvent clickEvent)
            {
                if (frm_directories.isVisible ())
                {
                    change_directory.setCaption ("Change location...");
                    frm_directories.setVisible (false);
                    confirm.setVisible (false);
                }
                else
                {
                    change_directory.setCaption ("Cancel change");
                    frm_directories.setVisible (true);
                    confirm.setVisible (true);
                }
            }
        });

        frm_directories.addListener (new Listener ()
        {
            @Override
            public void componentEvent (Event event)
            {
                if (event instanceof ItemClickEvent)
                {
                    ItemClickEvent itemClickEvent = (ItemClickEvent)event;
                    File item_id = ((File)itemClickEvent.getItemId ());

                    log.info ("CLICK item_path={}", item_id);
                    frm_directory.setValue (item_id.getPath ());
                }
            }
        });

        return (rolldown);
    }


    @Override
    public void enter (ViewChangeListener.ViewChangeEvent event)
    {
        if (getComponentCount () == 0)
        {
            buildView ();
        }

        // Looks silly, but it's handy preserve everything but the project name
        frm_project_name.setValue ("");
    }
}

// EOF
