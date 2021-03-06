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

package org.lucidj.api.core;

import java.net.URL;
import java.util.Map;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;

public interface ServiceContext
{
    ServiceLocator newServiceLocator ();
    void register (Class objectClass);
    void register (Class objectClass, ServiceObject.Provider provider);
    void addListener (ServiceObject.Listener listener, Class filterClass);
    ServiceTracker addServiceTracker (Class objectClass, ServiceContext.TrackerListener listener);
    ServiceTracker addServiceTracker (String filter, ServiceContext.TrackerListener listener);
    ServiceTracker addServiceTracker (Filter filter, ServiceContext.TrackerListener listener);

    Object newServiceObject (String objectClassName, Map<String, Object> properties);
    Object newServiceObject (String objectClassName);
    <T> T  newServiceObject (Class<T> objectClass, Map<String, Object> properties);
    <T> T  newServiceObject (Class<T> objectClass);
    <T> T  wrapObject       (Class<T> objectClass, Object serviceObject);

    <T> T    getService (BundleContext context, Class<T> type);
    <T> void putService (BundleContext context, Class<T> type, T service);

    ServiceRegistration publishUrl (BundleContext context, String localUrl);
    ServiceRegistration publishUrl (BundleContext context, URL url);

    interface TrackerListener
    {
        void bind (Object service, ServiceReference ref);
        void unbind (Object service, ServiceReference ref);
        void modified (Object service, ServiceReference ref);
    }
}

// EOF
