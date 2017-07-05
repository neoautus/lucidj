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
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.osgi.framework.BundleContext;

public class BundleView extends VerticalLayout implements ManagedObject, View
{
    private final static Logger log = LoggerFactory.getLogger (ExplorerView.class);
    private final static String view_name = "bundle";
    public static Pattern NAV_PATTERN = Pattern.compile ("^" + view_name + "\\/(\\d{1,19})");

    private ArtifactDeployer artifactDeployer;
    private String parameters;
    private long bundle_id;
    private BundleContext context;

    public BundleView (BundleContext context, ArtifactDeployer artifactDeployer)
    {
        this.context = context;
        this.artifactDeployer = artifactDeployer;
    }

    private void build_toolbar ()
    {
        // No toolbar
    }

    private void build_view ()
    {
        setMargin (true);

        Label private_caption = new Label ("Bundle " + this);
        private_caption.addStyleName ("h2");
        addComponent (private_caption);
        addComponent (new Label ("Parameters: " + parameters));
        addComponent (new Label ("Bundle ID: " + bundle_id));
        addComponent (new Label ("Bundle: " + context.getBundle (bundle_id)));
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
        Matcher m = NAV_PATTERN.matcher (event.getViewName ());
        bundle_id = m.find()? Long.parseLong (m.group (1)): -1;

        if (getComponentCount() == 0)
        {
            build_view ();
            build_toolbar ();
        }
    }

    public static String buildViewName (long bundle_id)
    {
        return (view_name + "/" + Long.toString (bundle_id));
    }
}

// EOF
