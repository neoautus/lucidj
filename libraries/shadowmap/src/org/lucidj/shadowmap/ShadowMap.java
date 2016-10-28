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

package org.lucidj.shadowmap;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShadowMap
{
    private transient HashMap<String, Object> custom_fields = new HashMap<> ();

    @SuppressWarnings("unchecked")
    private boolean set_layout_option (String key, Object value)
    {
        Object map_object = this;

        if (key.contains ("."))
        {
            String[] keys = key.split ("\\.");

            for (int i = 0; i < keys.length - 1; i++)
            {
                String current_key = keys[i];
                Object next_map = null;

                // The map_object have the required key field?
                try
                {
                    Field local_field = map_object.getClass ().getDeclaredField (current_key);
                    next_map = local_field.get (map_object);
                }
                catch (Exception ignore) {};

                // No, it may be either a ShadowMap or any Map
                if (next_map == null)
                {
                    if (map_object instanceof ShadowMap)
                    {
                        if ((next_map = ((ShadowMap)map_object).getCustomFields ().get (current_key)) == null)
                        {
                            next_map = new HashMap ();
                            ((ShadowMap)map_object).set (current_key, next_map);
                        }
                    }

                    if (map_object instanceof Map)
                    {
                        if ((next_map = ((Map)map_object).get (current_key)) == null)
                        {
                            next_map = new HashMap ();
                            ((Map)map_object).put (current_key, next_map);
                        }
                    }
                }

                if (next_map == null)
                {
                    return (false);
                }

                map_object = next_map;
            }

            // Assumes the last key in the chain
            key = keys [keys.length - 1];
        }

        // Set the key field value
        try
        {
            Field target_field = map_object.getClass ().getDeclaredField (key);
            target_field.set (map_object, value);
        }
        catch (NoSuchFieldException e)
        {
            // Field doesn't exists, set the ShadowMap...
            if (map_object instanceof ShadowMap)
            {
                ((ShadowMap)map_object).getCustomFields ().put (key, value);
            }

            // ... or any map
            if (map_object instanceof Map)
            {
                ((Map)map_object).put (key, value);
            }
        }
        catch (IllegalAccessException e)
        {
            // Cannot write field
            return (false);
        }

        return (true);
    }

    public Map<String, Object> getCustomFields ()
    {
        return (custom_fields);
    }

    public void set (String key, boolean value)
    {
        set_layout_option (key, value);
    }

    public void set (String key, int value)
    {
        set_layout_option (key, value);
    }

    public void set (String key, String value)
    {
        set_layout_option (key, value);
    }

    public void set (String key, Object value)
    {
        set_layout_option (key, value);
    }

    public void set (String key, List value)
    {
        set_layout_option (key, value.toArray ());
    }
}

// EOF
