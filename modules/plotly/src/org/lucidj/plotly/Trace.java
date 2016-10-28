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

package org.lucidj.plotly;

import org.lucidj.api.Quark;

import java.util.HashMap;
import java.util.Map;

public class Trace implements Quark
{
    private HashMap properties = new HashMap<> ();

    protected JsonMap json = new JsonMap ();

    public Stream stream = new Stream ();

    public Map getLinkableMap ()
    {
        return (json.linkableMap ());
    }

    public Trace name (String name)
    {
        json.set ("name", name);
        return (this);
    }

    public Trace visible (Boolean flag)
    {
        json.set ("visible", flag);
        return (this);
    }

    @Override
    public Map<String, Object> serializeObject ()
    {
        properties.put ("/", json.toPrettyJson ());
        return (properties);
    }

    @Override
    public void deserializeObject (Map<String, Object> properties)
    {
        this.properties.putAll (properties);
        json.fromJson ((String)properties.get ("/"));
    }
}

// EOF
