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

import javax.script.ScriptEngineFactory;

import java.util.List;

import org.osgi.framework.Bundle;

public interface CodeEngineManager
{
//    void registerEngineExtension (String extension, CodeEngineProvider factory);
//    void registerEngineMimeType (String type, CodeEngineProvider factory);
    void registerEngineName (String name, CodeEngineProvider factory);
    void registerEngine (ScriptEngineFactory factory);

    // Get/set the value for the specified key in the Global Scope
//    Object get (String key);
//    void put (String key, Object value);

    // getBindings returns the value of the globalScope field.
//    CodeBindings getBindings ();
//    void setBindings (CodeBindings bindings);

    // Look up and create a ScriptEngine for a given extension
//    ScriptEngine getEngineByExtension (String extension);
//    ScriptEngine getEngineByMimeType (String mimeType);
    CodeEngine getEngineByName (String shortName);
    List<String> getEngines ();

    CodeContext newContext (Bundle parentBundle);
}

// EOF
