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

import org.lucidj.api.BundleManager;
import org.lucidj.api.DeploymentEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
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
    private BundleManager bundle_manager;

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
        Manifest mf;
        Attributes attrs;

        // Check compatibility looking for X-Package attribute on manifest
        if ((mf = bundle_manager.getManifest (location)) != null &&
            (attrs = mf.getMainAttributes ()) != null &&
            attrs.getValue ("X-Package") != null)
        {
            return (ENGINE_LEVEL);
        }

        // Not compatible
        return (0);
    }

    private List<String> list_existing_files (List<String> file_list, Path root_path)
    {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream (root_path))
        {
            for (Path path: stream)
            {
                if (path.toFile ().isDirectory ())
                {
                    list_existing_files (file_list, path);
                }
                else
                {
                    file_list.add (path.toAbsolutePath ().toString ());
                }
            }
        }
        catch (IOException e)
        {
            log.error ("Exception listing package files: {}", root_path, e);
        }
        return (file_list);
    }

    private List<String> list_existing_files (File root_dir)
    {
        return (list_existing_files (new ArrayList<String> (), root_dir.toPath ()));
    }

    private boolean extract_all (String package_dist, final String dest_package_dir)
    {
        File dest_dir = new File (dest_package_dir);

        if (!dest_dir.exists () && !dest_dir.mkdirs ())
        {
            log.error ("Unable create directory: {}", dest_package_dir);
            return (false);
        }

        // We'll compare what exists with what will be extracted so we can delete excess files later
        List<String> files_to_remove = list_existing_files (dest_dir);

        try
        {
            ZipFile zipFile = new ZipFile (new File (new URI (package_dist)));
            Enumeration<? extends ZipEntry> entries = zipFile.entries ();

            while (entries.hasMoreElements ())
            {
                // Get file or dir from zip entry
                ZipEntry entry = entries.nextElement();
                File file_entry = new File (dest_package_dir, entry.getName ());

                if (entry.isDirectory ()) // Ensure the needed dirs are available
                {
                    if (!file_entry.exists () && !file_entry.mkdirs ())
                    {
                        log.error ("Unable create directory: {}", entry.getName ());
                        return (false);
                    }
                }
                else // Extract and/or replace changed files
                {
                    if (!file_entry.exists () || entry.getTime () != file_entry.lastModified ())
                    {
                        InputStream stream = zipFile.getInputStream(entry);
                        Files.copy (stream, file_entry.toPath (), StandardCopyOption.REPLACE_EXISTING);
                        stream.close ();
                        file_entry.setLastModified (entry.getTime());
                    }

                    // Remove the existing file from the deletion list
                    files_to_remove.remove (dest_package_dir + "/" + entry.getName ());
                }
            }

            // Delete all excess files
            for (String file: files_to_remove)
            {
                if (!new File (file).delete ())
                {
                    log.error ("Unable delete file: {}", file);
                    return (false);
                }
            }
            return (true);
        }
        catch (Exception e)
        {
            log.error ("Exception extracting package: {}", package_dist, e);
            return (false);
        }
    }

    @Override
    public Bundle installBundle (String location, Properties properties)
    {
        Manifest mf = bundle_manager.getManifest (location);

        if (mf == null)
        {
            return (null);
        }

        Attributes attrs = mf.getMainAttributes ();
        String bundle_symbolic_name = attrs.getValue ("Bundle-SymbolicName");
        Version bundle_version = new Version (attrs.getValue ("Bundle-Version"));

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

                    if (bundle_manager.installBundle (bundle_uri, properties) == null)
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
                return (bundle_manager.installBundle (location, properties));
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
        // TODO: HANDLE Bundles/ AND Resources/
        return (bundle_manager.updateBundle (bnd));
    }

    @Override
    public boolean refreshBundle (Bundle bnd)
    {
        return (false);
        // TODO: HANDLE Bundles/ AND Resources/
        //return (bundle_manager.refreshBundle (bnd));
    }

    @Override
    public boolean uninstallBundle (Bundle bnd)
    {
        // TODO: HANDLE Bundles/ AND Resources/
        return (bundle_manager.uninstallBundle (bnd));
    }
}

// EOF
