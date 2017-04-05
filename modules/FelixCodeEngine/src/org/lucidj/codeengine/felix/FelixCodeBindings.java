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

import org.lucidj.api.CodeBindings;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class FelixCodeBindings implements CodeBindings
{
    private Map<String, Object> bindings = new HashMap<> ();

    @Override
    public Object put (String name, Object value)
    {
        return (bindings.put (name, value));
    }

    @Override
    public void putAll (Map<? extends String, ? extends Object> toMerge)
    {
        bindings.putAll (toMerge);
    }

    @Override
    public void clear ()
    {
        bindings.clear ();
    }

    @Override
    public Set<String> keySet ()
    {
        return (bindings.keySet ());
    }

    @Override
    public Collection<Object> values ()
    {
        return (bindings.values ());
    }

    @Override
    public Set<Entry<String, Object>> entrySet ()
    {
        return (bindings.entrySet ());
    }

    @Override
    public int size ()
    {
        return (bindings.size ());
    }

    @Override
    public boolean isEmpty ()
    {
        return (bindings.isEmpty ());
    }

    @Override
    public boolean containsKey (Object key)
    {
        return (bindings.containsKey (key));
    }

    @Override
    public boolean containsValue (Object value)
    {
        return (bindings.containsValue (value));
    }

    @Override
    public Object get (Object key)
    {
        return (bindings.get (key));
    }

    @Override
    public Object remove (Object key)
    {
        return (bindings.remove (key));
    }
}

// EOF
