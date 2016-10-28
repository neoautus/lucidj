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

package org.lucidj.api;

import java.nio.file.Path;

import org.osgi.framework.BundleContext;

public interface TaskContext
{
    BundleContext getBundleContext ();
    <A> A currentTask (Class<A> type);
    Task currentTask ();
    void putObject (Object obj);
    <T> T getObject (Class<T> obj_class);
    void publishObject (String identifier, Object obj);
    void unpublishObject (String identifier);
    Object getPublishedObject (String identifier);
    boolean load (Path source_path);
    boolean save (Path destination_path);
    boolean save ();
    ClassLoader getClassLoader ();
}

// EOF
