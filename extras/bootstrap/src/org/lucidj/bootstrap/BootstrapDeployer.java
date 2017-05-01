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

package org.lucidj.bootstrap;

import org.apache.karaf.features.BootFinished;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.wiring.BundleRevision;

public class BootstrapDeployer extends Thread implements BundleListener
{
    TinyLog log = new TinyLog ();

    private BundleContext context;

    private String watched_directory;
    private int poll_ms = 2000;
    private boolean initial_delay_flag = false;
    private int initial_delay_ms = 3000;

    private Map<String, Bundle> managed_bundles = new ConcurrentHashMap<> ();
    private Set<Bundle> installing_bundles = new HashSet<> ();
    private Set<Bundle> linked_bundles = new HashSet<> ();

    private volatile boolean bootstrap_finished;

    public BootstrapDeployer (BundleContext context)
    {
        this.context = context;
        this.context.addBundleListener (this);
        setName (this.context.getBundle ().getSymbolicName ());

        // TODO: (IN)SANITY CHECKS
        watched_directory = System.getProperty ("system.home") + "/runtime/system";
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

    private boolean valid_file (File f)
    {
        return (f != null && f.exists () && f.isFile () && f.canRead ());
    }

    private boolean remove_bundle (Bundle bnd)
    {
        try
        {
            log.info ("Uninstalling bundle {}", bnd);
            bnd.uninstall ();
            return (true);
        }
        catch (Exception e)
        {
            log.error ("Error uninstalling {}", bnd, e);
            return (false);
        }
    }

    private File get_bundle_data_file (Bundle bnd)
    {
        return (bnd.getDataFile (Long.toString (bnd.getBundleId ()) + ".internal.lastModified"));
    }

    private void store_lastmodified ( Bundle bnd, long lastmodified)
    {
        File bdf = get_bundle_data_file (bnd);
        DataOutputStream out = null;

        try
        {
            out = new DataOutputStream (new FileOutputStream (bdf));
            out.writeLong(lastmodified);
        }
        catch ( Exception e )
        {
            // TODO: CHECK PROPER BEHAVIOUR
            log.error ("Error setting lastmodified for {}", bnd, e);
        }
        finally
        {
            if (out != null)
            {
                try
                {
                    out.close();
                }
                catch (IOException ignore) {};
            }
        }
    }

    private long load_lastmodified (Bundle bnd)
    {
        File bdf = get_bundle_data_file (bnd);
        DataInputStream in = null;

        try
        {
            in = new DataInputStream (new FileInputStream (bdf));
            return (in.readLong());
        }
        catch ( Exception e )
        {
            log.error ("Error setting lastmodified for {}", bnd, e);
        }
        finally
        {
            if (in != null)
            {
                try
                {
                    in.close();
                }
                catch (IOException ignore) {};
            }
        }

        return (Long.MIN_VALUE);
    }

    private boolean is_fragment (Bundle bnd)
    {
        return ((bnd.adapt (BundleRevision.class).getTypes() & BundleRevision.TYPE_FRAGMENT) != 0);
    }

    private boolean update_bundle (Bundle bnd)
    {
        try
        {
            if (!is_fragment (bnd))
            {
                bnd.stop (Bundle.STOP_TRANSIENT);
            }
            log.info ("Updating bundle {}", bnd);
            bnd.update ();
            return (true);
        }
        catch (Exception e)
        {
            log.error ("Error updating {}", bnd, e);
            // TODO: CHECK STALE managed_bundles ENTRY
            remove_bundle (bnd);
            return (false);
        }
    }

    private void update_if_changed (Bundle bnd, File bnd_file)
    {
        long bundle_lastmodified = load_lastmodified (bnd);

        if (bundle_lastmodified != bnd_file.lastModified ())
        {
            log.debug ("Modified ==> bnd={} bnd.getLastModified={} bnd_file.lastModified={}",
                bnd, bundle_lastmodified, bnd_file.lastModified ());

            store_lastmodified (bnd, bnd_file.lastModified ());
            installing_bundles.add (bnd);
            update_bundle (bnd);
        }
    }

    private void locate_removed_updated_bundles ()
    {
        for (Map.Entry<String, Bundle> bnd_entry: managed_bundles.entrySet ())
        {
            Bundle bnd = bnd_entry.getValue ();
            File bnd_file = null;

            try
            {
                bnd_file = new File (new URI (bnd_entry.getKey ()));
            }
            catch (Exception ignore) {};

            log.debug ("LOCATE Scanning {}", (bnd_file == null)? null: bnd_file.getName ());

            if (valid_file (bnd_file))
            {
                update_if_changed (bnd, bnd_file);
            }
            else if (remove_bundle (bnd))
            {
                managed_bundles.remove (bnd_entry.getKey ());
            }
        }
    }

    private void locate_added_bundles ()
    {
        File[] bundle_list = new File (watched_directory).listFiles ();

        if (bundle_list == null)
        {
            return;
        }

        for (File bnd_file: bundle_list)
        {
            String bnd_uri = bnd_file.toURI ().toString ();

            log.debug ("INSTALL Scanning {} -> {}", bnd_uri, bnd_file);

            if (valid_file (bnd_file) && !managed_bundles.containsKey (bnd_uri))
            {
                Bundle new_bundle = context.getBundle (bnd_uri);

                if (new_bundle != null)
                {
                    if (!installing_bundles.contains (new_bundle))
                    {
                        log.info ("Linking bundle {} from {}", new_bundle, bnd_uri);
                        update_if_changed (new_bundle, bnd_file);

                        if (!is_fragment (new_bundle))
                        {
                            linked_bundles.add (new_bundle);
                        }
                    }
                }
                else // The bundle isn't installed yet
                {
                    try
                    {
                        new_bundle = context.installBundle (bnd_uri);
                        store_lastmodified (new_bundle, bnd_file.lastModified ());
                        installing_bundles.add (new_bundle);
                        log.info ("Installing bundle {} from {}", new_bundle, bnd_uri);
                    }
                    catch (Exception e)
                    {
                        log.error ("Exception on bundle install: {}", bnd_uri, e);
                    }
                }

                // We always store the bundle info assigned with every file (even invalid ones)
                managed_bundles.put (bnd_uri, new_bundle);
            }
        }
    }

    private void update_bundle_states ()
    {
        // TODO: GET RID OF THIS LAZY LOOP AND USE bundleChanged TO FIRE INSTALL ACTIONS
        for (Map.Entry<String, Bundle> bnd_entry: managed_bundles.entrySet ())
        {
            Bundle bnd = bnd_entry.getValue ();

            if (bnd == null || !installing_bundles.contains (bnd))
            {
                continue;
            }

            log.debug ("Bundle {} state {}", bnd, get_state_string (bnd.getState ()));

            switch (bnd.getState ())
            {
                case Bundle.INSTALLED:
                {
                    // This forces framework to try to get bundle resolved
                    bnd.getResource ("META-INF/MANIFEST.MF");
                    log.info ("Bundle {} installed -- trying to resolve", bnd);
                    break;
                }
                case Bundle.RESOLVED:
                {
                    if (is_fragment (bnd))
                    {
                        // This bundle is a FRAGMENT, do NOT try to start
                        log.info ("Bundle Fragment {} is now RESOLVED", bnd);
                        installing_bundles.remove (bnd);
                        break;
                    }

                    try
                    {
                        log.info ("Bundle {} is resolved -- will start now", bnd);
                        bnd.start ();
                    }
                    catch (Exception e)
                    {
                        log.info ("Exception starting bundle {}", bnd, e);
                    }
                    break;
                }
                case Bundle.ACTIVE:
                {
                    // No need for log here
                    break;
                }
            }
        }
    }

    @Override
    public void run ()
    {
        long check_start_time = System.currentTimeMillis ();

        log.info ("Bootstrap bundle scanner started");

        if (initial_delay_flag)
        {
            log.info ("Initial delay {}ms", initial_delay_ms);

            try
            {
                // Enforce a delay before the first directory scan
                Thread.sleep (initial_delay_ms);
            }
            catch (InterruptedException e)
            {
                log.info ("Watcher initial delay interrupted", e);
                // TODO: KEEP THE LOOP, EXIT WHEN THREAD STOPPED
                //return;
            }
        }

        //-------------------------------------------------------------------
        // IMPORTANT: SERVICE BootFinished IS REGISTERED WHEN KARAF FINISHES
        // STARTING IT'S INTERNAL FEATURES/BUNDLES, SO WE CAN KICK IN
        //-------------------------------------------------------------------
        String boot_finished_svc = BootFinished.class.getCanonicalName ();
        boolean boot_finished_flag = false;
        boolean start_level_good_flag = false;
        bootstrap_finished = false;

        //-----------
        // Main loop
        //-----------
        while (!interrupted ())
        {
            if (!start_level_good_flag)
            {
                log.info ("System check cycle {}ms", System.currentTimeMillis () - check_start_time);
            }

            try
            {
                if (context.getBundle (0) == null)
                {
                    log.error ("The Daleks are here. It's time to die :(");
                    break;
                }

                //----------------------
                // Check framework boot
                //----------------------
                if (!boot_finished_flag)
                {
                    ServiceReference boot_marker = context.getServiceReference (boot_finished_svc);

                    log.debug ("boot_marker = {}", boot_marker);

                    if (boot_marker != null)
                    {
                        log.info ("Framework is ready for deployments");
                        boot_finished_flag = true;
                    }
                }

                //--------------------------
                // Check proper start level
                //--------------------------
                if (boot_finished_flag)
                {
                    // Don't access the disk when the framework is still in a startup phase.
                    start_level_good_flag = (context.getBundle (0).getState() == Bundle.ACTIVE);
                }

                if (boot_finished_flag && start_level_good_flag)
                {
                    //--------------
                    // Main actions
                    //--------------
                    locate_removed_updated_bundles ();
                    locate_added_bundles ();
                    update_bundle_states ();
                }

                if (installing_bundles.size () > 0)
                {
                    log.info ("{} new/updated bundle(s) awaiting activation", installing_bundles.size ());
                }

                if (linked_bundles.size () > 0)
                {
                    log.info ("{} linked bundle(s) awaiting activation", linked_bundles.size ());
                }

                if (installing_bundles.size () == 0 && linked_bundles.size () == 0)
                {
                    bootstrap_finished = true;
                }

                synchronized (this)
                {
                    log.debug ("Sleeping for {}ms", poll_ms);
                    wait (poll_ms);
                }
            }
            catch (InterruptedException e)
            {
                return;
            }
            catch (Throwable e)
            {
                try
                {
                    // This will fail if this bundle is uninstalled (zombie)
                    context.getBundle();
                }
                catch (IllegalStateException t)
                {
                    // FileInstall bundle has been uninstalled, exiting loop
                    return;
                }

                log.error ("In main loop, serious trouble we have, young padawan", e);
            }
        }
    }

    public void close ()
    {
        log.info ("Closing scanner thread...");

        context.removeBundleListener (this);

        try
        {
            interrupt ();
            join (10000);
        }
        catch (InterruptedException ignore) {};

        log.info ("Scanner thread closed.");
    }

    public synchronized boolean bootstrapFinished ()
    {
        return (bootstrap_finished);
    }

    @Override
    public void bundleChanged (BundleEvent bundleEvent)
    {
        Bundle bnd = bundleEvent.getBundle ();
        String msg;

        if (managed_bundles.containsKey (bnd.getLocation ()) &&
            bundleEvent.getType () == BundleEvent.STARTED)
        {
            log.info ("Bundle {} is now ACTIVE", bnd);

            // We remove from either list, the source doesn't matters
            installing_bundles.remove (bnd);
            linked_bundles.remove (bnd);
        }

        switch (bundleEvent.getType ())
        {
            case BundleEvent.INSTALLED:       msg = "INSTALLED";       break;
            case BundleEvent.LAZY_ACTIVATION: msg = "LAZY_ACTIVATION"; break;
            case BundleEvent.RESOLVED:        msg = "RESOLVED";        break;
            case BundleEvent.STARTED:         msg = "STARTED";         break;
            case BundleEvent.STARTING:        msg = "STARTING";        break;
            case BundleEvent.STOPPED:         msg = "STOPPED";         break;
            case BundleEvent.STOPPING:        msg = "STOPPING";        break;
            case BundleEvent.UNINSTALLED:     msg = "UNINSTALLED";     break;
            case BundleEvent.UNRESOLVED:      msg = "UNRESOLVED";      break;
            case BundleEvent.UPDATED:         msg = "UPDATED";         break;
            default:                          msg = "unknown";         break;
        }

        log.debug ("bundleChanged: {} type={} state={}", bnd, msg, get_state_string (bnd.getState ()));
    }
}

// EOF
