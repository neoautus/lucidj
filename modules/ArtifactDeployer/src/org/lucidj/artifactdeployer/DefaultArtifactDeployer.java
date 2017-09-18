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

package org.lucidj.artifactdeployer;

import org.lucidj.api.Artifact;
import org.lucidj.api.ArtifactDeployer;
import org.lucidj.api.BundleManager;
import org.lucidj.api.DeploymentEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URI;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Version;
import org.apache.felix.ipojo.annotations.Bind;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Context;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Unbind;
import org.apache.felix.ipojo.annotations.Validate;

@Component (immediate = true, publicFactory = false)
@Instantiate
@Provides (specifications = ArtifactDeployer.class)
public class DefaultArtifactDeployer implements ArtifactDeployer, Runnable
{
    private final static Logger log = LoggerFactory.getLogger (DefaultArtifactDeployer.class);

    @Context
    private BundleContext context;

    @Requires
    private BundleManager bundle_manager;

    private Map<String, DeploymentEngine> deployment_engines = new ConcurrentHashMap<> ();

    private Thread poll_thread;
    private int thread_poll_ms = 1000;

    private File get_valid_file (String location)
    {
        // TODO: MOVE THIS TO DeploymentEngine.validBundle()
        if (location.startsWith ("reference:"))
        {
            location = location.substring ("reference:".length ());
        }

        try
        {
            File f = new File (new URI (location));

            if (f.exists () && f.canRead ())
            {
                return (f);
            }
        }
        catch (Exception ignore) {};
        return (null);
    }

    private DeploymentEngine find_deployment_engine (String location)
    {
        DeploymentEngine found_engine = null;
        int level, found_level = 0;

        for (DeploymentEngine engine: deployment_engines.values ())
        {
            if ((level = engine.compatibleArtifact (location)) > found_level)
            {
                found_engine = engine;
                found_level = level;
            }
        }

        return (found_engine);
    }

    private DeploymentEngine get_deployment_engine (Bundle bnd)
        throws IllegalStateException
    {
        Properties properties = bundle_manager.getBundleProperties (bnd);

        if (properties == null)
        {
            throw (new IllegalStateException ("Bundle is unmanaged: " + bnd));
        }

        String deployment_engine_name = properties.getProperty (Artifact.PROP_DEPLOYMENT_ENGINE);

        if (deployment_engine_name == null)
        {
            throw (new IllegalStateException ("Internal error: Missing property: " + Artifact.PROP_DEPLOYMENT_ENGINE));
        }

        DeploymentEngine engine = deployment_engines.get (deployment_engine_name);

        if (engine == null)
        {
            throw (new IllegalStateException ("Deployment Engine not found: " + deployment_engine_name));
        }

        return (engine);
    }

    @Override // ArtifactDeployer
    public Bundle getArtifactByDescription (String symbolic_name, Version version)
    {
        return (bundle_manager.getBundleByDescription (symbolic_name, version));
    }

    @Override // ArtifactDeployer
    public Bundle installArtifact (String location)
        throws Exception
    {
        File bundle_file = get_valid_file (location);

        if (bundle_file == null)
        {
            throw (new Exception ("Invalid artifact: " + location));
        }

        DeploymentEngine deployment_engine = find_deployment_engine (location);

        if (deployment_engine == null)
        {
            throw (new Exception ("Deployer service not found for: " + location));
        }

        // These properties will be stored alongside the bundle and other internal properties
        Properties properties = new Properties ();
        properties.setProperty (Artifact.PROP_DEPLOYMENT_ENGINE, deployment_engine.getEngineName ());
        properties.setProperty (Artifact.PROP_SOURCE, location);

        // Install bundle!
        Bundle new_bundle = deployment_engine.install (location, properties);
        log.info ("Installing package {} from {}", new_bundle, location);
        return (new_bundle);
    }

    @Override // ArtifactDeployer
    public int getState (Bundle bnd)
        throws IllegalStateException // TODO: THROW A CHECKED EXCEPTION INSTEAD!
    {
        return (get_deployment_engine (bnd).getState (bnd));
    }

    @Override // ArtifactDeployer
    public int getExtState (Bundle bnd)
    {
        try
        {
            return (get_deployment_engine (bnd).getExtState (bnd));
        }
        catch (IllegalStateException e)
        {
            log.error ("Exception reading extended state for bundle {}", bnd, e);
            return (Artifact.STATE_EX_ERROR);
        }
    }

    @Override // ArtifactDeployer
    public boolean openArtifact (Bundle bnd)
    {
        try
        {
            return (get_deployment_engine (bnd).open (bnd));
        }
        catch (IllegalStateException e)
        {
            log.error ("Exception opening bundle {}", bnd, e);
            return (false);
        }
    }

    @Override // ArtifactDeployer
    public boolean closeArtifact (Bundle bnd)
    {
        try
        {
            return (get_deployment_engine (bnd).close (bnd));
        }
        catch (IllegalStateException e)
        {
            log.error ("Exception closing bundle {}", bnd, e);
            return (false);
        }
    }

    @Override // ArtifactDeployer
    public boolean updateArtifact (Bundle bnd)
    {
        try
        {
            return (get_deployment_engine (bnd).update (bnd));
        }
        catch (IllegalStateException e)
        {
            log.error ("Exception updating bundle {}", bnd, e);
            return (false);
        }
    }

    @Override // ArtifactDeployer
    public boolean refreshArtifact (Bundle bnd)
    {
        try
        {
            return (get_deployment_engine (bnd).refresh (bnd));
        }
        catch (IllegalStateException e)
        {
            log.error ("Exception refreshing bundle {}", bnd, e);
            return (false);
        }
    }

    @Override // ArtifactDeployer
    public boolean uninstallArtifact (Bundle bnd)
    {
        try
        {
            return (get_deployment_engine (bnd).uninstall (bnd));
        }
        catch (IllegalStateException e)
        {
            log.error ("Exception uninstalling bundle: {}", bnd, e);
            return (false);
        }
    }

    @Override // ArtifactDeployer
    public Bundle getArtifactByLocation (String location)
    {
        return (bundle_manager.getBundleByProperty (Artifact.PROP_SOURCE, location));
    }

    private void poll_repository_for_updates_and_removals ()
    {
        Map<Bundle, Properties> bundles = bundle_manager.getBundles ();

        for (Map.Entry<Bundle, Properties> bundle_entry: bundles.entrySet ())
        {
            String location = bundle_entry.getValue ().getProperty (Artifact.PROP_LOCATION);
            Bundle bundle = bundle_entry.getKey ();

            // TODO: USE DeploymentEngine.validBundle() METHOD INSTEAD
            if (get_valid_file (location) == null)
            {
                // The bundle probably was removed
                uninstallArtifact (bundle);
            }
            else // Bundle file exists, check for changes
            {
                // We only refresh if the bundle is active
                if (bundle.getState () == Bundle.ACTIVE)
                {
                    try
                    {
                        // Refresh the artifact, but ignore if the DeploymentEngine is not available
                        get_deployment_engine (bundle).refresh (bundle);
                    }
                    catch (IllegalStateException ignore) {};
                }
            }
        }
    }

    @Override // Runnable
    public void run ()
    {
        // TODO: MORE GRACEFUL START WHEN LOADING DEPLOYMENT ENGINES +++
        while (!poll_thread.isInterrupted ())
        {
            try
            {
                synchronized (this)
                {
                    log.debug ("Sleeping for {}ms", thread_poll_ms);
                    wait (thread_poll_ms);
                }

                poll_repository_for_updates_and_removals ();
            }
            catch (InterruptedException e)
            {
                // Interrupt status is clear, we should break loop
                break;
            }
            catch (Throwable t)
            {
                try
                {
                    // This will fail if this bundle is uninstalled (zombie)
                    context.getBundle ();
                }
                catch (IllegalStateException e)
                {
                    // This bundle has been uninstalled, exiting loop
                    break;
                }

                // Since this bundle is still valid, something nasty happened...
                log.error ("Package deployment exception", t);
            }
        }
    }

    @Bind (aggregate=true, optional=true, specification = DeploymentEngine.class)
    private void bindDeploymentEngine (DeploymentEngine engine)
    {
        log.info ("Adding deployment engine: {}", engine.getEngineName ());
        deployment_engines.put (engine.getEngineName (), engine);
    }

    @Unbind
    private void unbindDeploymentEngine (DeploymentEngine engine)
    {
        log.info ("Removing deployment engine: {}", engine.getEngineName ());
        deployment_engines.remove (engine.getEngineName ());
    }

    @Validate
    private void validate ()
    {
        // Start things
        poll_thread = new Thread (this);
        poll_thread.setName (this.getClass ().getSimpleName ());
        poll_thread.start ();

        log.info ("DefaultArtifactDeployer started");
    }

    @Invalidate
    private void invalidate ()
    {
        try
        {
            // Stop things, wait 10secs for clean stop
            poll_thread.interrupt ();
            poll_thread.join (10000);
        }
        catch (InterruptedException ignore) {};

        log.info ("DefaultArtifactDeployer stopped");
    }
}

// EOF
