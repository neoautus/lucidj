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

import java.util.Map;

public interface ManagedObjectFactory
{
    // TODO: REQUEST A GROUP OF INTERFACES/ASPECTS
    ManagedObjectInstance register (String clazz, ManagedObjectProvider provider, Map<String, Object> properties);
    ManagedObjectInstance register (Class clazz, ManagedObjectProvider provider, Map<String, Object> properties);
    ManagedObjectInstance[] getManagedObjects (String clazz, String filter);
    ManagedObjectInstance[] getManagedObjects (Class clazz, String filter);
    ManagedObjectInstance wrapObject (ManagedObject managed_object);
    ManagedObjectInstance newInstance (String clazz, Map<String, Object> properties);
    ManagedObjectInstance newInstance (Class clazz, Map<String, Object> properties);
    ManagedObjectInstance newInstance (ManagedObjectInstance descriptor);
}

// EOF
