/*
 * Copyright 2016 NEOautus Ltd. (http://neoautus.com)
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

import org.lucidj.api.BundleDeployer;
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

@Component (immediate = true)
@Instantiate
@Provides (specifications = BundleDeployer.class)
public class DefaultBundleDeployer implements BundleDeployer, Runnable
{
    private final static transient Logger log = LoggerFactory.getLogger (DefaultBundleDeployer.class);

    @Context
    private BundleContext context;

    @Requires
    private BundleManager bundle_manager;

    private final static String DBD_DEPLOYMENT_ENGINE = "deployment-engine";
    private final static String DBD_DEPLOYMENT_SOURCE = "deployment-source";

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

        String deployment_engine_name = properties.getProperty (DBD_DEPLOYMENT_ENGINE);

        if (deployment_engine_name == null)
        {
            throw (new IllegalStateException ("Internal error: Missing property: " + DBD_DEPLOYMENT_ENGINE));
        }

        DeploymentEngine engine = deployment_engines.get (deployment_engine_name);

        if (engine == null)
        {
            throw (new IllegalStateException ("Deployment Engine not found: " + deployment_engine_name));
        }

        return (engine);
    }

    @Override // BundleDeployer
    public Bundle getBundleByDescription (String symbolic_name, Version version)
    {
        return (bundle_manager.getBundleByDescription (symbolic_name, version));
    }

    @Override // BundleDeployer
    public Bundle installBundle (String location)
    {
        Bundle new_bundle = null;

        try
        {
            File bundle_file = get_valid_file (location);

            if (bundle_file == null)
            {
                return (null);
            }

            DeploymentEngine deployment_engine = find_deployment_engine (location);

            if (deployment_engine == null)
            {
                log.error ("Package deployer not found for: {}", location);
                return (null);
            }

            // These properties will be stored alongside the bundle and other internal properties
            Properties properties = new Properties ();
            properties.setProperty (DBD_DEPLOYMENT_ENGINE, deployment_engine.getEngineName ());
            properties.setProperty (DBD_DEPLOYMENT_SOURCE, location);

            // Install bundle!
            new_bundle = deployment_engine.installBundle (location, properties);
            log.info ("Installing package {} from {}", new_bundle, location);
        }
        catch (Exception e)
        {
            log.error ("Exception on package install: {}", location, e);
        }
        return (new_bundle);
    }

    @Override // BundleDeployer
    public boolean updateBundle (Bundle bnd)
    {
        try
        {
            return (get_deployment_engine (bnd).updateBundle (bnd));
        }
        catch (IllegalStateException e)
        {
            log.error ("Exception updating bundle {}", bnd, e);
            return (false);
        }
    }

    @Override // BundleDeployer
    public boolean refreshBundle (Bundle bnd)
    {
        try
        {
            return (get_deployment_engine (bnd).refreshBundle (bnd));
        }
        catch (IllegalStateException e)
        {
            log.error ("Exception refreshing bundle {}", bnd, e);
            return (false);
        }
    }

    @Override // BundleDeployer
    public boolean uninstallBundle (Bundle bnd)
    {
        try
        {
            return (get_deployment_engine (bnd).uninstallBundle (bnd));
        }
        catch (IllegalStateException e)
        {
            log.error ("Exception uninstalling bundle: {}", bnd, e);
            return (false);
        }
    }

    @Override // BundleDeployer
    public Bundle getBundleByLocation (String location)
    {
        return (bundle_manager.getBundleByProperty (DBD_DEPLOYMENT_SOURCE, location));
    }

    private void poll_repository_for_updates_and_removals ()
    {
        Map<Bundle, Properties> bundles = bundle_manager.getBundles ();

        for (Map.Entry<Bundle, Properties> bundle_entry: bundles.entrySet ())
        {
            String location = bundle_entry.getValue ().getProperty (BundleManager.PROP_LOCATION);
            Bundle bundle = bundle_entry.getKey ();

            // TODO: USE DeploymentEngine.validBundle() METHOD INSTEAD
            if (get_valid_file (location) == null)
            {
                // The bundle probably was removed
                uninstallBundle (bundle);
            }
            else // Bundle file exists, check for changes
            {
                // We only refresh if the bundle is active
                if (bundle.getState () == Bundle.ACTIVE)
                {
                    refreshBundle (bundle);
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

        log.info ("DefaultBundleDeployer started");
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

        log.info ("DefaultBundleDeployer stopped");
    }
}

// EOF
