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
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Version;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Context;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;

@Component
@Instantiate
public class PkgDeployer implements Runnable
{
    private final static transient Logger log = LoggerFactory.getLogger (PkgDeployer.class);

    @Context
    private BundleContext context;

    @Requires
    private BundleDeployer bnd_deployer;

    private String packages_dir;

    private String watched_directory;
    private Thread poll_thread;
    private int thread_poll_ms = 1000;

    public PkgDeployer ()
    {
        packages_dir = System.getProperty ("rq.home") + "/cache/" + this.getClass ().getSimpleName ();

        File check_packages_dir = new File (packages_dir);

        if (!check_packages_dir.exists ())
        {
            if (check_packages_dir.mkdir ())
            {
                log.info ("Creating cache {}", packages_dir);
            }
            else
            {
                log.error ("Error creating cache {}", packages_dir);
            }
        }
    }

    private boolean valid_file (File f)
    {
        // Basic check
        if (f != null && f.exists () && f.isFile () && f.canRead ())
        {
            // Now check if we have X-Package-Version stated on MANIFEST.MF
            try
            {
                Manifest mf = bnd_deployer.getManifest (f);
                Attributes attrs = mf.getMainAttributes ();
                String package_version = attrs.getValue ("X-Package");

                if (package_version != null)
                {
                    return (true);
                }
            }
            catch (Exception ignore) {};
        }
        return (false);
    }

    private boolean extractAll (File package_file, final String dest_package_dir)
    {
        log.info ("extractAll: {} to {}", package_file, dest_package_dir);

        File dest_dir = new File (dest_package_dir);

        // TODO: IF IT EXISTS, WIPE OUT
        if (!dest_dir.exists ())
        {
            dest_dir.mkdirs ();
        }

        ZipFile zipFile = null;

        try
        {
            zipFile = new ZipFile (package_file);
        }
        catch (Exception e)
        {
            log.error ("Exception extracting package contents", e);
            return (false);
        }

        Enumeration<? extends ZipEntry> entries = zipFile.entries ();

        log.info ("entries = {}", entries);

        try
        {
            while (entries.hasMoreElements ())
            {
                ZipEntry entry = entries.nextElement();
                log.info ("entry: {} isDirectory={}", entry, entry.isDirectory ());

                if (entry.isDirectory ())
                {
                    File new_directory = new File (dest_package_dir, entry.getName ());

                    if (!new_directory.exists ())
                    {
                        new_directory.mkdirs ();
                    }
                }
                else
                {
                    File new_file = new File (dest_package_dir, entry.getName ());
                    InputStream stream = zipFile.getInputStream(entry);
                    Files.copy (stream, new_file.toPath (), StandardCopyOption.REPLACE_EXISTING);
                }

            }
            return (true);
        }
        catch (Exception e)
        {
            log.error ("Error extracting package", e);
            return (false);
        }
    }

    private boolean install_package (File package_file)
    {
        Manifest mf = bnd_deployer.getManifest (package_file);

        if (mf == null)
        {
            return (false);
        }

        Attributes attrs = mf.getMainAttributes ();
        String bundle_symbolic_name = attrs.getValue ("Bundle-SymbolicName");
        Version bundle_version = new Version (attrs.getValue ("Bundle-Version"));

        Bundle installed_bundle = bnd_deployer.getBundleByDescription (bundle_symbolic_name, bundle_version);

        // TODO: HANDLE UPDATE
        if (installed_bundle != null && installed_bundle.getState () == Bundle.ACTIVE)
        {
            log.info ("Package {} already installed", installed_bundle);
            return (true);
        }

        // TODO: ALLOW MULTIPLE PACKAGES WITH DIFFERENT VERSIONS WHEN CONFIG SET
        String extracted_package_dir = packages_dir + "/" + bundle_symbolic_name + "/" + bundle_version;
        extractAll (package_file, extracted_package_dir);

        File[] embedded_bundles = new File (extracted_package_dir, "Bundles/").listFiles ();
        boolean got_errors = false;

        if (embedded_bundles != null)
        {
            for (File bundle_file: embedded_bundles)
            {
                if (bundle_file.isFile ())
                {
                    String bundle_uri = bundle_file.toURI ().toString ();

                    if (bnd_deployer.installBundle (bundle_uri) == null)
                    {
                        got_errors = true;
                    }
                }
            }
        }

        if (!got_errors)
        {
            String package_uri = package_file.toURI ().toString ();
            bnd_deployer.installBundle (package_uri);
        }
        else
        {
            log.error ("Errors found when deploying embedded bundles -- will not install package.");
        }

        return (true);
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

            if (valid_file (package_file))
            {
                Bundle bnd = bnd_deployer.getBundleByLocation (package_uri);

                if (bnd == null) // The bundle isn't installed yet
                {
                    install_package (package_file);
                }
            }
        }
    }

    @Validate
    private void validate ()
    {
        // Configuration
        watched_directory = System.getProperty ("rq.home") + "/runtime/applications";

        // Start things
        poll_thread = new Thread (this);
        poll_thread.setName (this.getClass ().getSimpleName ());
        poll_thread.start ();

        log.info ("PkgDeployer started: applications dir = {}", watched_directory);
    }

    @Invalidate
    private void invalidate ()
    {
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
        while (!poll_thread.isInterrupted ())
        {
            try
            {
                locate_added_bundles ();

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

                log.error ("Package deployment exception", t);
            }
        }
    }
}

// EOF
