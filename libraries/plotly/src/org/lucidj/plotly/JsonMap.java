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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.internal.LinkedTreeMap;

import java.util.HashMap;
import java.util.Map;

public class JsonMap
{
    private transient Gson gson = new Gson ();
    private transient Gson pretty = new GsonBuilder ().setPrettyPrinting().create();

    private LinkedTreeMap tree_map = new LinkedTreeMap ();
    private String json_map = "{}";
    private JsonMapListener listener;

    public void fromJson (String json_string)
    {
        json_map = json_string;
        tree_map = gson.fromJson (json_string, LinkedTreeMap.class);
    }

    public String toJson ()
    {
        if (json_map == null)
        {
            json_map = gson.toJson (tree_map);
        }
        return (json_map);
    }

    public String toPrettyJson ()
    {
        if (json_map == null)
        {
            json_map = pretty.toJson (tree_map);
        }
        return (json_map);
    }

    public Map linkableMap ()
    {
        return (tree_map);
    }

    public void setJsonChanged ()
    {
        // Generate new JSON
        json_map = null;

        if (listener != null)
        {
            listener.changed (this);
        }
    }

    @SuppressWarnings("unchecked")
    public Map getJsonSubmap (Map map, String key)
    {
        Map submap = (Map)map.get (key);

        if (submap == null)
        {
            submap = new HashMap<> ();
            map.put (key, submap);
        }

        return (submap);
    }

    public Map getJsonSubmap (String key)
    {
        return (getJsonSubmap (tree_map, key));
    }

    @SuppressWarnings("unchecked")
    public void setJsonProperty (String key, Object value)
    {
        Map current_map = tree_map;

        if (key.contains ("."))
        {
            String[] keys = key.split ("\\.");

            for (int i = 0; i < keys.length - 1; i++)
            {
                current_map = getJsonSubmap (current_map, keys [i]);
            }

            key = keys [keys.length - 1];
        }

        current_map.put (key, value);
        setJsonChanged ();
    }

    public void set (String key, boolean value)
    {
        setJsonProperty (key, value);
    }

    public void set (String key, int value)
    {
        setJsonProperty (key, value);
    }

    public void set (String key, String value)
    {
        setJsonProperty (key, value);
    }

    public void set (String key, Double value)
    {
        setJsonProperty (key, value);
    }

    public void set (String key, Map map)
    {
        setJsonProperty (key, map);
    }

    public void set (String key, Object[] value)
    {
        setJsonProperty (key, value);
    }

    public void setListener (JsonMapListener listener)
    {
        this.listener = listener;
    }

    public interface JsonMapListener
    {
        void changed (JsonMap source);
    }
}

// EOF
