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

package org.lucidj.api;

import java.util.Map;

import org.osgi.framework.BundleContext;

public interface ServiceContext
{
    ServiceLocator newServiceLocator ();
    void register (Class objectClass);
    void register (Class objectClass, ServiceObject.Provider provider);
    void addListener (ServiceObject.Listener listener, Class filterClass);

    Object newServiceObject (String objectClassName, Map<String, Object> properties);
    Object newServiceObject (String objectClassName);
    <T> T  newServiceObject (Class<T> objectClass, Map<String, Object> properties);
    <T> T  newServiceObject (Class<T> objectClass);
    <T> T  wrapObject       (Class<T> objectClass, Object serviceObject);

    <T> T    getService (BundleContext context, Class<T> type);
    <T> void putService (BundleContext context, Class<T> type, T service);
}

// EOF
