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
import org.lucidj.api.EmbeddingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.DirectoryStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;

// TODO: USE ServiceObject
public class PackageInstance implements Artifact
{
    private final static Logger log = LoggerFactory.getLogger (PackageInstance.class);

    private Thread local_thread;
    private volatile int extended_state = Artifact.STATE_EX_ACTIVE;

    private BundleManager bundleManager;
    private String packages_dir;
    private Bundle main_bundle;
    private EmbeddingContext embedding_context;
    private URI location_uri;

    public PackageInstance (EmbeddingContext embedding_context, BundleManager bundleManager, String cache_dir)
    {
        this.embedding_context = embedding_context;
        this.bundleManager = bundleManager;
        this.packages_dir = cache_dir;
    }

    @Override
    public URI getLocation ()
    {
        return (location_uri);
    }

    @Override
    public URI getLocation (String entry_path)
    {
        Path source_path = Paths.get (location_uri).resolve (entry_path);
        return (source_path == null? null: source_path.toUri ());
    }

    @Override
    public Bundle getMainBundle ()
    {
        return (main_bundle);
    }

    @Override
    public <T> T adapt (Class<T> type)
    {
        if (type == null)
        {
            return (null);
        }
        else if (EmbeddingContext.class.isAssignableFrom (type))
        {
            return (type.cast (embedding_context));
        }
        if (type.isAssignableFrom (this.getClass ()))
        {
            return (type.cast (this));
        }
        return (null);
    }

    @Override
    public int getState ()
    {
        // TODO: WE NEED open() AND close()
        return (main_bundle.getState ());
    }

    @Override
    public int getExtState ()
    {
        return (main_bundle.getState () == Bundle.ACTIVE? extended_state: Artifact.STATE_EX_NONE);
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
        return (list_existing_files (new ArrayList<> (), root_dir.toPath ()));
    }

    private boolean copy_or_update_file_tree (File source_package, final File dest_dir)
    {
        // We'll compare what exists with what will be extracted so we can delete excess files later
        List<String> files_to_remove = list_existing_files (dest_dir);
        Path dest_path = dest_dir.toPath ();
        Path source_path = source_package.toPath ();

        try
        {
            // Simple and stupid tree copy
            Files.walkFileTree (source_path, new SimpleFileVisitor<Path> ()
            {
                @Override
                public FileVisitResult preVisitDirectory (final Path dir, final BasicFileAttributes attrs)
                        throws IOException
                {
                    Files.createDirectories (dest_path.resolve (source_path.relativize (dir)));
                    return (FileVisitResult.CONTINUE);
                }

                @Override
                public FileVisitResult visitFile (final Path file, final BasicFileAttributes attrs)
                        throws IOException
                {
                    // TODO: OVERRIDE ONLY NEW/CHANGED FILES
                    Path dest_file = dest_path.resolve (source_path.relativize (file));
                    Files.copy (file, dest_file, StandardCopyOption.REPLACE_EXISTING);
                    Files.setLastModifiedTime (dest_file, Files.getLastModifiedTime (file));
                    files_to_remove.remove (dest_file.toAbsolutePath ().toString ());
                    return (FileVisitResult.CONTINUE);
                }

                @Override
                public FileVisitResult visitFileFailed (Path file, IOException e)
                {
                    // TODO: HANDLE IT!
                    log.error ("visitFileFailed: file={}", e);
                    return (FileVisitResult.CONTINUE);
                }
            });

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
            log.error ("Exception extracting package: {}", source_package, e);
            return (false);
        }
    }

    private boolean extract_all (File source_package, final File dest_dir)
    {
        // We'll compare what exists with what will be extracted so we can delete excess files later
        List<String> files_to_remove = list_existing_files (dest_dir);

        try
        {
            ZipFile zipFile = new ZipFile (source_package);
            Enumeration<? extends ZipEntry> entries = zipFile.entries ();

            while (entries.hasMoreElements ())
            {
                // Get file or dir from zip entry
                ZipEntry entry = entries.nextElement();
                File file_entry = new File (dest_dir, entry.getName ());

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
                    files_to_remove.remove (dest_dir + "/" + entry.getName ());
                    // TODO: +++ CHECK FILE STRING MATCH
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
            log.error ("Exception extracting package: {}", source_package, e);
            return (false);
        }
    }

    // TODO: PROVIDE A PROPER EXCEPTION CLASS FOR THIS SUBSYSTEM
    @Override
    public Bundle install (String location, Properties properties)
        throws Exception
    {
        // Probably we'll need some underlying artifact that can keep track
        // of overall deployment status, as well as embedded main_bundle status,
        // it's deployment status, errors, warnings, configurations and so forth.

        // Exceptions are unlikely, but may bubble up
        location_uri = new URI (location);
        File source_location = new File (location_uri);

        //-----------------------------------------------------
        // 1) DETERMINE Bundle-SymbolicName AND Bundle-Version
        //-----------------------------------------------------

        // The default symbolic name is the package filename without .leap extension
        String source_filename = source_location.getName ();
        String bundle_symbolic_name = source_filename.substring (0, source_filename.lastIndexOf ("."));
        Version bundle_version = new Version ("0");

        // The provided manifest is the primary source of valid information
        Manifest package_mf = bundleManager.getManifest (source_location);

        if (package_mf != null)
        {
            Attributes attrs = package_mf.getMainAttributes ();
            bundle_symbolic_name = (String)attrs.getOrDefault ("Bundle-SymbolicName", bundle_symbolic_name);
            bundle_version = new Version ((String)attrs.getOrDefault ("Bundle-Version", "0"));
        }
        else
        {
            // Try to get the defaults from Package.info.
            // Notice that Package.info is ONLY used with exploded packages, in the
            // absence of a valid MANIFEST.MF. For zipped packages, we assume that
            // Package.info attributes were properly copied to MANIFEST.MF.
            try
            {
                Properties attrs = new Properties ();
                File package_info = new File (source_location, "/meta-inf/Package.info");
                attrs.load (new FileReader (package_info));
                bundle_symbolic_name = attrs.getProperty ("Bundle-SymbolicName", bundle_symbolic_name);
                bundle_version = new Version (attrs.getProperty ("Bundle-Version", "0"));
            }
            catch (Exception ignore) {};
        }

        //----------------------------------------------------
        // 2) BUILD THE RUNTIME, UNZIPPED COPY OF THE PACKAGE
        //----------------------------------------------------

        // TODO: ALLOW MULTIPLE PACKAGES WITH DIFFERENT VERSIONS WHEN CONFIG SET
        // TODO: AVOID UPDATE BUNDLE __WHILE EXTRACTING__
        // TODO: DELETE EXTRACTED PACKAGE CONTENTS WHEN UNINSTALLING
        String extracted_package_dir = packages_dir + "/" + bundle_symbolic_name + "/" + bundle_version;
        File runtime_location = new File (extracted_package_dir);

        if (!runtime_location.exists () && !runtime_location.mkdirs ())
        {
            throw (new Exception ("Unable to create runtime directory: " + runtime_location));
        }

        if (source_location.isDirectory ())
        {
            copy_or_update_file_tree (source_location, runtime_location);
        }
        else
        {
            extract_all (source_location, runtime_location);
        }

        //-------------------------------------------------------------------
        // 3) PROVIDE A SENSIBLE MANIFEST.MF IF THE PACKAGE DOESN'T HAVE ONE
        //-------------------------------------------------------------------

        // Create a base manifest if needed.
        // We need a manifest in order to become a valid OSGi main_bundle
        if (package_mf == null)
        {
            File meta_inf = new File (runtime_location, "META-INF");

            if (!meta_inf.exists () && !meta_inf.mkdirs ())
            {
                throw (new Exception ("Unable to create META-INF directory: " + meta_inf));
            }

            File generated_mf = new File (meta_inf, "MANIFEST.MF");

            Manifest manifest = new Manifest();
            Attributes atts = manifest.getMainAttributes ();
            atts.put (Attributes.Name.MANIFEST_VERSION, "1.0");
            atts.putValue ("Created-By",
                    System.getProperty("java.version") +
                            " (" + System.getProperty("java.vendor") + ") & LucidJ");
            atts.putValue (Constants.BUNDLE_MANIFESTVERSION, "2");
            atts.putValue (Constants.BUNDLE_SYMBOLICNAME, bundle_symbolic_name);
            atts.putValue (PackageDeploymentEngine.ATTR_PACKAGE, PackageDeploymentEngine.ATTR_PACKAGE_VERSION);

            Properties package_props = new Properties ();

            // Read the attributes from Package.info, if available
            try
            {
                File package_info = new File (runtime_location, "/meta-inf/Package.info");
                package_props.load (new FileReader (package_info));
            }
            catch (IOException ignore) {};

            // Merge the attributes from Package.info, if any
            for (String key: package_props.stringPropertyNames ())
            {
                try
                {
                    atts.putValue (key, package_props.getProperty (key));
                }
                catch (IllegalArgumentException e)
                {
                    log.warn ("Ignoring invalid Package.info key: {}", key);
                }
            }

            try (FileOutputStream os = new FileOutputStream (generated_mf))
            {
                manifest.write (os);
            }
            catch (IOException e)
            {
                throw (new Exception ("Unable to create MANIFEST.MF: " + generated_mf));
            }
        }

        //-------------------------------------------------
        // 4) EXTRACT NATIVE OSGi BUNDLES AND INSTALL THEM
        //-------------------------------------------------

        // TODO: BUILD AN EMBEDDED Bundles/ DIRECTORY FOR SHARED EMBEDDED BUNDLES USED BY MANY PACKAGES
        File[] embedded_bundles = new File (extracted_package_dir, "Bundles/").listFiles ();
        Exception got_errors = null;

        if (embedded_bundles != null)
        {
            for (File bundle_file: embedded_bundles)
            {
                if (bundle_file.isFile ())
                {
                    String bundle_uri = bundle_file.toURI ().toString ();

                    try
                    {
                        Bundle new_bundle = bundleManager.installBundle (bundle_uri, properties);
                        log.info ("Installing embedded main_bundle {} from {}", new_bundle, bundle_uri);
                    }
                    catch (Exception e)
                    {
                        // TODO: CLEANUP ON ERRORS
                        got_errors = e;
                        break;
                    }
                }
            }
        }

        //--------------------------------
        // 5) INSTALL THIS PACKAGE BUNDLE
        //--------------------------------

        if (got_errors == null)
        {
            try
            {
                // Here the native OSGi main_bundle install
                // Notice we install the exploded main_bundle stored on cache, NOT the jar
                String runtime_location_ref = "reference:" + runtime_location.toURI ().toString ();
                main_bundle = bundleManager.installBundle (runtime_location_ref, properties);
                log.info ("----------- BUNDLE INSTALL: main_bundle={} state={} context={}",
                        main_bundle, main_bundle.getState (), main_bundle.getBundleContext ());
                return (main_bundle);
            }
            catch (Exception e)
            {
                throw (new Exception ("Exception installing main_bundle: " + location, e));
            }
        }
        throw (new Exception ("Errors found when deploying embedded bundles -- will not install package.", got_errors));
    }

    @Override
    public boolean open ()
    {
        synchronized (this)
        {
            if (getExtState () != Artifact.STATE_EX_ERROR)
            {
                // Now we are opening
                extended_state = Artifact.STATE_EX_OPENING;
                embedding_context.open (main_bundle);

                // Proceed opening sequence
                local_thread = new OpenTransition ();
                local_thread.start ();
                return (true);
            }
        }
        return (false);
    }

    @Override
    public boolean close ()
    {
        synchronized (this)
        {
            if (getExtState () == Artifact.STATE_EX_OPEN)
            {
                // Now we are closing
                extended_state = Artifact.STATE_EX_CLOSING;

                // Proceed closing sequence
                local_thread = new CloseTransition ();
                local_thread.start ();
                return (true);
            }
        }
        return (false);
    }

    @Override
    public boolean update ()
    {
        // TODO: HANDLE Bundles/ AND Resources/
        return (bundleManager.updateBundle (main_bundle));
    }

    @Override
    public boolean refresh ()
    {
        return (false);
        // TODO: HANDLE Bundles/ AND Resources/
        //return (bundleManager.refreshBundle (main_bundle));
    }

    @Override
    public boolean uninstall ()
    {
        // TODO: HANDLE Bundles/ AND Resources/
        return (bundleManager.uninstallBundle (main_bundle));
    }

    private String get_state_str (Bundle bundle)
    {
        switch (bundle.getState ())
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

    private String get_event_str (BundleEvent bundleEvent)
    {
        if (bundleEvent == null)
        {
            return ("NULL_EVENT");
        }
        switch (bundleEvent.getType ())
        {
            case BundleEvent.INSTALLED:       return ("INSTALLED");
            case BundleEvent.LAZY_ACTIVATION: return ("LAZY_ACTIVATION");
            case BundleEvent.RESOLVED:        return ("RESOLVED");
            case BundleEvent.STARTED:         return ("STARTED");
            case BundleEvent.STARTING:        return ("STARTING");
            case BundleEvent.STOPPED:         return ("STOPPED");
            case BundleEvent.STOPPING:        return ("STOPPING");
            case BundleEvent.UNINSTALLED:     return ("UNINSTALLED");
            case BundleEvent.UNRESOLVED:      return ("UNRESOLVED");
            case BundleEvent.UPDATED:         return ("UPDATED");
        }
        return ("Unknown");
    }

    class OpenTransition extends Thread
    {
        public OpenTransition ()
        {
            setName ("Open_" + main_bundle.getSymbolicName () + "_" + main_bundle.getVersion ());
        }

        private void do_opening_transition ()
        {
            // Prepare and load embeddings context
            try
            {
                embedding_context.updateEmbeddings ().get ();
            }
            catch (InterruptedException | ExecutionException e)
            {
                log.error ("Exception opening bundle: {}", main_bundle, e);
            }

            // After Opening, we are Running
            extended_state = Artifact.STATE_EX_OPEN;
        }

        @Override
        public void run ()
        {
            try
            {
                // TODO: WE MAY ACTIVATE A TRANSIENT BUNDLE HERE
                if (main_bundle.getState () != Bundle.ACTIVE)
                {
                    log.info ("Waiting for package {} activation (state = {})",
                        main_bundle, get_state_str (main_bundle));

                    // TODO: WE MAY USE BUNDLE LISTENERS
                    while (main_bundle.getState () != Bundle.ACTIVE)
                    {
                        try
                        {
                            Thread.sleep (100);
                        }
                        catch (InterruptedException interrupt)
                        {
                            break;
                        }
                    }
                }

                log.info ("Opening package {} (state = {})",
                    main_bundle, get_state_str (main_bundle));

                if (main_bundle.getState () == Bundle.ACTIVE)
                {
                    do_opening_transition ();
                }

                if (extended_state == Artifact.STATE_EX_ERROR)
                {
                    log.warn ("Package {} not opened (state = {})",
                        main_bundle, get_state_str (main_bundle));
                }
                else
                {
                    log.info ("Package {} opened (state = {})",
                        main_bundle, get_state_str (main_bundle));
                }
            }
            catch (Throwable transition_exception)
            {
                log.error ("Unhandled exception opening package {}", main_bundle, transition_exception);
            }
        }
    }

    class CloseTransition extends Thread
    {
        public CloseTransition ()
        {
            setName ("Close_" + main_bundle.getSymbolicName () + "_" + main_bundle.getVersion ());
        }

        private void do_closing_transition ()
        {
            extended_state = Artifact.STATE_EX_CLOSING;

            // Incredibly complex things....

            // Back to pure OSGi state
            extended_state = Artifact.STATE_EX_NONE;

            // Close all embeddings
            embedding_context.close ();
        }

        @Override
        public void run ()
        {
            try
            {
                log.info ("Closing package {} (state = {})",
                        main_bundle, get_state_str (main_bundle));

                do_closing_transition ();

                if (extended_state == Artifact.STATE_EX_ERROR)
                {
                    log.warn ("Package {} not closed (state = {})",
                            main_bundle, get_state_str (main_bundle));
                }
                else
                {
                    log.info ("Package {} closed (state = {})",
                            main_bundle, get_state_str (main_bundle));
                }
            }
            catch (Throwable transition_exception)
            {
                log.error ("Unhandled exception closing package {}", main_bundle, transition_exception);
            }
        }
    }
}

// EOF
