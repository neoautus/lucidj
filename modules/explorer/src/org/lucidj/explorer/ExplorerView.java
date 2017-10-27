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

package org.lucidj.explorer;

import org.lucidj.api.vui.IconHelper;
import org.lucidj.api.vui.NavigatorManager;
import org.lucidj.api.vui.ObjectRenderer;
import org.lucidj.api.vui.RendererFactory;
import org.lucidj.api.SecurityEngine;
import org.lucidj.api.SecuritySubject;
import org.lucidj.api.ServiceContext;
import org.lucidj.api.ServiceObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.data.Property;
import com.vaadin.data.util.FilesystemContainer;
import com.vaadin.data.util.ObjectProperty;
import com.vaadin.event.ItemClickEvent;
import com.vaadin.event.LayoutEvents;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.server.Resource;
import com.vaadin.ui.AbstractComponent;
import com.vaadin.ui.Button;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.ui.Tree;
import com.vaadin.ui.VerticalLayout;

import java.io.File;
import java.io.FilenameFilter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.osgi.framework.BundleContext;

public class ExplorerView extends VerticalLayout implements View, ItemClickEvent.ItemClickListener
{
    private final static Logger log = LoggerFactory.getLogger (ExplorerView.class);

    public final static String NAVID = "explorer";

    private SecurityEngine security;
    private RendererFactory rendererFactory;
    private IconHelper iconHelper;
    private NavigatorManager navigatorManager;

    private Panel browse_files;
    private Resource default_icon;

    private Map<String, Tree> active_trees = new HashMap<> ();
    private Path userdir;
    private boolean filesystem_changed = false;
    private WatchService watch_service;

    public ExplorerView (ServiceContext serviceContext, BundleContext bundleContext)
    {
        security = serviceContext.getService (bundleContext, SecurityEngine.class);
        rendererFactory = serviceContext.getService (bundleContext, RendererFactory.class);
        iconHelper = serviceContext.getService (bundleContext, IconHelper.class);
        navigatorManager = serviceContext.getService (bundleContext, NavigatorManager.class);
        default_icon = iconHelper.getIcon ("default" /*"apps/plasma"*/, 32);
    }

//    private void build_toolbar ()
//    {
//        toolbar = new CssLayout();
//
//        CssLayout group = new CssLayout();
//        group.addStyleName("v-component-group");
//
//        Button output_view = new Button ();
//        output_view.setHtmlContentAllowed(true);
//        String ico2 = "<path d=\"M249.649 792.806l-107.776 166.4 11.469 54.426 54.272-11.622 107.725-166.298c-11.469-6.144-22.835-12.698-33.843-19.968-11.162-7.219-21.811-14.95-31.846-22.938zM705.943 734.694c0.717-1.485 1.178-3.123 1.843-4.71 2.714-5.99 5.12-11.981 7.066-18.278 0.307-1.126 0.461-2.253 0.819-3.277 1.997-6.963 3.686-13.824 5.018-20.89 0-0.358 0-0.614 0-1.075 9.984-59.853-7.424-126.618-47.258-186.931l56.832-87.757c65.485 8.346 122.112-8.141 149.35-50.278 47.258-72.858-10.24-194.15-128.256-271.002-118.118-76.902-252.058-80.128-299.213-7.373-27.341 42.189-19.354 100.71 15.002 157.338l-56.934 87.757c-71.117-11.93-139.059-0.819-189.594 32.768-0.307 0.102-0.666 0.205-0.87 0.41-5.888 3.994-11.622 8.397-16.998 13.005-0.87 0.717-1.894 1.382-2.611 2.099-5.018 4.301-9.523 9.114-13.875 13.926-1.024 1.229-2.458 2.304-3.43 3.584-5.427 6.195-10.445 12.749-14.848 19.712-70.861 109.21-10.394 274.483 134.81 369.101 145.306 94.618 320.512 82.637 391.219-26.573 4.454-6.912 8.55-14.131 11.93-21.555zM664.215 224.845c-45.414-29.542-67.584-76.134-49.408-104.243 18.125-28.006 69.683-26.726 114.995 2.816 45.517 29.542 67.482 76.237 49.408 104.243s-69.53 26.726-114.995-2.816z\"></path>";
//        output_view.setCaption("<svg style=\"fill: currentColor; width: 1.5em; margin-top:0.3em;\" viewBox=\"0 0 1024 1024\">" + ico2 + "</svg>");
//        output_view.addStyleName("tiny");
//        group.addComponent (output_view);
//
//        Button run = new Button ();
//        run.setHtmlContentAllowed(true);
//        String ico3 = "<path class=\"path1\" d=\"M192 128l640 384-640 384z\"></path>";
//        run.setCaption("<svg style=\"fill: currentColor; width: 1.5em; margin-top:0.3em;\" viewBox=\"0 0 1024 1024\">" + ico3 + "</svg>");
//        run.addStyleName("tiny");
//        group.addComponent (run);
//
//        run.addClickListener(new Button.ClickListener()
//        {
//            @Override
//            public void buttonClick(Button.ClickEvent clickEvent)
//            {
//                //action_run();
//            }
//        });
//
//        run.addShortcutListener (new AbstractField.FocusShortcut (run,
//                ShortcutAction.KeyCode.ENTER, ShortcutAction.ModifierKey.SHIFT)
//        {
//            @Override
//            public void handleAction (Object sender, Object target)
//            {
//                //action_run();
//            }
//        });
//
//        toolbar.addComponent(group);
//    }

//    private void refresh_treetable ()
//    {
//        log.info ("Refreshing filesystem treetable");
//        treetable.setContainerDataSource (treetable.getContainerDataSource ());
//        treetable.setVisibleColumns (treetable_visible_columns);
//    }

    private void refresh_tree ()
    {
        log.info ("Refreshing filesystem treetable");
//        tree.setContainerDataSource (tree.getContainerDataSource ());
//        treetable.setVisibleColumns (treetable_visible_columns);
    }

    private void deactivate_file_change_watcher ()
    {
        if (watch_service != null)
        {
            try
            {
                watch_service.close ();
            }
            catch (Exception ignore) {};

            watch_service = null;
        }
    }

    private void activate_file_change_watcher (Path userdir)
    {
        try
        {
            watch_service = userdir.getFileSystem ().newWatchService();
            userdir.register (watch_service,
                StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_MODIFY,
                StandardWatchEventKinds.ENTRY_DELETE);
        }
        catch (Exception e)
        {
            // No watch, rely on manual refresh
        }

        if (watch_service != null)
        {
            Thread watch_thread = new Thread ()
            {
                public void run()
                {
                    log.info ("watch_thread START");

                    while (watch_service != null)
                    {
                        log.debug ("poll events");

                        WatchKey watch_key = watch_service.poll ();

                        //log.info ("watch_key > {}", watch_key.isValid ());

                        if (watch_key != null)
                        {
                            for (WatchEvent<?> event : watch_key.pollEvents ())
                            {
                                log.debug ("events ready");

                                WatchEvent.Kind eventKind = event.kind();

                                if (eventKind.equals(StandardWatchEventKinds.OVERFLOW) ||
                                    eventKind.equals(StandardWatchEventKinds.ENTRY_CREATE) ||
                                    eventKind.equals(StandardWatchEventKinds.ENTRY_MODIFY) ||
                                    eventKind.equals(StandardWatchEventKinds.ENTRY_DELETE))
                                {
                                    filesystem_changed = true;
                                    log.debug ("Filesystem changed.");
                                }
                            }

                            // Reset key and check whether it's still valid
                            if (!watch_key.reset())
                            {
                                log.debug ("Watch no longer valid.");
                                watch_key.cancel();
                                deactivate_file_change_watcher ();
                                break;
                            }
                        }

                        try
                        {
                            Thread.sleep (1000);
                        }
                        catch (InterruptedException ignore) {};
                    }

                    log.info ("watch_thread STOP");
                }
            };

            watch_thread.start();

            Thread refresh_thread = new Thread ()
            {
                public void run()
                {
                    log.info ("refresh_thread START");

                    while (watch_service != null)
                    {
                        log.debug ("treetable watcher...");

                        if (filesystem_changed)
                        {
                            // Refresh treetable contents
//                            refresh_treetable ();
                            refresh_tree ();
                            filesystem_changed = false;
                        }

                        try
                        {
                            Thread.sleep (1000);
                        }
                        catch (InterruptedException ignore) {};
                    }

                    log.info ("refresh_thread STOP");
                }
            };

            refresh_thread.start ();
        }

        // Initial treetable refresh
//        refresh_treetable ();
        refresh_tree ();
    }

    @Override
    public void detach ()
    {
        super.detach();
        deactivate_file_change_watcher ();
    }

    private Panel new_shortcuts_panel (String title)
    {
        Panel new_panel = new Panel (title);

        new_panel.setWidth (100, Unit.PERCENTAGE);
        new_panel.setContent (new Label ("Empty."));

        return (new_panel);
    }

    private Map<String, Object> new_component (String name, String directory, String icon_family_name)
    {
        Map<String, Object> component = new HashMap<> ();
        component.put ("iconTitle", name);
        component.put ("directory", directory);
        component.put ("iconUrl", icon_family_name);
        return (component);
    }

    private void view_directory (Map<String, Object> entry)
    {
        String directory = (String)entry.get ("directory");
        String caption = (String)entry.get ("iconTitle");
        Tree tree = active_trees.get (directory);

        if (tree == null)
        {
            CustomFilesystemContainer fs = new CustomFilesystemContainer (Paths.get (directory));
            tree = new Tree ();
            tree.addStyleName ("x-explorerview");
            tree.setContainerDataSource (fs);
            tree.setItemIconPropertyId (FilesystemContainer.PROPERTY_ICON);
            tree.setItemCaptionPropertyId (FilesystemContainer.PROPERTY_NAME);
            tree.setSelectable (false);
            tree.setImmediate (true);
            tree.setHeightUndefined ();
            tree.addItemClickListener (this);
            active_trees.put (directory, tree);
        }

        // Refresh the tree and switch over
        tree.setContainerDataSource (tree.getContainerDataSource ());
        browse_files.setContent (tree);
        browse_files.setCaption (caption);
    }

    private void build_explorer_view ()
    {
        setMargin (true);
        setSpacing (true);

        List<Map<String, Object>> components = new ArrayList<> ();

        if (System.getProperty ("user.conf") != null)
        {
            components.add (new_component ("My Home", System.getProperty ("user.home"), "places/user-home"));
        }
        components.add (new_component ("My Files", userdir.toString (), "places/folder"));
        components.add (new_component ("LucidJ Home", System.getProperty ("system.home"), "places/folder-script"));
        components.add (new_component ("Applications", System.getProperty ("system.home") + "/system/apps", "apps/preferences-desktop-icons"));

//        components.add (new_component ("Your Unbelievable Projects", "org.Buga", "places/folder-development"));
//        components.add (new_component ("Strange Project", "org.Buga", "some awkward thing"));

        Panel bookmarks = new Panel ("Bookmarks");
        bookmarks.setWidth (100, Unit.PERCENTAGE);
        ObjectRenderer component_renderer = rendererFactory.newRenderer (components);
        bookmarks.setContent (component_renderer);
        bookmarks.setWidth (100, Unit.PERCENTAGE);
        addComponent (bookmarks);

        component_renderer.addListener (new Listener ()
        {
            @Override
            public void componentEvent (Event event)
            {
                log.info ("[EVENT] ====> {}", event);

                if (event instanceof LayoutEvents.LayoutClickEvent)
                {
                    LayoutEvents.LayoutClickEvent layoutClickEvent = (LayoutEvents.LayoutClickEvent)event;

                    if (layoutClickEvent.isDoubleClick ())
                    {
                        log.info ("**--DOUBLE-CLICK--** component => {}", layoutClickEvent.getClickedComponent ());
                        fireEvent (layoutClickEvent);
                    }
                }
                else if (event instanceof Button.ClickEvent)
                {
                    Button.ClickEvent clickEvent = (Button.ClickEvent)event;

                    log.info ("**--CLICK--** CLICK! component = {}", clickEvent.getButton ());
                    AbstractComponent c = clickEvent.getButton ();

                    if (c.getData () instanceof Map)
                    {
                        Map<String, Object> data = (Map<String, Object>)c.getData ();
                        log.info ("Component Data = {}", data);

                        if (data.containsKey ("directory"))
                        {
                            log.info ("---> BROWSE DIR: {}", data.get ("directory"));
                            view_directory (data);
                        }
                    }
                }
            }
        });

        HorizontalLayout browse_and_shortcuts = new HorizontalLayout ();
        browse_and_shortcuts.setWidth (100, Unit.PERCENTAGE);
        browse_and_shortcuts.setSpacing (true);

        browse_files = new Panel ("Your files");
        browse_files.setWidth (100, Unit.PERCENTAGE);

        // Show the first directory
        view_directory (components.get (0));

        browse_and_shortcuts.addComponent (browse_files);

        VerticalLayout shortcuts = new VerticalLayout ();
        shortcuts.setSpacing (true);

        Panel today_last_opened = new_shortcuts_panel ("Today");
        shortcuts.addComponent (today_last_opened);

        Panel this_week_last_opened = new_shortcuts_panel ("Yesterday");
        shortcuts.addComponent (this_week_last_opened);

        Panel full_history = new_shortcuts_panel ("History");
        shortcuts.addComponent (full_history);

        browse_and_shortcuts.addComponent (shortcuts);
        addComponent (browse_and_shortcuts);
    }

    @Override // ItemClickEvent.ItemClickListener
    public void itemClick (ItemClickEvent itemClickEvent)
    {
        // FilesystemContainer uses File as item id
        File item_id = ((File)itemClickEvent.getItemId ());
        String item_path = userdir.relativize (item_id.toPath ()).toString ();

        if (item_id.getName ().toLowerCase ().endsWith (".leap"))
        {
            log.info ("OPEN item_path={}", item_id.toPath ().toString ());

            try
            {
                String view_name = OpenView.NAVID + '/' + item_path;

                Map<String, Object> properties = new HashMap<> ();
                properties.put (OpenView.ARTIFACT_URL, item_id.toURI ().toString ());

                navigatorManager.navigateTo (view_name, properties);
            }
            catch (Exception e)
            {
                log.error ("Exception deploying artifact", e);
                //show message
            }
        }
        else if (item_id.isDirectory ())
        {
            // Open/close directory
            if (itemClickEvent.getSource () instanceof Tree)
            {
                Tree tree = (Tree)itemClickEvent.getSource ();

                if (tree.isExpanded (item_id))
                {
                    tree.collapseItem (item_id);
                }
                else
                {
                    tree.expandItem (item_id);
                }
            }
        }
        else
        {
            // Get item relative path
            log.info ("CLICK item_path={}", item_path);
        }
    }

    @Override // View
    public void enter (ViewChangeListener.ViewChangeEvent event)
    {
        log.info ("Enter viewName=" + event.getViewName() + " parameters=" + event.getParameters());

        SecuritySubject subject = security.getSubject ();

        if (subject.isAuthenticated ())
        {
            userdir = subject.getDefaultUserDir ();

            if (getComponentCount() == 0)
            {
                build_explorer_view ();
//                build_toolbar ();
            }

            activate_file_change_watcher (userdir);
        }
    }

    @ServiceObject.Validate
    public void validate ()
    {
        // Nothing
    }

    @ServiceObject.Invalidate
    public void invalidate ()
    {
        // Nothing
    }

    public class CustomFilesystemContainer extends FilesystemContainer implements FilenameFilter
    {
        CustomFilesystemContainer (Path rootDir)
        {
            super (rootDir.toFile (), true);
            setFilter (this);
        }

        @Override // FilenameFilter
        public boolean accept (File dir, String name)
        {
            return (!name.startsWith ("."));
        }

        @Override
        public Property getContainerProperty (Object itemId, Object propertyId)
        {
            if (PROPERTY_ICON.equals (propertyId))
            {
                File itemFile = itemId instanceof File? (File)itemId: null;
                Resource icon = iconHelper.getIcon (iconHelper.getMimeIconDescriptor (itemFile), 32);
                // TODO: CACHE ICON ObjectProperty
                return (new ObjectProperty<> (icon));
            }
            return (super.getContainerProperty (itemId, propertyId));
        }
    }
}

// EOF
