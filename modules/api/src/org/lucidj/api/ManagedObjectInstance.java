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

/* ManagedObjects are created and managed by OSGi services. Their lifecycle is tied with
 * the underlining service. These objects may have a predefined set of properties.
 */

import org.osgi.framework.Bundle;

public interface ManagedObjectInstance
{
    String PROVIDER = "object-provider";
    String CLASS = "object-class";

    // Null if the object is no more available. Do NOT hold object
    <A> A adapt (Class<A> type);

    Bundle getBundle ();

    String[] getPropertyKeys ();
    boolean  containsKey     (String key);
    Object   getProperty     (String key);
    Class<?> getPropertyType (String key);
    void     setProperty     (String key, Object value);
    <T> T    getObject       (Class<T> type);
    <T> void putObject       (Class<T> type, T obj);
}

// EOF
