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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Context;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Validate;

@Component (immediate = true)
@Instantiate
@Provides (specifications = BundleDeployer.class)
public class DefaultBundleDeployer implements BundleDeployer, BundleListener, Runnable
{
    private final static transient Logger log = LoggerFactory.getLogger (DefaultBundleDeployer.class);

    @Context
    private BundleContext context;

    private final static String DBD_LAST_MODIFIED = "last-modified";
    private final static String DBD_BUNDLE_STATE = "bundle-status";

    private Map<String, Properties> bundle_prop_repository = new HashMap<> ();
    private Thread poll_thread;
    private int thread_poll_ms = 1000;

    private File get_bundle_data_file (String location)
    {
        // <Sanitized location>.properties
        return (context.getDataFile (location.replaceAll("\\p{P}", "_") + ".properties"));
    }

    private Properties load_properties (String location)
    {
        try
        {
            Properties properties = new Properties ();
            properties.load (new FileInputStream (get_bundle_data_file (location)));
            return (properties);
        }
        catch (Exception e)
        {
            return (null);
        }
    }

    private Properties get_properties (String location)
    {
        Properties properties = bundle_prop_repository.get (location);

        if (properties == null)
        {
            properties = load_properties (location);
        }

        if (properties != null)
        {
            bundle_prop_repository.put (location, properties);
        }

        return (properties);
    }

    private boolean store_properties (String location, Properties properties)
    {

        bundle_prop_repository.put (location, properties);

        try
        {
            properties.store (new FileOutputStream (get_bundle_data_file (location)), "Automatically generated.");
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
        Properties properties = get_properties (location);

        // Is this bundle managed by us?
        if (properties == null)
        {
            // Nope
            return;
        }

        // Keep bundle state
        properties.setProperty (DBD_BUNDLE_STATE, Integer.toString (bnd.getState ()));
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

    private boolean valid_file (File f)
    {
        // Basic check
        return (f != null && f.exists () && f.isFile () && f.canRead ());
    }

    @Override // BundleDeployer
    public Bundle installBundle (String location)
    {
        Bundle new_bundle = null;

        try
        {
            File bundle_file = new File (new URI (location));

            if (!valid_file (bundle_file))
            {
                return (null);
            }

            // Add bundle properties to repository, so we can manage it
            Properties properties = new Properties ();
            properties.setProperty (DBD_LAST_MODIFIED, Long.toString (bundle_file.lastModified ()));
            properties.setProperty (DBD_BUNDLE_STATE, Integer.toString (Bundle.UNINSTALLED));
            store_properties (location, properties);

            // Install bundle
            new_bundle = context.installBundle (location);
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
            log.info ("Updating package {}", bnd);
            bnd.stop (Bundle.STOP_TRANSIENT);
            bnd.update ();
            return (true);
        }
        catch (Exception e)
        {
            log.error ("Error updating {}", bnd, e);
            // TODO: CHECK STALE managed_bundles ENTRY
            uninstallBundle (bnd);
        }
        return (false);
    }

    @Override // BundleDeployer
    public boolean refreshBundle (Bundle bnd)
    {
        String location = bnd.getLocation ();
        File bundle_file = null;

        try
        {
            bundle_file = new File (new URI (location));
        }
        catch (Exception ignore) {};

        if (bundle_file == null)
        {
            // The origin is not available, probably was deleted
            return (false);
        }

        Properties properties = get_properties (location);
        long bundle_lastmodified = Long.parseLong (properties.getProperty (DBD_LAST_MODIFIED));

        if (bundle_lastmodified != bundle_file.lastModified ())
        {
            log.debug ("Modified ==> bnd={} bnd.getLastModified={} bnd_file.lastModified={}",
                bnd, bundle_lastmodified, bundle_file.lastModified ());

            if (updateBundle (bnd))
            {
                properties = get_properties (location);
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
            // TODO: PROP FILE CLEANUP
            log.info ("Uninstalling bundle {}", bnd);

            String location = bnd.getLocation ();
            bnd.uninstall ();
            bundle_prop_repository.remove (location);
            return (true);
        }
        catch (Exception e)
        {
            log.error ("Exception uninstalling {}", bnd, e);
        }
        return (false);
    }

    @Override // BundleDeployer
    public Bundle getDeployedBundle (String location)
    {
        Bundle bnd = context.getBundle (location);

        return ((bnd != null && get_properties (location) != null)? bnd: null);
    }

    private void poll_repository_for_updates_and_removals ()
    {
        for (Map.Entry<String, Properties> bnd_entry: bundle_prop_repository.entrySet())
        {
            String location = bnd_entry.getKey ();
            Bundle bnd = getDeployedBundle (location);
            File bundle_file = null;

            try
            {
                bundle_file = new File (new URI (location));
            }
            catch (URISyntaxException ignore) {};

            if (bundle_file == null || !valid_file (bundle_file))
            {
                // The bundle probably was removed
                uninstallBundle (bnd);
            }
            else // All ok, check for changes
            {
                refreshBundle (bnd);
            }
        }
    }

    @Override
    public void run ()
    {
        while (!Thread.interrupted ())
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
                // Nothing, will check stop_thread
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

    @Validate
    private void validate ()
    {
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
