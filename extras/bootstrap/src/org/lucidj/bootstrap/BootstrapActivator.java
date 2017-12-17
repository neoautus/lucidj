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

package org.lucidj.bootstrap;

import org.apache.karaf.features.BootFinished;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpService;
import org.osgi.util.tracker.ServiceTracker;

public class BootstrapActivator extends Thread implements BundleActivator
{
    private TinyLog log = new TinyLog ();

    private BootFinishedTracker boot_finished_tracker;
    private ServiceTracker<HttpService, HttpService> httpServiceTracker;
    private BootstrapDeployer deployer = null;

    public void start (BundleContext context)
        throws Exception
    {
        log.info ("Starting Bootstrap monitor");
        boot_finished_tracker = new BootFinishedTracker (context);
        boot_finished_tracker.open();
    }

    public void stop (BundleContext context)
        throws Exception
    {
        log.info ("Stopping Bootstrap monitor");

        boot_finished_tracker.close ();
        boot_finished_tracker = null;

        httpServiceTracker.close ();
        httpServiceTracker = null;

        if (deployer != null)
        {
            deployer.close ();
        }
    }

    //-------------------------------------------------------------------
    // IMPORTANT: SERVICE BootFinished IS REGISTERED WHEN KARAF FINISHES
    // STARTING IT'S INTERNAL FEATURES/BUNDLES, SO WE CAN KICK IN
    //-------------------------------------------------------------------
    public class BootFinishedTracker extends ServiceTracker<BootFinished, BootFinished>
    {
        public BootFinishedTracker (BundleContext context)
        {
            super (context, BootFinished.class.getName (), null);
        }

        @Override
        public BootFinished addingService (ServiceReference<BootFinished> reference)
        {
            log.info ("Framework is ready for deployments");

            deployer = new BootstrapDeployer (context);
            deployer.setDaemon (true);
            deployer.start();

            httpServiceTracker = new HttpServiceTracker (context, deployer);
            httpServiceTracker.open();

            return (super.addingService (reference));
        }
    }
}

// EOF
