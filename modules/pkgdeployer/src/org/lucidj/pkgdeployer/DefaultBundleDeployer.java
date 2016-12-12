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
import org.lucidj.api.DeploymentEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.Attributes;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Version;
import org.apache.felix.ipojo.annotations.Bind;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Context;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Unbind;
import org.apache.felix.ipojo.annotations.Validate;

@Component (immediate = true)
@Instantiate
@Provides (specifications = BundleDeployer.class)
public class DefaultBundleDeployer implements BundleDeployer, BundleListener, Runnable
{
    private final static transient Logger log = LoggerFactory.getLogger (DefaultBundleDeployer.class);

    @Context
    private BundleContext context;

    private final static String DBD_DEPLOYMENT_ENGINE = "deployment-engine";
    private final static String DBD_LOCATION = "location";
    private final static String DBD_LAST_MODIFIED = "last-modified";
    private final static String DBD_BUNDLE_STATE = "bundle-status";
    private final static String DBD_BUNDLE_STATE_HUMAN = "bundle-status-human";

    private String cache_dir;

    private Map<String, DeploymentEngine> deployment_engines = new ConcurrentHashMap<> ();
    private Map<String, Properties> bundle_prop_cache = new ConcurrentHashMap<> ();
    private Thread poll_thread;
    private int thread_poll_ms = 1000;

    public DefaultBundleDeployer ()
    {
        cache_dir = System.getProperty ("rq.home") + "/cache/" + this.getClass ().getSimpleName ();

        File check_cache_dir = new File (cache_dir);

        if (!check_cache_dir.exists ())
        {
            if (check_cache_dir.mkdir ())
            {
                log.info ("Creating cache {}", cache_dir);
            }
            else
            {
                log.error ("Error creating cache {}", cache_dir);
            }
        }
    }

    private File get_bundle_data_file (String location)
    {
        // <Sanitized location>.properties
        return (new File (cache_dir + "/" + location.replaceAll("\\p{P}", "_") + ".properties"));
    }

    private Properties load_properties (File properties_file)
    {
        try
        {
            Properties properties = new Properties ();
            properties.load (new FileInputStream (properties_file));
            bundle_prop_cache.put (properties.getProperty (DBD_LOCATION), properties);
            return (properties);
        }
        catch (Exception e)
        {
            return (null);
        }
    }

    private void populate_cache ()
    {
        File[] bundle_list = new File (cache_dir).listFiles ();

        if (bundle_list == null)
        {
            log.error ("Error reading cache {}", cache_dir);
            return;
        }

        // TODO: PROP FILE CLEANUP FOR UNUSED BUNDLES (LastModified > N minutes)
        for (File bundle_data_file: bundle_list)
        {
            load_properties (bundle_data_file);
        }
    }

    private boolean store_properties (String location, Properties properties)
    {
        bundle_prop_cache.put (location, properties);

        try
        {
            // Always store location so we can populate the bundle cache properly
            properties.setProperty (DBD_LOCATION, location);
            properties.store (new FileOutputStream (get_bundle_data_file (location)), null);
            return (true);
        }
        catch (Exception e)
        {
            return (false);
        }
    }

    private String get_state_string (int state)
    {
        switch (state)
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

    @Override // BundleListener
    public void bundleChanged (BundleEvent bundleEvent)
    {
        String msg = "Peace";
        Bundle bnd = bundleEvent.getBundle ();
        String location = bnd.getLocation ();

        // Is this bundle managed by us?
        if (!bundle_prop_cache.containsKey (location))
        {
            // Nope
            return;
        }

        // Keep bundle state
        Properties properties = bundle_prop_cache.get (location);
        properties.setProperty (DBD_BUNDLE_STATE, Integer.toString (bnd.getState ()));
        properties.setProperty (DBD_BUNDLE_STATE_HUMAN, get_state_string (bnd.getState ()));
        store_properties (location, properties);

        switch (bundleEvent.getType ())
        {
            case BundleEvent.INSTALLED:
            {
                // This forces framework to try to get bundle resolved
                bnd.getResource ("META-INF/MANIFEST.MF");
                log.info ("Bundle {} installed -- trying to resolve", bnd);
                msg = "INSTALLED";
                break;
            }
            case BundleEvent.LAZY_ACTIVATION:
            {
                msg = "LAZY_ACTIVATION";
                break;
            }
            case BundleEvent.RESOLVED:
            {
                try
                {
                    log.info ("Bundle {} is resolved -- will start now", bnd);
                    bnd.start ();
                }
                catch (Exception e)
                {
                    log.info ("Exception starting bundle {}", bnd, e);
                }
                msg = "RESOLVED";
                break;
            }
            case BundleEvent.STARTED:
            {
                log.info ("Bundle {} is now ACTIVE", bnd);
                msg = "STARTED";
                break;
            }
            case BundleEvent.STARTING:
            {
                msg = "STARTING";
                break;
            }
            case BundleEvent.STOPPED:
            {
                msg = "STOPPED";
                break;
            }
            case BundleEvent.STOPPING:
            {
                msg = "STOPPING";
                break;
            }
            case BundleEvent.UNINSTALLED:
            {
                msg = "UNINSTALLED";
                break;
            }
            case BundleEvent.UNRESOLVED:
            {
                msg = "UNRESOLVED";
                break;
            }
            case BundleEvent.UPDATED:
            {
                // This forces framework to try to get bundle resolved
                bnd.getResource ("META-INF/MANIFEST.MF");
                log.info ("Bundle {} updated -- trying to resolve", bnd);
                msg = "UPDATED";
                break;
            }
        }

        log.debug ("bundleChanged: {} eventType={} state={}", bnd, msg, get_state_string (bnd.getState ()));
    }

    @Override // BundleDeployer
    public Manifest getManifest (File jar_file)
    {
        FileInputStream jar_stream = null;

        try
        {
            jar_stream = new FileInputStream (jar_file);
            JarInputStream jarStream = new JarInputStream (jar_stream);
            return (jarStream.getManifest ());
        }
        catch (IOException e)
        {
            return (null);
        }
        finally
        {
            if (jar_stream != null)
            {
                try
                {
                    jar_stream.close ();
                }
                catch (Exception ignore) {};
            }
        }
    }

    @Override // BundleDeployer
    public Manifest getManifest (String location)
    {
        try
        {
            return (getManifest (new File (new URI (location))));
        }
        catch (URISyntaxException e)
        {
            return (null);
        }
    }

    @Override // BundleDeployer
    public Bundle getBundleByDescription (String symbolic_name, Version version)
    {
        // TODO: ADD CACHE
        Bundle latest_bundle = null;
        Version latest_version = null;
        Bundle[] bundles = context.getBundles ();

        for (Bundle bundle: bundles)
        {
            if (symbolic_name.equals (bundle.getSymbolicName ()))
            {
                Version bundle_version = bundle.getVersion ();

                if (version != null)
                {
                    if (version.equals (bundle_version))
                    {
                        return (bundle);
                    }
                }
                else // version == null, find the latest bundle
                {
                    if (latest_version == null ||
                        latest_version.compareTo (bundle_version) == -1)
                    {
                        // A newer version was found
                        latest_bundle = bundle;
                        latest_version = bundle_version;
                    }
                }
            }
        }

        return (latest_bundle);
    }

    private File get_valid_file (String location)
    {
        try
        {
            File f = new File (new URI (location));

            if (f != null && f.exists () && f.isFile () && f.canRead ())
            {
                return (f);
            }
        }
        catch (Exception ignore) {};
        return (null);
    }

    private DeploymentEngine get_deployment_engine (String location)
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
        Properties properties = bundle_prop_cache.get (bnd.getLocation ());

        if (properties == null)
        {
            throw (new IllegalStateException ("Bundle is unmanaged: " + bnd.getLocation ()));
        }

        String deployment_engine_name = properties.getProperty (DBD_DEPLOYMENT_ENGINE);

        if (deployment_engine_name == null)
        {
            throw (new IllegalStateException ("Internal error: Missing directive: " + DBD_DEPLOYMENT_ENGINE));
        }

        DeploymentEngine engine = deployment_engines.get (deployment_engine_name);

        if (engine == null)
        {
            throw (new IllegalStateException ("Deployment Engine not found: " + deployment_engine_name));
        }

        return (engine);
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

            DeploymentEngine deployment_engine = get_deployment_engine (location);

            if (deployment_engine == null)
            {
                return (null);
            }

            // Fetch base bundle description
            Manifest mf = getManifest (bundle_file);
            Attributes attrs = mf.getMainAttributes ();
            String symbolic_name = attrs.getValue ("Bundle-SymbolicName");
            Version version = new Version (attrs.getValue ("Bundle-Version"));

            if ((new_bundle = getBundleByDescription (symbolic_name, version)) != null)
            {
                if (location.equals (new_bundle.getLocation ()))
                {
                    log.info ("Bundle {} already installed (location: {})", new_bundle, location);
                }
                else
                {
                    log.info ("Bundle {} installed from other location (proposed: {}, original: {})",
                        new_bundle, location, new_bundle.getLocation ());
                }
                return (new_bundle);
            }

            // Add bundle properties to repository, so we can manage it
            Properties properties = new Properties ();
            properties.setProperty (DBD_DEPLOYMENT_ENGINE, deployment_engine.getEngineName ());
            properties.setProperty (DBD_LAST_MODIFIED, Long.toString (bundle_file.lastModified ()));
            properties.setProperty (DBD_BUNDLE_STATE, Integer.toString (Bundle.UNINSTALLED));
            store_properties (location, properties);

            // Install bundle
            new_bundle = deployment_engine.installBundle (location);
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
            log.error ("Exception updating bundle {}", e);
            return (false);
        }
    }

    @Override // BundleDeployer
    public boolean refreshBundle (Bundle bnd)
    {
        String location = bnd.getLocation ();
        File bundle_file = get_valid_file (location);

        if (bundle_file == null || !bundle_prop_cache.containsKey (location))
        {
            // The origin is not available, probably was deleted
            return (false);
        }

        Properties properties = bundle_prop_cache.get (location);
        long bundle_lastmodified = Long.parseLong (properties.getProperty (DBD_LAST_MODIFIED));

        if (bundle_lastmodified != bundle_file.lastModified ())
        {
            log.debug ("Modified ==> bnd={} bnd.getLastModified={} bnd_file.lastModified={}",
                bnd, bundle_lastmodified, bundle_file.lastModified ());

            if (updateBundle (bnd))
            {
                properties.setProperty (DBD_LAST_MODIFIED, Long.toString (bundle_file.lastModified ()));
                store_properties (location, properties);
                return (true);
            }
        }

        return false;
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
            log.error ("Exception updating bundle: {}", bnd, e);
            return (false);
        }
    }

    @Override // BundleDeployer
    public Bundle getBundleByLocation (String location)
    {
        if (bundle_prop_cache.containsKey (location))
        {
            return (context.getBundle (location));
        }

        // Either the bundle doesn't exists or it's uninstalled
        return (null);
    }

    private void poll_repository_for_updates_and_removals ()
    {
        // TODO: CHECK STALE bundle_prop_cache ENTRY
        for (Map.Entry<String, Properties> bnd_entry: bundle_prop_cache.entrySet())
        {
            String location = bnd_entry.getKey ();
            Bundle bnd = getBundleByLocation (location);

            if (bnd == null)
            {
                // Bundle already uninstalled
                continue;
            }

            if (get_valid_file (location) == null)
            {
                // The bundle probably was removed
                uninstallBundle (bnd);
            }
            else // Bundle file exists, check for changes
            {
                // We only refresh if the bundle is active
                if (bnd.getState () == Bundle.ACTIVE)
                {
                    refreshBundle (bnd);
                }
            }
        }
    }

    @Override
    public void run ()
    {
        while (!poll_thread.isInterrupted ())
        {
            try
            {
                poll_repository_for_updates_and_removals ();

                synchronized (this)
                {
                    log.debug ("Sleeping for {}ms", thread_poll_ms);
                    wait (thread_poll_ms);
                }
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
        // Load references to all bundles we manage
        populate_cache ();

        // Start listening to bundle events
        context.addBundleListener (this);

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

        // Stop listening to bundle events
        context.removeBundleListener (this);

        log.info ("DefaultBundleDeployer stopped");
    }
}

// EOF
