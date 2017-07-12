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
import org.lucidj.api.BundleManager;
import org.lucidj.api.Embedding;
import org.lucidj.api.EmbeddingContext;
import org.lucidj.api.ManagedObject;
import org.lucidj.api.ManagedObjectInstance;
import org.lucidj.api.Package;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public class BundleView extends VerticalLayout implements ManagedObject, View
{
    private final static Logger log = LoggerFactory.getLogger (ExplorerView.class);
    private final static String view_name = "bundle";
    private final static String long_rex = "\\d{1,19}";
    private final static String nav_rex = "^" + view_name + "\\/(" + long_rex + "|[\\-._a-zA-Z0-9]+)";
    public static Pattern NAV_PATTERN = Pattern.compile (nav_rex);

    private ArtifactDeployer artifactDeployer;
    private BundleManager bundleManager;
    private String parameters;
    private BundleContext context;
    private Bundle bundle = null;
    private Package pkg = null;

    private Label parameters_label;

    public BundleView (BundleContext context, BundleManager bundleManager, ArtifactDeployer artifactDeployer)
    {
        this.context = context;
        this.bundleManager = bundleManager;
        this.artifactDeployer = artifactDeployer;
    }

    private void build_toolbar ()
    {
        // No toolbar
    }

    private void update_view ()
    {
        parameters_label.setValue ("Parameters: " + parameters);
    }

    private void build_view ()
    {
        setMargin (true);

        Label private_caption = new Label ("Bundle " + this);
        private_caption.addStyleName ("h2");
        addComponent (private_caption);
        parameters_label = new Label ();
        addComponent (parameters_label);
        addComponent (new Label ("Bundle ID: " + bundle.getBundleId ()));
        addComponent (new Label ("Bundle: " + bundle));

        EmbeddingContext ec = pkg.getEmbeddingContext ();

        // Retrieve all active embeddings/files and print them
        for (Embedding file: ec.getEmbeddedFiles ())
        {
            log.info ("Embedding: [{}] -> {}", file.getName (), file.getObject ());
            addComponent (new Label ("File: <b>" + file.getName () + "</b> (" + file.getObject () + ")", ContentMode.HTML));

            for (Embedding embedding: ec.getEmbeddings (file))
            {
                log.info ("Embedding: [{}] {} -> {}", file.getName (), embedding.getName (), embedding.getObject ());
                addComponent (new Label ("&nbsp;&nbsp;&nbsp;&nbsp;Embedding: <b>" + embedding.getName () + ":</b> " + embedding.getObject (), ContentMode.HTML));

                try
                {
                    URI file_uri = new URI (file.getName ());
                    final String browse_nav = "browse/" + bundle.getSymbolicName () + file_uri.getPath ();
                    Button browse = new Button ("Go to " + embedding.getName ());
                    browse.addClickListener (new Button.ClickListener ()
                    {
                        @Override
                        public void buttonClick (Button.ClickEvent clickEvent)
                        {
                            getUI ().getNavigator ().navigateTo (browse_nav);
                        }
                    });
                    addComponent (browse);
                }
                catch (URISyntaxException e)
                {
                    log.warn ("Embedding exception", e);
                }
            }
        }

        // Implicit on build view
        update_view ();
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

    public boolean init_component (ViewChangeListener.ViewChangeEvent event)
    {
        Matcher m = NAV_PATTERN.matcher (event.getViewName ());
        String bundle_ref = m.find()? m.group (1): null;

        log.info ("bundle_ref = {}", bundle_ref);

        if (bundle_ref == null)
        {
            return (false);
        }

        // TODO: SUPPORT FOR Bundle-Version
        if (bundle_ref.matches (long_rex))
        {
            // The long number is the bundle id
            bundle = context.getBundle (bundle_ref);
            log.info ("bundle Long {} = {}", m.group (), bundle);
        }
        else
        {
            // The text is the BSN
            bundle = bundleManager.getBundleByDescription (bundle_ref, null);
            log.info ("bundle BSN {} = {}", bundle_ref, bundle);
        }

        if (bundle == null)
        {
            return (false);
        }

        ServiceReference[] service_list = bundle.getServicesInUse ();

        // Locate the Package descriptor registered for this bundle
        for (ServiceReference service: service_list)
        {
            if (service.isAssignableTo (bundle, Package.class.getName ()))
            {
                pkg = (Package)context.getService (service);
                break;
            }
        }

        log.info ("Final bundle={}, pkg={}", bundle, pkg);
        return (true);
    }

    @Override // View
    public void enter (ViewChangeListener.ViewChangeEvent event)
    {
        log.info ("Enter viewName=" + event.getViewName() + " parameters=" + event.getParameters());

        parameters = event.getParameters ();

        if (getComponentCount() == 0)
        {
            if (init_component (event))
            {
                build_view ();
                build_toolbar ();
            }
        }
        else
        {
            update_view ();
        }
    }

    public static String buildViewName (long bundle_id)
    {
        return (view_name + "/" + Long.toString (bundle_id));
    }

    public static String buildViewName (String bundle_symbolic_name)
    {
        return (view_name + "/" + bundle_symbolic_name);
    }
}

// EOF