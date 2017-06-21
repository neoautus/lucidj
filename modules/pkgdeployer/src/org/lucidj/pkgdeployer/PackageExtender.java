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

import org.lucidj.api.EmbeddingManager;
import org.lucidj.api.Package;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.BundleTracker;
import org.osgi.util.tracker.BundleTrackerCustomizer;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Context;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;

@Component
@Instantiate
public class PackageExtender implements BundleTrackerCustomizer<ServiceRegistration<Package>>
{
    private final static transient Logger log = LoggerFactory.getLogger (PackageExtender.class);

    private BundleTracker bundleTracker;

    @Requires
    private EmbeddingManager embeddingManager;

    @Context
    private BundleContext context;

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

    @Validate
    private void validate ()
    {
        bundleTracker = new BundleTracker<> (context, Bundle.ACTIVE, this);
        bundleTracker.open();
        log.info ("PackageExtender started");
    }

    @Invalidate
    private void invalidate ()
    {
        bundleTracker.close ();
        bundleTracker = null;
        log.info ("PackageExtender stopped");
    }

    @Override
    public ServiceRegistration<Package> addingBundle (Bundle bundle, BundleEvent bundleEvent)
    {
        log.debug ("#####> addingBundle (bundle={}, state={} bundleEvent={} / {}", bundle, get_state_str (bundle), bundleEvent, get_event_str (bundleEvent));

        Dictionary headers = bundle.getHeaders ();
        String x_package = (String)headers.get ("X-Package");
        BundleContext bundle_context = bundle.getBundleContext ();

        if (x_package == null)
        {
            return (null);
        }

        Package pkg = new PackageImpl (bundle, embeddingManager);

        log.debug ("#####> bnd={} ctx={} exstat={}", bundle, bundle_context, pkg);

        // The returned bundle must have an Package service
        // registered, so the status changes can be properly tracked
        return (bundle_context.registerService (Package.class, pkg, null));
    }

    @Override
    public void modifiedBundle (Bundle bundle, BundleEvent bundleEvent,
        ServiceRegistration<Package> pkg_sreg)
    {
        log.debug ("#####> modifiedBundle (bundle={}, state={}, bundleEvent={} / {}, essr={}",
            bundle, get_state_str (bundle), bundleEvent, get_event_str (bundleEvent), pkg_sreg);
    }

    @Override
    public void removedBundle (Bundle bundle, BundleEvent bundleEvent,
        ServiceRegistration<Package> pkg_sreg)
    {
        log.debug ("#####> removedBundle (bundle={}, state={}, bundleEvent={} / {}, essr={}",
            bundle, get_state_str (bundle), bundleEvent, get_event_str (bundleEvent), pkg_sreg);
    }
}

// EOF
