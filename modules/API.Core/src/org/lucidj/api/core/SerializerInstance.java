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

public interface SerializerInstance
{
    void                setValue         (String representation);
    String              getValue         ();
    SerializerInstance  addObject        (Object object);
    Object[]            getObjects       ();
    SerializerInstance  setProperty      (String key, Object object);
    SerializerInstance  setAttribute     (String property, String attribute, Object object);
    Object              getProperty      (String key);
    Object              getAttribute     (String property, String attribute);
    Object[]            getArrayProperty (String key);
    String[]            getPropertyKeys  ();
    void                setPropertyKey   (String key);
    SerializerInstance  setObjectClass   (Class clazz);
    SerializerInstance  setObjectClass   (String clazz);
    boolean             serializeAs      (Object object, String clazz);
    Object              deserializeAs    (String clazz);
}

// EOF
