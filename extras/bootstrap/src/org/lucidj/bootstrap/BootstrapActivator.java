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

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.service.http.HttpService;
import org.osgi.util.tracker.ServiceTracker;

public class BootstrapActivator extends Thread implements BundleActivator
{
    TinyLog log = new TinyLog ();

    private BootstrapDeployer deployer = null;
    private ServiceTracker<HttpService, HttpService> httpServiceTracker;

    public void start (BundleContext context)
        throws Exception
    {
        log.info ("Starting Bootstrap service...");

        deployer = new BootstrapDeployer (context);
        deployer.setDaemon (true);
        deployer.start();

        httpServiceTracker = new HttpServiceTracker (context, deployer);
        httpServiceTracker.open();

        log.info ("Bootstrap service started");
    }

    public void stop (BundleContext context)
        throws Exception
    {
        log.info ("Stoping Bootstrap service...");

        httpServiceTracker.close ();
        httpServiceTracker = null;

        if (deployer != null)
        {
            deployer.close ();
        }

        log.info ("Bootstrap service stopped");
    }
}

// EOF
