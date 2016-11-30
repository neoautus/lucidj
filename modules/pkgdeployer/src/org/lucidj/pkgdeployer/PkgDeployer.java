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

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
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

            Bundle bnd = bnd_deployer.getBundleByLocation (package_uri);

            if (bnd == null) // The bundle isn't installed yet
            {
                bnd_deployer.installBundle (package_uri);
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
