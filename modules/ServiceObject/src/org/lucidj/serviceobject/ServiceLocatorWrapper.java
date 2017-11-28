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

package org.lucidj.serviceobject;

import org.lucidj.api.core.ServiceLocator;
import org.lucidj.api.core.ServiceObjectException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

// Wrapper around the real ServiceLocator
public class ServiceLocatorWrapper implements ServiceLocator
{
    private final static Logger log = LoggerFactory.getLogger (ServiceLocator.class);

    private BundleContext bundleContext = null;
    private Set<ServiceReference> serviceReferences = new HashSet<> ();

    public ServiceLocatorWrapper ()
    {
        this (ServiceLocator.class);
    }

    public ServiceLocatorWrapper (Class bundleClass)
    {
        this (FrameworkUtil.getBundle (bundleClass).getBundleContext ());
    }

    public ServiceLocatorWrapper (BundleContext context)
    {
        this.bundleContext = context;
        log.info ("!ServiceLocatorWrapper! context = {}", context);
    }

    public BundleContext getBundleContext ()
    {
        return (bundleContext);
    }

    public <T> T getService (Class<T> serviceClass)
        throws ServiceObjectException
    {
        log.info ("getService (serviceClass={})", serviceClass);
        try
        {
            ServiceReference serviceReference = bundleContext.getServiceReference (serviceClass);

            log.info ("serviceReference = {}", serviceReference);

            serviceReferences.add (serviceReference);

            return ((T)bundleContext.getService (serviceReference));
        }
        catch (Exception e)
        {
            log.error ("Exception on getService()", e);
            throw (new ServiceObjectException (serviceClass));
        }
    }

    public void close ()
    {
        log.info ("close(): context={} serviceReferences={}", bundleContext, serviceReferences);

        if (bundleContext != null)
        {
            for (ServiceReference ref: serviceReferences)
            {
                bundleContext.ungetService (ref);
            }
        }
    }
}

// EOF
