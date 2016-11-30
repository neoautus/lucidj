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
import java.io.InputStream;
import java.net.URI;
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
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

@Component (immediate = true)
@Instantiate
@Provides (specifications = DeploymentEngine.class)
public class PackageDeploymentEngine implements DeploymentEngine
{
    private final static transient Logger log = LoggerFactory.getLogger (PackageDeploymentEngine.class);
    private final static int ENGINE_LEVEL = 50;

    @Context
    private BundleContext context;

    @Requires
    private BundleDeployer bnd_deployer;

    private String packages_dir;

    public PackageDeploymentEngine ()
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

    @Override
    public String getEngineName ()
    {
        return (getClass ().getCanonicalName () + "(" + ENGINE_LEVEL + ")");
    }

    @Override
    public int compatibleArtifact (String location)
    {
        Manifest mf = bnd_deployer.getManifest (location);

        if (mf == null)
        {
            // No manifest no glory
            return (0);
        }

        // We need at very least Bundle-SymbolicName on the manifest...
        Attributes attrs = mf.getMainAttributes ();

        // ...then we return the lowest compatibility, as fallback, to deploy any generic OSGi bundle
        return ((attrs != null && attrs.getValue ("X-Package") != null)? 50: 0);
    }

    private boolean extract_all (String package_dist, final String dest_package_dir)
    {
        File dest_dir = new File (dest_package_dir);

        // TODO: IF IT EXISTS, WIPE OUT
        if (!dest_dir.exists ())
        {
            dest_dir.mkdirs ();
        }

        try
        {
            ZipFile zipFile = new ZipFile (new File (new URI (package_dist)));
            Enumeration<? extends ZipEntry> entries = zipFile.entries ();

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

    @Override
    public Bundle installBundle (String location)
    {
        Manifest mf = bnd_deployer.getManifest (location);

        if (mf == null)
        {
            return (null);
        }

        Attributes attrs = mf.getMainAttributes ();
        String bundle_symbolic_name = attrs.getValue ("Bundle-SymbolicName");
        Version bundle_version = new Version (attrs.getValue ("Bundle-Version"));

//        Bundle installed_bundle = bnd_deployer.getBundleByDescription (bundle_symbolic_name, bundle_version);
//
//        // TODO: HANDLE UPDATE
//        if (installed_bundle != null && installed_bundle.getState () == Bundle.ACTIVE)
//        {
//            log.info ("Package {} already installed", installed_bundle);
//            return (installed_bundle);
//        }
//
        // TODO: ALLOW MULTIPLE PACKAGES WITH DIFFERENT VERSIONS WHEN CONFIG SET
        // TODO: AVOID UPDATE BUNDLE __WHILE EXTRACTING__
        // TODO: DELETE EXTRACTED PACKAGE CONTENTS WHEN UNINSTALLING
        // TODO: BUILD AN EMBEDDED Bundles/ DIRECTORY FOR SHARED EMBEDDED BUNDLES USED BY MANY PACKAGES
        String extracted_package_dir = packages_dir + "/" + bundle_symbolic_name + "/" + bundle_version;
        extract_all (location, extracted_package_dir);

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
            try
            {
                // Here the native OSGi bundle install
                return (context.installBundle (location));
            }
            catch (Exception e)
            {
                log.error ("Exception installing package: {}", location, e);
            }
        }

        log.error ("Errors found when deploying embedded bundles -- will not install package.");
        return (null);
    }

    @Override
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
            uninstallBundle (bnd);
            return (false);
        }
    }

    @Override
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
}

// EOF
