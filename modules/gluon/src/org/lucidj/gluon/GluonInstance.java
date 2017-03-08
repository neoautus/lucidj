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

import org.lucidj.api.SerializerInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class GluonInstance implements SerializerInstance
{
    private final static transient Logger log = LoggerFactory.getLogger (GluonInstance.class);

    private GluonSerializer serializer;

    private Object backing_object = null;
    private String object_representation = null;
    private String name = null;
    private GluonInstance root = null;
    private GluonInstance next = null;
    private GluonInstance children = null;

    public GluonInstance (GluonSerializer serializer)
    {
        this.serializer = serializer;
        this.root = this;
    }

    public GluonInstance (GluonInstance parent)
    {
        this.serializer = parent.serializer;
        this.root = parent.root;
    }

    public GluonInstance getRoot ()
    {
        return (root);
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
        return (new GluonInstance (this));
    }

    public GluonInstance newChildInstance ()
    {
        GluonInstance instance = newInstance ();

        instance.next = children;
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

    public void removeEntry (GluonInstance entry_to_remove)
    {
        if (this.children == entry_to_remove)
        {
            this.children = entry_to_remove.next;
        }
        else
        {
            for (GluonInstance entry = this.children; entry.next != null; entry = entry.next)
            {
                if (entry.next == entry_to_remove)
                {
                    entry.next = entry_to_remove.next;
                    break;
                }
            }
        }
    }

    @Override
    public SerializerInstance setProperty (String key, Object object)
    {
        GluonInstance entry = getOrCreatePropertyEntry (key);
        serializer.applySerializer (entry, object);

        if (entry.containsKey (GluonConstants.OBJECT_CLASS))
        {
            // Remove entry from this instance
            removeEntry (entry);

            // Create the placeholder
            GluonObject object_ref = (GluonObject)entry.getProperty (GluonConstants.OBJECT_CLASS);

            // Transmogrify entry into an embedding
            entry.name = null;
            entry.setAttribute (GluonConstants.OBJECT_CLASS, "embedded", true);
            object_ref.generateId ();

            // Update representation
            serializer.applySerializer (entry.getPropertyEntry (GluonConstants.OBJECT_CLASS), object_ref);

            // Add entry on root instance
            entry.next = root.next;
            root.next = entry;

            // Add placeholder
            entry = getOrCreatePropertyEntry (key);
            serializer.applySerializer (entry, object_ref);
        }
        return (entry);
    }

    @Override
    public SerializerInstance setObjectClass (Class clazz)
    {
        return (setProperty (GluonConstants.OBJECT_CLASS, new GluonObject (clazz)));
    }

    @Override
    public SerializerInstance setAttribute (String property, String attribute, Object object)
    {
        GluonInstance entry = getOrCreatePropertyEntry (property);
        return (entry.setProperty (attribute, object));
    }

    public void _setRepresentation (String representation)
    {
        serializer.applyDeserializer (this, representation);
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
        return ((property == null)? null: property._resolveObject ());
    }

    public Object _getProperty (String key)
    {
        GluonInstance property = getPropertyEntry (key);
        return ((property == null)? null: property._getValueObject ());
    }

    public Object[] getArrayProperty (String key)
    {
        GluonInstance property = getPropertyEntry (key);

        if (property == null)
        {
            return (null);
        }

        Object value = property._resolveObject ();

        if (value instanceof GluonInstance[])
        {
            List<Object> values = new ArrayList<> ();

            for (GluonInstance entry: (GluonInstance[])value)
            {
                values.add (entry._resolveObject ());
            }
            return (values.toArray (new Object[0]));
        }
        else
        {
            return (new Object [] { value });
        }
    }

    public Object getAttribute (String property, String attribute)
    {
        GluonInstance entry = getPropertyEntry (property);
        return ((entry == null)? null: entry.getProperty (attribute));
    }

    public String _getPropertyRepresentation (String key)
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

    public Object _resolveObject ()
    {
        // Resolve Object
        if (containsKey (GluonConstants.OBJECT_CLASS))
        {
            if (backing_object == null)
            {
                serializer.applyDeserializer (this, null);
                log.info ("-----> _resolveObject({}) => {}", name, backing_object);
            }
        }

        // Resolve Reference
        if (backing_object instanceof GluonObject)
        {
            GluonObject object_ref = (GluonObject)backing_object;

            if (object_ref.getId () != 0) // Is it an object instance?
            {
                return (root.getProperty (GluonConstants.HIDDEN + object_ref.getValue ()));
            }
        }
        return (backing_object);
    }

    @Override
    public SerializerInstance addObject (Object object)
    {
        GluonInstance entry = newChildInstance ();

        serializer.applySerializer (entry, object);

        if (this != root && entry.containsKey (GluonConstants.OBJECT_CLASS))
        {
            // Remove entry from this instance
            removeEntry (entry);

            // Create the placeholder
            GluonObject object_ref = (GluonObject)entry.getProperty (GluonConstants.OBJECT_CLASS);

            // Transmogrify entry into an embedding
            entry.setAttribute (GluonConstants.OBJECT_CLASS, "embedded", true);
            object_ref.generateId ();

            // Update representation
            serializer.applySerializer (entry.getPropertyEntry (GluonConstants.OBJECT_CLASS), object_ref);

            // Add entry on root instance
            entry.next = root.children;
            root.children = entry;

            // Add placeholder
            entry = newChildInstance ();
            serializer.applySerializer (entry, object_ref);
        }

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
                objects.add (0, entry._resolveObject ());
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
