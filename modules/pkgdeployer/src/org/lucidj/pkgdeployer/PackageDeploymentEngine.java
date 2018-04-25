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

package org.lucidj.pkgdeployer;

import org.lucidj.api.core.Artifact;
import org.lucidj.api.core.BundleManager;
import org.lucidj.api.core.DeploymentEngine;
import org.lucidj.api.core.EmbeddingContext;
import org.lucidj.api.core.EmbeddingManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.BundleTracker;
import org.osgi.util.tracker.BundleTrackerCustomizer;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Context;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;

@Component (immediate = true, publicFactory = false)
@Instantiate
@Provides
public class PackageDeploymentEngine implements
    DeploymentEngine, BundleTrackerCustomizer<ServiceRegistration<Artifact>>
{
    private final static Logger log = LoggerFactory.getLogger (PackageDeploymentEngine.class);
    private final static int ENGINE_LEVEL = 50;

    public final static String ATTR_PACKAGE = "X-Package";
    public final static String ATTR_PACKAGE_VERSION = "1.0";

    private String packages_dir;
    private BundleTracker bundleTracker;

    private Map<String, PackageInstance> source_to_instance = new HashMap<> ();

    @Requires
    private BundleManager bundleManager;

    @Requires
    private EmbeddingManager embeddingManager;

    @Context
    private BundleContext context;

    public PackageDeploymentEngine ()
    {
        // TODO: THIS SHOULD BE RECONFIGURABLE
        packages_dir = System.getProperty ("system.data") + "/leap-cache/" + this.getClass ().getPackage ().getName ();

        File check_packages_dir = new File (packages_dir);

        if (!check_packages_dir.exists ())
        {
            if (check_packages_dir.mkdirs ())
            {
                log.info ("Creating cache {}", packages_dir);
            }
            else
            {
                log.error ("Error creating cache {}", packages_dir);
            }
        }
    }

    @Override // DeploymentEngine
    public String getEngineName ()
    {
        return (getClass ().getCanonicalName () + "(" + ENGINE_LEVEL + ")");
    }

    @Override // DeploymentEngine
    public int compatibleArtifact (String location)
    {
        // LEAP requires only the proper package extension
        File location_file = new File (location);
        return (location_file.getName ().toLowerCase ().endsWith (".leap")? ENGINE_LEVEL: 0);
    }

    @Override // DeploymentEngine
    public Artifact install (String source, Properties properties)
        throws Exception
    {
        EmbeddingContext embedding_context = embeddingManager.newEmbeddingContext ();
        PackageInstance instance = new PackageInstance (embedding_context, bundleManager, packages_dir);
        source_to_instance.put (source, instance);
        instance.install (source, properties);
        return (instance);
    }

    @Override // BundleTrackerCustomizer<ServiceRegistration<PackageInstance>>
    public ServiceRegistration<Artifact> addingBundle (Bundle bundle, BundleEvent bundleEvent)
    {
        // TODO: USE JUST compatibleArtifact()
        if (bundle.getHeaders ().get (PackageDeploymentEngine.ATTR_PACKAGE) == null)
        {
            return (null);
        }

        try
        {
            String source = bundleManager.getBundleProperty (bundle, BundleManager.BND_SOURCE, null);

            if (source == null)
            {
                log.error ("Source location not found for {}", bundle);
                return (null);
            }

            PackageInstance instance = source_to_instance.get (source);

            if (instance == null)
            {
                EmbeddingContext embedding_context = embeddingManager.newEmbeddingContext ();
                instance = new PackageInstance (embedding_context, bundleManager, packages_dir);
                instance._setMainBundle (bundle);
            }

            if (!instance.open ())
            {
                log.error ("Error opening package {}", instance.getMainBundle ());
            }

            // The returned bundle must have an Package service
            // registered, so the status changes can be properly tracked
            return (context.registerService (Artifact.class, instance, null));
        }
        catch (Exception e)
        {
            log.error ("Exception adding package bundle: {}", bundle, e);
            return (null);
        }
    }

    @Override // BundleTrackerCustomizer<ServiceRegistration<Artifact>>
    public void modifiedBundle (Bundle bundle, BundleEvent bundleEvent, ServiceRegistration<Artifact> artifact_sreg)
    {
        log.debug ("#####> modifiedBundle (bundle={}, state={}, bundleEvent={} / {}, artifactsr={}",
            bundle, get_state_str (bundle), bundleEvent, get_event_str (bundleEvent), artifact_sreg);
    }

    @Override // BundleTrackerCustomizer<ServiceRegistration<Artifact>>
    public void removedBundle (Bundle bundle, BundleEvent bundleEvent, ServiceRegistration<Artifact> artifact_sreg)
    {
        log.debug ("#####> removedBundle (bundle={}, state={}, bundleEvent={} / {}, artifactsr={}",
            bundle, get_state_str (bundle), bundleEvent, get_event_str (bundleEvent), artifact_sreg);
    }

    private String get_state_str (Bundle bundle)
    {
        switch (bundle.getState ())
        {
            case Bundle.INSTALLED:   return ("INSTALLED");
            case Bundle.RESOLVED:    return ("RESOLVED");
            case Bundle.STARTING:    return ("STARTING");
            case Bundle.STOPPING:    return ("STOPPING");
            case Bundle.ACTIVE:      return ("ACTIVE");
            case Bundle.UNINSTALLED: return ("UNINSTALLED");
        }
        return ("Unknown");
    }

    private String get_event_str (BundleEvent bundleEvent)
    {
        if (bundleEvent == null)
        {
            return ("NULL_EVENT");
        }
        switch (bundleEvent.getType ())
        {
            case BundleEvent.INSTALLED:       return ("INSTALLED");
            case BundleEvent.LAZY_ACTIVATION: return ("LAZY_ACTIVATION");
            case BundleEvent.RESOLVED:        return ("RESOLVED");
            case BundleEvent.STARTED:         return ("STARTED");
            case BundleEvent.STARTING:        return ("STARTING");
            case BundleEvent.STOPPED:         return ("STOPPED");
            case BundleEvent.STOPPING:        return ("STOPPING");
            case BundleEvent.UNINSTALLED:     return ("UNINSTALLED");
            case BundleEvent.UNRESOLVED:      return ("UNRESOLVED");
            case BundleEvent.UPDATED:         return ("UPDATED");
        }
        return ("Unknown");
    }

    @Validate
    private void validate ()
    {
        bundleTracker = new BundleTracker<> (context, Bundle.ACTIVE, this);
        bundleTracker.open();
        log.info (getClass ().getSimpleName () + " started");
    }

    @Invalidate
    private void invalidate ()
    {
        bundleTracker.close ();
        bundleTracker = null;
        log.info (getClass ().getSimpleName () + " stopped");
    }
}

// EOF
