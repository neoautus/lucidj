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
import org.lucidj.api.Package;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.Enumeration;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;

public class PackageImpl implements Package, ServiceListener, Runnable
{
    private final static transient Logger log = LoggerFactory.getLogger (PackageImpl.class);

    private Bundle bnd;
    private BundleContext context;
    private ServiceReference local_reference;
    private Thread local_thread;
    private volatile int extended_state;

    public PackageImpl (Bundle bnd)
    {
        this.bnd = bnd;
        context = bnd.getBundleContext ();
        context.addServiceListener (this);
    }

    public void setState (int state)
    {
        extended_state = state;
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

    @Override
    public Bundle getBundle ()
    {
        return (bnd);
    }

    @Override
    public int getState ()
    {
        int bundle_state = bnd.getState ();

        // Extended states are non-zero and derive from valid ACTIVE states
        if (bundle_state == Bundle.ACTIVE && extended_state != 0)
        {
            return (extended_state);
        }
        return (bundle_state);
    }

    private void do_opening_transition ()
    {
        // Assert the state...
        extended_state = Artifact.STATE_EX_OPENING;

        // List ALL bundle entries
        Enumeration<URL> e = bnd.findEntries ("/", null, true);

        // Find a specific localization file
        while (e.hasMoreElements ())
        {
            URL entry = e.nextElement ();
            log.info ("{} ----> {}", bnd, entry.toString ());
        }

        // After Opening, we are Running
        extended_state = Artifact.STATE_EX_OPEN;
    }

    private void do_closing_transition ()
    {
        extended_state = Artifact.STATE_EX_CLOSING;

        // Incredibly complex things....

        // Back to pure OSGi state
        extended_state = 0;
    }

    @Override
    public void run ()
    {
        log.info ("Starting package {} (state = {})", bnd, get_state_str (bnd));
        do_opening_transition ();
    }

    @Override
    public void serviceChanged (ServiceEvent serviceEvent)
    {
        ServiceReference reference = serviceEvent.getServiceReference ();

        if (local_reference == null)
        {
            if (serviceEvent.getType () == ServiceEvent.REGISTERED
                && reference.getBundle () == bnd)
            {
                if (context.getService (reference) == this)
                {
                    local_reference = reference;

                    local_thread = new Thread (this);
                    local_thread.setName ("P/" + bnd.getSymbolicName () + "/" + bnd.getVersion ());
                    local_thread.start ();
                }
                context.ungetService (reference);
            }
        }
        else if (reference == local_reference)
        {
            if (serviceEvent.getType () == ServiceEvent.UNREGISTERING)
            {
                log.info ("=====> THIS SERVICE IS UNREGISTERING: reference={}", local_reference);
            }
        }
    }
}

// EOF
