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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import java.util.jar.Attributes;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Context;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Validate;

@Component
@Instantiate
public class PkgDeployer implements Runnable, BundleListener
{
    private final static transient Logger log = LoggerFactory.getLogger (PkgDeployer.class);

    @Context
    private BundleContext context;

    private Map<String, Bundle> managed_bundles = new ConcurrentHashMap<> ();
    private Set<Bundle> installing_bundles = new HashSet<> ();
    private Set<Bundle> linked_bundles = new HashSet<> ();

    private String watched_directory;
    private Thread poll_thread;
    private int thread_poll_ms = 3000;

    private boolean valid_file (File f)
    {
        // Basic check
        if (f != null && f.exists () && f.isFile () && f.canRead ())
        {
            // Now check if we have X-Package-Version stated on MANIFEST.MF
            try
            {
                JarInputStream jar = new JarInputStream (new FileInputStream (f));
                Manifest mf = jar.getManifest ();
                Attributes attrs = mf.getMainAttributes ();
                String package_version = attrs.getValue ("X-Package-Version");

                if (package_version != null)
                {
                    return (true);
                }
            }
            catch (Exception ignore) {};
        }
        return (false);
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

    private boolean update_bundle (Bundle bnd)
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

    private boolean remove_bundle (Bundle bnd)
    {
        try
        {
            log.info ("Uninstalling package {}", bnd);
            bnd.uninstall ();
            return (true);
        }
        catch (Exception e)
        {
            log.error ("Error uninstalling {}", bnd, e);
            return (false);
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
        File[] package_list = new File (watched_directory).listFiles ();

        if (package_list == null)
        {
            return;
        }

        for (File package_file: package_list)
        {
            String package_uri = package_file.toURI ().toString ();

            log.debug ("INSTALL Scanning {} -> {}", package_uri, package_file);

            if (valid_file (package_file) && !managed_bundles.containsKey (package_uri))
            {
                Bundle new_bundle = context.getBundle (package_uri);

                if (new_bundle != null)
                {
                    if (!installing_bundles.contains (new_bundle))
                    {
                        log.info ("Linking bundle {} from {}", new_bundle, package_uri);
                        update_if_changed (new_bundle, package_file);
                        linked_bundles.add (new_bundle);
                    }
                }
                else // The bundle isn't installed yet
                {
                    try
                    {
                        new_bundle = context.installBundle (package_uri);
                        store_lastmodified (new_bundle, package_file.lastModified ());
                        installing_bundles.add (new_bundle);
                        log.info ("Installing package {} from {}", new_bundle, package_uri);
                    }
                    catch (Exception e)
                    {
                        log.error ("Exception on package install: {}", package_uri, e);
                    }
                }

                // We always store the bundle info assigned with every file (even invalid ones)
                managed_bundles.put (package_uri, new_bundle);
            }
        }
    }

    private void poll_repository ()
    {
        locate_removed_updated_bundles ();
        locate_added_bundles ();
        update_bundle_states ();

        if (installing_bundles.size () > 0)
        {
            log.info ("{} new/updated packages(s) awaiting activation", installing_bundles.size ());
        }

        if (linked_bundles.size () > 0)
        {
            log.info ("{} linked packages(s) awaiting activation", linked_bundles.size ());
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

    @Override
    public void bundleChanged (BundleEvent bundleEvent)
    {
        Bundle bnd = bundleEvent.getBundle ();
        String msg;

        if (managed_bundles.containsKey (bnd.getLocation ()) &&
            bundleEvent.getType () == BundleEvent.STARTED)
        {
            log.info ("Package {} is now ACTIVE", bnd);

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

    @Validate
    private void validate ()
    {
        // Configuration
        watched_directory = System.getProperty ("rq.home") + "/runtime/applications";

        // Start listening to bundle events
        this.context.addBundleListener (this);

        // Start things
        poll_thread = new Thread (this);
        poll_thread.setName (this.getClass ().getSimpleName ());
        poll_thread.start ();

        log.info ("PkgDeployer started: applications dir = {}", watched_directory);
    }

    @Invalidate
    private void invalidate ()
    {
        // Stop listening to bundle events
        context.removeBundleListener (this);

        try
        {
            // Stop things, wait 10secs to clean stop
            poll_thread.interrupt ();
            poll_thread.join (10000);
        }
        catch (InterruptedException ignore) {};

        log.info ("PkgDeployer stopped");
    }

    @Override // Runnable
    public void run ()
    {
        while (!poll_thread.interrupted ())
        {
            try
            {
                poll_repository ();

                synchronized (this)
                {
                    log.debug ("Sleeping for {}ms", thread_poll_ms);
                    wait (3000);
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
                    // FileInstall bundle has been uninstalled, exiting loop
                    break;
                }

                log.error ("Package deployment exception", t);
            }
        }
    }
}

// EOF
