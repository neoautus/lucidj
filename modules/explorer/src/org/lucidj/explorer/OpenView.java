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

import org.lucidj.api.ArtifactDeployer;
import org.lucidj.api.ManagedObject;
import org.lucidj.api.ManagedObjectInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.ui.Button;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import org.osgi.framework.Bundle;

public class OpenView extends VerticalLayout implements ManagedObject, View, Runnable
{
    private final transient Logger log = LoggerFactory.getLogger (ExplorerView.class);

    private ArtifactDeployer artifactDeployer;
    private VerticalLayout install_pane;
    private Thread install_thread;
    private Bundle install_bundle;

    private String artifact_url;
    private String parameters;

    public OpenView (ArtifactDeployer artifactDeployer)
    {
        this.artifactDeployer = artifactDeployer;
    }

    public void setArtifactURL (String artifact_url)
    {
        this.artifact_url = artifact_url;
    }

    private void build_toolbar ()
    {
        // No toolbar
    }

    private void build_view ()
    {
        setMargin (true);

        Label private_caption = new Label ("Open " + this);
        private_caption.addStyleName ("h2");
        addComponent (private_caption);
        addComponent (new Label ("Artifact: " + artifact_url));
        addComponent (new Label ("Parameters: " + parameters));

        install_pane = new VerticalLayout ();
        addComponent (install_pane);
    }

    @Override // Runnable
    public void run ()
    {
        install_pane.addComponent (new Label ("Installing " + artifact_url));

        try
        {
            install_bundle = artifactDeployer.installArtifact (artifact_url);
            install_pane.addComponent (new Label ("Bundle installed: " + install_bundle));

            Button go_to_bundle = new Button ("Go to new bundle");
            go_to_bundle.addClickListener (new Button.ClickListener ()
            {
                @Override
                public void buttonClick (Button.ClickEvent clickEvent)
                {
                    getUI ().getNavigator ().navigateTo (BundleView.buildViewName (install_bundle.getSymbolicName ()));
                }
            });
            install_pane.addComponent (go_to_bundle);
        }
        catch (Exception e)
        {
            String msg = "Exception installing artifact: " + e.toString ();
            String exception_msg = e.getMessage ();
            if (exception_msg != null)
            {
                msg += " (" + exception_msg + ")";
            }
            install_pane.addComponent (new Label (msg));
        }
    }

    private void start_deploy ()
    {
        // First try to locate the artifact already installed
        if ((install_bundle = artifactDeployer.getArtifactByLocation (artifact_url)) == null)
        {
            // Not found, should install
            if (install_thread == null)
            {
                install_thread = new Thread (this);
                install_thread.setName (this.getClass ().getSimpleName ());
                install_thread.start ();
            }
        }
    }

    @Override // ManagedObject
    public void validate (ManagedObjectInstance instance)
    {
        // Nop
    }

    @Override // ManagedObject
    public void invalidate (ManagedObjectInstance instance)
    {
        // Nop
    }

    @Override // View
    public void enter (ViewChangeListener.ViewChangeEvent event)
    {
        log.info ("Enter viewName=" + event.getViewName() + " parameters=" + event.getParameters());

        parameters = event.getParameters ();

        if (getComponentCount() == 0)
        {
            build_view ();
            build_toolbar ();
            start_deploy ();
        }

        if (install_bundle != null)
        {
            log.info ("Redirecting {} to {}", event.getViewName (), install_bundle);
            event.getNavigator ().navigateTo (BundleView.buildViewName (install_bundle.getSymbolicName ()));
        }
    }
}

// EOF