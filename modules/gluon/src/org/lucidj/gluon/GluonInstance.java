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

package org.lucidj.gluon;

import org.lucidj.api.Serializer;
import org.lucidj.api.SerializerInstance;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class GluonInstance implements SerializerInstance
{
    static final AtomicLong NEXT_ID = new AtomicLong(0);
    final long id = NEXT_ID.getAndIncrement();

    private GluonSerializer serializer;

    private boolean is_resolved = false;
    private Object backing_object = null;
    private String object_representation = null;
    private String name = null;
    private GluonInstance next = null;
    private GluonInstance children = null;
    private Serializer selected_serializer = null;

    public GluonInstance (GluonSerializer parent)
    {
        this.serializer = parent;
    }



    public boolean isObject ()
    {
        return (backing_object instanceof GluonInstance);
    }

    public String[] getPropertyKeys ()
    {
        List<String> keys = new ArrayList<> ();

        for (GluonInstance entry = this.children; entry != null; entry = entry.next)
        {
            if (entry.name != null)
            {
                keys.add (entry.name);
            }
        }
        return (keys.toArray (new String [0]));
    }

    public GluonInstance newInstance ()
    {
        return (new GluonInstance (serializer));
    }

    private GluonInstance newChildInstance ()
    {
        GluonInstance instance = newInstance ();

        if (children != null)
        {
            instance.next = children;
        }
        children = instance;
        return (instance);
    }

    public GluonInstance getPropertyEntry (String key)
    {
        for (GluonInstance entry = this.children; entry != null; entry = entry.next)
        {
            if (entry.name != null && entry.name.equals (key))
            {
                return (entry);
            }
        }
        return (null);
    }

    @Override
    public void setPropertyKey (String name)
    {
        this.name = name;
    }

    public GluonInstance getOrCreatePropertyEntry (String key)
    {
        GluonInstance entry = getPropertyEntry (key);

        if (entry == null)
        {
            // Create empty property entry
            entry = newChildInstance ();
            entry.name = key;
        }
        return (entry);
    }

    public boolean containsKey (String key)
    {
        return (getPropertyEntry (key) != null);
    }

    public boolean isPrimitive ()
    {
        return (!containsKey (GluonConstants.OBJECT_CLASS));
    }

    @Override
    public SerializerInstance setProperty (String key, Object object)
    {
        GluonInstance entry = getOrCreatePropertyEntry (key);
        serializer.applySerializer (entry, object);
        return (entry);
    }

    @Override
    public SerializerInstance setObjectClass (Class clazz)
    {
        return (setProperty (GluonConstants.OBJECT_CLASS, clazz.getName ()));
    }

    @Override
    public SerializerInstance setAttribute (String property, String attribute, Object object)
    {
        GluonInstance entry = getOrCreatePropertyEntry (property);
        return (entry.setProperty (attribute, object));
    }

    public SerializerInstance _setPropertyRepresentation (String key, String representation)
    {
        GluonInstance entry = getOrCreatePropertyEntry (key);
        serializer.applyDeserializer (entry, representation);
        return (entry);
    }

    public SerializerInstance _setAttributeRepresentation (String property, String attribute, String representation)
    {
        GluonInstance entry = getOrCreatePropertyEntry (property);
        return (entry._setPropertyRepresentation (attribute, representation));
    }

    public Object getProperty (String key)
    {
        GluonInstance property = getPropertyEntry (key);
        return ((property == null)? null: property.backing_object);
    }

    public Object getAttribute (String property, String attribute)
    {
        GluonInstance entry = getPropertyEntry (property);
        return ((entry == null)? null: entry.getProperty (attribute));
    }

    public String getPropertyRepresentation (String key)
    {
        GluonInstance property = getPropertyEntry (key);
        return ((property == null)? null: property.object_representation);
    }

    @Override
    public void setValue (String representation)
    {
        this.object_representation = representation;
    }

    public void _setValueObject (Object object)
    {
        backing_object = object;
    }

    public String getValue ()
    {
        return (object_representation);
    }

    public Object _getValueObject ()
    {
        return (backing_object);
    }

    @Override
    public SerializerInstance addObject (Object object)
    {
        GluonInstance entry = newChildInstance ();

        serializer.applySerializer (entry, object);

        return (entry);
    }

    @Override
    public Object[] getObjects ()
    {
        List<Object> objects = new ArrayList<> ();

        for (GluonInstance entry = this.children; entry != null; entry = entry.next)
        {
            if (entry.name == null)
            {
                objects.add (0, entry.backing_object);
            }
        }
        return (objects.toArray (new Object[0]));
    }

    public List<GluonInstance> getObjectEntries ()
    {
        List<GluonInstance> object_entries = new ArrayList<> ();

        for (GluonInstance entry = this.children; entry != null; entry = entry.next)
        {
            if (entry.name == null)
            {
                object_entries.add (0, entry);
            }
        }
        return (object_entries);
    }

    public boolean hasObjects ()
    {
        for (GluonInstance entry = this.children; entry != null; entry = entry.next)
        {
            if (entry.name == null)
            {
                return (true);
            }
        }
        return (false);
    }
}

// EOF
