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

import org.lucidj.api.ManagedObject;
import org.lucidj.api.ManagedObjectInstance;
import org.lucidj.api.SecurityEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.lucidj.uiaccess.UIAccess;

import com.vaadin.data.util.FilesystemContainer;
import com.vaadin.event.ItemClickEvent;
import com.vaadin.event.ShortcutAction;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.ui.AbstractField;
import com.vaadin.ui.Button;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.TreeTable;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;

public class ExplorerView extends VerticalLayout implements ManagedObject, View, ItemClickEvent.ItemClickListener
{
    private final transient Logger log = LoggerFactory.getLogger (ExplorerView.class);

    private SecurityEngine security;
    private CssLayout toolbar = null;

    private final VerticalLayout self = this;
    private Path userdir;
    private transient boolean filesystem_changed = false;
    private TreeTable treetable;
    private Object[] treetable_visible_columns;
    private transient WatchService watch_service;

    public ExplorerView (SecurityEngine security)
    {
        this.security = security;
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

    private void refresh_treetable ()
    {
        log.info ("Refreshing filesystem treetable");
        treetable.setContainerDataSource (treetable.getContainerDataSource ());
        treetable.setVisibleColumns (treetable_visible_columns);
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
                            new UIAccess (self)
                            {
                                @Override
                                public void updateUI()
                                {
                                    // Refresh treetable contents
                                    refresh_treetable ();
                                    filesystem_changed = false;
                                }
                            };
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
        refresh_treetable ();
    }

    @Override
    public void detach ()
    {
        super.detach();
        deactivate_file_change_watcher ();
    }

    private void build_explorer_view ()
    {
        setMargin (true);

        Label private_caption = new Label ("Home");
        private_caption.addStyleName ("h2");
        addComponent (private_caption);

        FilesystemContainer fc = new FilesystemContainer (userdir.toFile ());

        treetable = new TreeTable();
        treetable.setContainerDataSource (fc);
        treetable.setItemIconPropertyId ("Icon");
        treetable.setWidth ("100%");
        treetable.setSelectable (false);
        treetable.setImmediate (true);
        treetable.setPageLength (0);
        treetable.setHeightUndefined ();
        treetable.addItemClickListener (this);
        treetable_visible_columns = new Object[] { "Name", "Size", "Last Modified" };
        addComponent (treetable);
    }

    @Override // ItemClickEvent.ItemClickListener
    public void itemClick (ItemClickEvent itemClickEvent)
    {
        // FilesystemContainer uses File as item id
        File item_id = ((File)itemClickEvent.getItemId ());

        if (item_id.isDirectory ())
        {
            // Open/close directory
            treetable.setCollapsed (item_id, !treetable.isCollapsed (item_id));
        }
        else
        {
            // Get item relative path
            String item_path = userdir.relativize (item_id.toPath ()).toString ();

            log.info ("OPEN item_path={}", item_path);

            UI.getCurrent().getNavigator().navigateTo ("formulas:" + item_path);
        }
    }

    @Override // View
    public void enter (ViewChangeListener.ViewChangeEvent event)
    {
        log.info ("Enter viewName=" + event.getViewName() + " parameters=" + event.getParameters());

        if (security.getSubject ().isAuthenticated ())
        {
            userdir = security.getDefaultUserDir ();

            if (getComponentCount() == 0)
            {
                build_explorer_view ();
                build_toolbar ();
            }

            activate_file_change_watcher (userdir);
        }
    }

    @Override
    public void validate (ManagedObjectInstance instance)
    {
        // Nothing
    }

    @Override
    public void invalidate (ManagedObjectInstance instance)
    {
        // Nothing
    }
}

// EOF
