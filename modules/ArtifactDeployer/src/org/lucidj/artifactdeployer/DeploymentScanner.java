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

package org.lucidj.artifactdeployer;

import org.lucidj.api.ArtifactDeployer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Context;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;

@Component (immediate = true, publicFactory = false)
@Instantiate
public class DeploymentScanner implements Runnable
{
    private final static Logger log = LoggerFactory.getLogger (DeploymentScanner.class);

    @Context
    private BundleContext context;

    @Requires
    private ArtifactDeployer artifactDeployer;

    private Map<String, Exception> troubled_artifacts = new HashMap<> ();

    private String watched_directory;
    private Thread poll_thread;
    private int thread_poll_ms = 1000;

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

            Bundle bnd = artifactDeployer.getArtifactByLocation (package_uri);

            if (bnd == null) // The bundle isn't installed yet
            {
                try
                {
                    artifactDeployer.installArtifact (package_uri);
                    troubled_artifacts.remove (package_uri); // Just in case
                }
                catch (Exception e)
                {
                    if (!troubled_artifacts.containsKey (package_uri))
                    {
                        // Show only first time exceptions
                        log.warn ("{}", e.getMessage ());
                    }

                    // Store all last exceptions for every troublesome artifact
                    troubled_artifacts.put (package_uri, e);
                }
            }
        }
    }

    @Validate
    private void validate ()
    {
        // Configuration
        // TODO: THIS SHOULD BE RECONFIGURABLE!!!
        watched_directory = System.getProperty ("system.home") + "/apps";

        // Start things
        poll_thread = new Thread (this);
        poll_thread.setName (this.getClass ().getSimpleName ());
        poll_thread.start ();

        log.info ("DeploymentScanner started: applications dir = {}", watched_directory);
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

        log.info ("DeploymentScanner stopped");
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
