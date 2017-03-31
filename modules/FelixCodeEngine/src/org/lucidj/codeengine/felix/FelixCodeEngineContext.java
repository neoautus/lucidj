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

package org.lucidj.codeengine.felix;

import org.lucidj.api.CodeEngine;
import org.lucidj.api.CodeEngineContext;
import org.lucidj.api.CodeEngineManager;
import org.lucidj.api.ManagedObjectInstance;

import java.util.HashMap;
import java.util.Map;

import org.osgi.framework.Bundle;

public class FelixCodeEngineContext implements CodeEngineContext
{
    private Map<String, Object> properties = new HashMap<> ();

    private Bundle bundle;
    private FelixCodeEngineManager engineManager;

    public FelixCodeEngineContext (Bundle bundle, CodeEngineManager engineManager)
    {
        this.bundle = bundle;
        this.engineManager = (FelixCodeEngineManager)engineManager;
    }

    @Override
    public Bundle getBundle ()
    {
        return (bundle);
    }

    @Override
    public <T> T getObject (Class<T> type)
    {
        return (type.cast (properties.get (type.getName ())));
    }

    @Override
    public <T> void putObject (Class<T> type, T obj)
    {
        properties.put (type.getName (), obj);
    }

    @Override
    public CodeEngine getEngineByName (String shortName)
    {
        // Create a CodeEngine tied to this context
        return (engineManager._getEngineByName (shortName, this));
    }

    @Override
    public void validate (ManagedObjectInstance instance)
    {
        // Nop
    }

    @Override
    public void invalidate (ManagedObjectInstance instance)
    {
        // Nop
    }
}

// EOF
