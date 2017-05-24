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

import org.lucidj.api.Artifact;
import org.lucidj.api.BundleManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
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
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Context;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Validate;

//
//   Low-level bundle services
//   Non-volatile properties                               Main deployment service
//   +-----------------+                                   +--------------------+
//   |  BundleManager  |<---------------------------------1|  ArtifactDeployer  |<---1 System
//   +-----------------+                                   +--------------------+
//        ^   ^   ^                                                 *
//        |   |   |           +---------------------+               |
//        |   |   \----------1|  DeploymentEngineA  |<--------------+
//        |   |               +---------------------+               |
//        |   |                                                     |
//        |   |               +---------------------+               |
//        |   \--------------1|  DeploymentEngineB  |<--------------+
//        |                   +---------------------+               |
//        |                              .                          |
//        |                              .                          |
//        |                              .                          |
//        |                   +---------------------+               |
//        \------------------1|  DeploymentEngineX  |<--------------/
//                            +---------------------+
//                            Many deployment engines
//

@Component (immediate = true, publicFactory = false)
@Instantiate
@Provides (specifications = BundleManager.class)
public class DefaultBundleManager implements BundleManager, BundleListener
{
    private final static transient Logger log = LoggerFactory.getLogger (DefaultBundleManager.class);

    private final static String REFERENCE_PREFIX = "reference:";

    @Context
    private BundleContext context;

    private Map<String, Properties> bundle_prop_cache;
    private String cache_dir;

    public DefaultBundleManager ()
    {
        bundle_prop_cache = new ConcurrentHashMap<> ();

        // TODO: THIS SHOULD BE RECONFIGURABLE
        cache_dir = System.getProperty ("system.home") + "/cache/" + this.getClass ().getSimpleName ();

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
        return (new File (cache_dir + "/" + location.replaceAll ("\\p{P}", "_") + ".properties"));
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
            try
            {
                Properties properties = new Properties ();
                properties.load (new FileInputStream (bundle_data_file));

                if (properties.containsKey (Artifact.PROP_LOCATION))
                {
                    bundle_prop_cache.put (properties.getProperty (Artifact.PROP_LOCATION), properties);
                }
                else
                {
                    log.error ("Internal error: Missing bundle location from {}", bundle_data_file);
                }
            }
            catch (Exception e)
            {
                log.error ("Error reading deployment properties from {}", bundle_data_file, e);
            }
        }
    }

    private boolean store_properties (String location, Properties properties)
    {
        bundle_prop_cache.put (location, properties);

        try
        {
            // Always store location so we can populate the bundle cache properly
            properties.setProperty (Artifact.PROP_LOCATION, location);
            properties.store (new FileOutputStream (get_bundle_data_file (location)), null);
            return (true);
        }
        catch (Exception e)
        {
            log.error ("Exception storing bundle properties: {}", location, e);
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
        String msg = "Live long and prosper";
        Bundle bnd = bundleEvent.getBundle ();
        String location = bnd.getLocation ();
        Properties properties = bundle_prop_cache.get (location);

        // Is this bundle managed by us?
        if (properties == null)
        {
            // Nope
            return;
        }
        
        // Store bundle state
        properties.setProperty (Artifact.PROP_BUNDLE_STATE, Integer.toString (bnd.getState ()));
        properties.setProperty (Artifact.PROP_BUNDLE_STATE_HUMAN, get_state_string (bnd.getState ()));
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
                    if ("transient".equalsIgnoreCase (properties.getProperty (Artifact.PROP_BUNDLE_START, "normal")))
                    {
                        log.info ("Bundle {} is resolved -- will start transient now", bnd);
                        bnd.start (Bundle.START_TRANSIENT);
                    }
                    else
                    {
                        log.info ("Bundle {} is resolved -- will start now", bnd);
                        bnd.start ();
                    }
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

    @Override // BundleManager
    public Manifest getManifest (File file)
    {
        FileInputStream file_stream = null;
        
        try
        {
            if (file.isDirectory ())
            {
                // Will open the manifest file itself
                file_stream = new FileInputStream (new File (file, "/META-INF/MANIFEST.MF"));
                return (new Manifest (file_stream));
            }
            else
            {
                // Open from within Jar file
                file_stream = new FileInputStream (file);
                JarInputStream jar_stream = new JarInputStream (file_stream);
                return (jar_stream.getManifest ());
            }
        }
        catch (IOException e)
        {
            log.error ("Exception reading MANIFEST.MF from: {}", file, e);
            return (null);
        }
        finally
        {
            if (file_stream != null)
            {
                try
                {
                    file_stream.close ();
                }
                catch (Exception ignore) {};
            }
        }
    }

    @Override // BundleManager
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

    @Override // BundleManager
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
        // TODO: ODD PLACE TO STRIP reference: FROM location
        if (location.startsWith (REFERENCE_PREFIX))
        {
            location = location.substring (REFERENCE_PREFIX.length ());
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

    @Override // BundleManager
    public Bundle installBundle (String location, Properties properties)
    {
        Bundle new_bundle = null;

        log.debug ("installBundle: location={} properties={}", location, properties);

        try
        {
            File bundle_file = get_valid_file (location);

            if (bundle_file == null)
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
                    log.info ("Bundle {} installed from other location (provided: {}, original: {})",
                        new_bundle, location, new_bundle.getLocation ());
                }

                // TODO: WHAT HAPPENS WHEN THE BUNDLE ISN'T MANAGED?
                return (new_bundle);
            }

            // We need properties anyway
            if (properties == null)
            {
                properties = new Properties ();
            }

            // Add bundle properties to repository, so we can manage it
            properties.setProperty (Artifact.PROP_LAST_MODIFIED, Long.toString (bundle_file.lastModified ()));
            properties.setProperty (Artifact.PROP_BUNDLE_STATE, Integer.toString (Bundle.UNINSTALLED));
            store_properties (location, properties);

            // Install bundle
            new_bundle = context.installBundle (location);
            
            log.info ("Installing bundle {} from {}", new_bundle, location);
        }
        catch (Exception e)
        {
            log.error ("Exception on bundle install: {}", location, e);
        }
        return (new_bundle);
    }

    @Override // BundleManager
    public boolean updateBundle (Bundle bnd)
    {
        try
        {
            log.info ("Updating bundle {}", bnd);
            bnd.stop (Bundle.STOP_TRANSIENT);
            bnd.update ();
            return (true);
        }
        catch (Exception e)
        {
            log.error ("Error updating bundle {}", bnd, e);
            uninstallBundle (bnd);
            return (false);
        }
    }

    @Override // BundleManager
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
        long bundle_lastmodified = Long.parseLong (properties.getProperty (Artifact.PROP_LAST_MODIFIED));

        if (bundle_lastmodified != bundle_file.lastModified ())
        {
            log.debug ("Modified ==> bnd={} bnd.getLastModified={} bnd_file.lastModified={}",
                bnd, bundle_lastmodified, bundle_file.lastModified ());

            if (updateBundle (bnd))
            {
                properties.setProperty (Artifact.PROP_LAST_MODIFIED, Long.toString (bundle_file.lastModified ()));
                store_properties (location, properties);
                return (true);
            }
        }

        return false;
    }

    @Override // BundleManager
    public boolean uninstallBundle (Bundle bnd)
    {
        try
        {
            log.info ("Uninstalling bundle {}", bnd);
            bnd.uninstall ();
            return (true);
        }
        catch (Exception e)
        {
            log.error ("Exception uninstalling {}", bnd, e);
            return (false);
        }
    }

    @Override // BundleManager
    public Bundle getBundleByLocation (String location)
    {
        if (bundle_prop_cache.containsKey (location))
        {
            return (context.getBundle (location));
        }

        // Either the bundle doesn't exists, isn't managed by us, or it's uninstalled
        return (null);
    }

    @Override // BundleManager
    public Bundle getBundleByProperty (String property, String value)
    {
        // TODO: Bundle[] getBundlesByProperty(...)
        Bundle found_bundle = null;

        for (Map.Entry<String, Properties> entry: bundle_prop_cache.entrySet())
        {
            Properties properties = entry.getValue ();

            if (properties.containsKey (property))
            {
                if (value == null)
                {
                    if (properties.getProperty (property) == null)
                    {
                        // We should try more than once because Bundle/ bundles have
                        // deployment-location != location
                        if ((found_bundle = context.getBundle (entry.getKey ())) != null)
                        {
                            break;
                        }
                    }
                }
                else if (value.equals (properties.getProperty (property)))
                {
                    if ((found_bundle = context.getBundle (entry.getKey ())) != null)
                    {
                        break;
                    }
                }
            }
        }

        return (found_bundle);
    }

    @Override // BundleManager
    public Map<Bundle, Properties> getBundles ()
    {
        Map<Bundle, Properties> bundle_list = new HashMap<> ();

        for (Map.Entry<String, Properties> entry: bundle_prop_cache.entrySet())
        {
            Bundle bundle = context.getBundle (entry.getKey ());

            if (bundle != null)
            {
                bundle_list.put (bundle, entry.getValue ());
            }
        }

        return (bundle_list);
    }

    @Override // BundleManager
    public Properties getBundleProperties (Bundle bnd)
    {
        return (bundle_prop_cache.get (bnd.getLocation ()));
    }

    @Validate
    private void validate ()
    {
        // Load references to all bundles we manage
        populate_cache ();

        // Start listening to bundle events
        context.addBundleListener (this);

        log.info ("DefaultBundleManager started");
    }

    @Invalidate
    private void invalidate ()
    {
        // Stop listening to bundle events
        context.removeBundleListener (this);

        log.info ("DefaultBundleManager stopped");
    }
}

// EOF
