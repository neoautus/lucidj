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

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.internal.LinkedTreeMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ShadowMapSerializer implements JsonSerializer, JsonDeserializer
{
    final Logger log = LoggerFactory.getLogger (ShadowMapSerializer.class);

    private String get_modifiers (int modifiers)
    {
        String modifier_list = "";

        if ((modifiers & Modifier.ABSTRACT) != 0)
        {
            modifier_list += "ABSTRACT ";
        }
        if ((modifiers & Modifier.FINAL) != 0)
        {
            modifier_list += "FINAL ";
        }
        if ((modifiers & Modifier.INTERFACE) != 0)
        {
            modifier_list += "INTERFACE ";
        }
        if ((modifiers & Modifier.NATIVE) != 0)
        {
            modifier_list += "NATIVE ";
        }
        if ((modifiers & Modifier.PRIVATE) != 0)
        {
            modifier_list += "PRIVATE ";
        }
        if ((modifiers & Modifier.PROTECTED) != 0)
        {
            modifier_list += "PROTECTED ";
        }
        if ((modifiers & Modifier.PUBLIC) != 0)
        {
            modifier_list += "PUBLIC ";
        }
        if ((modifiers & Modifier.STATIC) != 0)
        {
            modifier_list += "STATIC ";
        }
        if ((modifiers & Modifier.STRICT) != 0)
        {
            modifier_list += "STRICT ";
        }
        if ((modifiers & Modifier.SYNCHRONIZED) != 0)
        {
            modifier_list += "SYNCHRONIZED ";
        }
        if ((modifiers & Modifier.TRANSIENT) != 0)
        {
            modifier_list += "TRANSIENT ";
        }
        if ((modifiers & Modifier.VOLATILE) != 0)
        {
            modifier_list += "VOLATILE ";
        }
        return (modifier_list);
    }

    @Override
    public JsonElement serialize (Object obj, Type type, JsonSerializationContext jsctx)
    {
        final JsonObject jsonObject = new JsonObject ();

        Field[] field_list = obj.getClass ().getDeclaredFields ();

        log.info ("################## o = {}", obj.getClass ().getCanonicalName ());

        // First write all tangible fields from this object
        for (Field field: field_list)
        {
            log.info ("FIELD field={}, modifiers={}", field, get_modifiers (field.getModifiers ()));

            if ((field.getModifiers () & (Modifier.PRIVATE | Modifier.TRANSIENT)) != 0 ||
                field.isSynthetic ())
            {
                continue;
            }

            try
            {
                jsonObject.add (field.getName (), jsctx.serialize (field.get (obj)));
            }
            catch (IllegalAccessException e)
            {
                log.error ("Error serializing field {}", field, e);
            }
        }

        // This is a ShadowMap object anyway
        final ShadowMap smap = (ShadowMap)obj;

        // Now serialize every overflowed property we may have on this object's shadow
        for (Map.Entry<String, Object> entry: smap.getCustomFields ().entrySet ())
        {
            log.info ("SHADOW field={}, value={}", entry.getKey (), jsctx.serialize (entry.getValue ()));
            jsonObject.add (entry.getKey (), jsctx.serialize (entry.getValue ()));
        }

        return (jsonObject);
    }

    @Override
    public Object deserialize (JsonElement root, Type type, JsonDeserializationContext jsctx)
        throws JsonParseException
    {
        JsonObject jsonObject = root.getAsJsonObject();

        Class raw_type = (Class)type;
        ShadowMap map_object = null;

        log.info ("raw_type = {}", raw_type);

        try
        {
            map_object = (ShadowMap)raw_type.newInstance ();
        }
        catch (IllegalAccessException | InstantiationException e)
        {
            log.error ("Error instantiating ShadowMap", e);
            return (null);
        }

        log.info ("## type = {}", type);

        for (Map.Entry<String, JsonElement> entry: jsonObject.entrySet ())
        {
            String key = entry.getKey ();
            JsonElement el = entry.getValue ();
            Class field_class = LinkedTreeMap.class;

            // We use the field to match json incoming type
            try
            {
                Field local_field = raw_type.getField (key);
                field_class = local_field.getType ();
            }
            catch (NoSuchFieldException ignore) {};

            log.info ("key={}, el={}, cls={}", key, el, field_class);

            if (el.isJsonPrimitive ())
            {
                JsonPrimitive prim = el.getAsJsonPrimitive ();

                if (String.class.equals (field_class) ||
                    prim.isString ())
                {
                    map_object.set (key, el.getAsString ());
                }
                else if (Integer.class.equals (field_class) ||
                         int.class.equals (field_class) ||
                         prim.isNumber ())
                {
                    map_object.set (key, el.getAsInt ());
                }
                else if (Boolean.class.equals (field_class) ||
                         boolean.class.equals (field_class) ||
                         prim.isBoolean ())
                {
                    map_object.set (key, el.getAsBoolean ());
                }
                else
                {
                    // Log error and ignores the field
                    log.error ("Error: {} not handled for {}", field_class, el);
                }
            }
            else if (el.isJsonObject ())
            {
                map_object.set (key, (Object)jsctx.deserialize (el, field_class));
            }
            else if (el.isJsonNull ())
            {
                map_object.set (key, (Object)null);
            }
            else if (el.isJsonArray ())
            {
                JsonArray js_arr = el.getAsJsonArray ();

                if (js_arr.size () > 0)
                {
                    // We use the base type of the first argument...
                    JsonPrimitive prim = js_arr.get (0).getAsJsonPrimitive ();
                    field_class = prim.isString ()? String.class:
                                  prim.isNumber ()? Integer.class:
                                  prim.isBoolean ()? Boolean.class:
                                  Object.class;
                }
                else
                {
                    // ...or simply set as Object
                    field_class = Object.class;
                }

                // Safe fallback
                Class ref_class = Object[].class;

                // Build a reference class compatible with an Array
                try
                {
                    ref_class = Class.forName ("[L" + field_class.getCanonicalName () + ";");
                }
                catch (Exception ignore) {};

                // Fetch the array
                map_object.set (key, (Object)jsctx.deserialize (el, ref_class));
            }
        }

        return (map_object);
    }
}

// EOF
