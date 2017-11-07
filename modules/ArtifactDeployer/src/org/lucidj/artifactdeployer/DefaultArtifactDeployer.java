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

import org.lucidj.api.ArtifactDeployer;
import org.lucidj.api.BundleManager;
import org.lucidj.api.DeploymentEngine;
import org.lucidj.api.DeploymentInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URI;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
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
    private Map<Bundle, DeploymentInstance> bundle_to_instance = new ConcurrentHashMap<> (); // TODO: REMOVE THIS
    private Map<String, DeploymentInstance> location_to_instance = new ConcurrentHashMap<> ();

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

    @Override // ArtifactDeployer
    public DeploymentInstance installArtifact (String location)
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
        properties.setProperty (Constants.PROP_DEPLOYMENT_ENGINE, deployment_engine.getEngineName ());
        properties.setProperty (Constants.PROP_SOURCE, location);

        // Install bundle!
        DeploymentInstance new_deploy = deployment_engine.install (location, properties);
        bundle_to_instance.put (new_deploy.getMainBundle (), new_deploy);
        location_to_instance.put (location, new_deploy);
        log.info ("Installing package {} from {}", new_deploy, location);

        // Register the bundle controller
        Dictionary<String, Object> props = new Hashtable<> ();
        props.put ("@location", location);
        props.put ("@engine", deployment_engine.getEngineName ());
        props.put ("@bundleid", new_deploy.getMainBundle ().getBundleId ());
        props.put ("@bsn", new_deploy.getMainBundle ().getSymbolicName ());
        context.registerService (DeploymentInstance.class, new_deploy, props);
        return (new_deploy);
    }

    @Override
    public DeploymentInstance getDeploymentInstance (Bundle bundle)
    {
        return (bundle_to_instance.get (bundle));
    }

    @Override // ArtifactDeployer
    public DeploymentInstance getArtifactByLocation (String location)
    {
        return (location_to_instance.get (location));
    }

    private void poll_repository_for_updates_and_removals ()
    {
        Map<Bundle, Properties> bundles = bundle_manager.getBundles ();

        for (Map.Entry<Bundle, Properties> bundle_entry: bundles.entrySet ())
        {
            String location = bundle_entry.getValue ().getProperty (Constants.PROP_LOCATION);
            Bundle bundle = bundle_entry.getKey ();
            DeploymentInstance instance = bundle_to_instance.get (bundle);

            if (instance == null)
            {
                // Not managed by us
                continue;
            }

            // TODO: USE DeploymentEngine.validBundle() METHOD INSTEAD
            if (get_valid_file (location) == null)
            {
                // The bundle probably was removed
                instance.uninstall ();
            }
            else // Bundle file exists, check for changes
            {
                // We only refresh if the bundle is active
                if (bundle.getState () == Bundle.ACTIVE)
                {
                    try
                    {
                        // Refresh the artifact, but ignore if the DeploymentEngine is not available
                        instance.refresh ();
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
