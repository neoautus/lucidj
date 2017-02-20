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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GluonInstance implements SerializerInstance
{
    private GluonSerializer parent;
    private Map<String, GluonInstance> properties = null;
    private Object backing_object = null;
    private String string_value = null;
    private List<GluonInstance> object_values = null;

    public GluonInstance (GluonSerializer parent)
    {
        this.parent = parent;
    }

    @Override
    public SerializerInstance setObjectClass (Class clazz)
    {
        return (setProperty (GluonConstants.OBJECT_CLASS, clazz.getName ()));
    }

    public String getObjectClass ()
    {
        if (properties != null && properties.containsKey (GluonConstants.OBJECT_CLASS))
        {
            return ((String)properties.get (GluonConstants.OBJECT_CLASS).getBackingObject ());
        }
        return (null);
    }

    public boolean isPrimitive ()
    {
        return (getProperty (GluonConstants.OBJECT_CLASS) == null);
    }

    public String[] getPropertyKeys ()
    {
        return ((properties == null)? new String [0]: properties.keySet ().toArray (new String [0]));
    }

    public boolean containsKey (String key)
    {
        return (properties.containsKey (key));
    }

    @Override
    public SerializerInstance setProperty (String key, Object object)
    {
        if (properties == null)
        {
            properties = new HashMap<> ();
        }

        GluonInstance instance = parent.buildRepresentationTree (object);

        if (instance != null)
        {
            properties.put (key, instance);
        }
        return (instance);
    }

    public void renameProperty (String old_name, String new_name)
    {
        GluonInstance property = properties.get (old_name);
        properties.remove (old_name);
        properties.put (new_name, property);
    }

    public GluonInstance getProperty (String key)
    {
        return ((properties == null)? null: properties.get (key));
    }

    public Object getPropertyObject (String key)
    {
        if (properties == null)
        {
            return (null);
        }

        GluonInstance property = properties.get (key);
        return ((property == null)? null: property.getBackingObject ());
    }

    @Override
    public void setValue (String representation)
    {
        string_value = representation;
    }

    public void setBackingObject (Object value)
    {
        backing_object = value;
    }

    @Override
    public SerializerInstance addObject (Object object)
    {
        GluonInstance instance = parent.buildRepresentationTree (object);

        if (instance != null)
        {
            if (object_values == null)
            {
                object_values = new ArrayList<> ();
            }

            object_values.add (instance);
        }
        return (instance);
    }

    @Override
    public Object[] getObjects ()
    {
        List<Object> objects = new ArrayList<> ();

        for (GluonInstance object_instance: getEmbeddedObjects ())
        {
            if (object_instance.getBackingObject () != null
                && object_instance.getProperty (GluonConstants.OBJECT_CLASS) != null
                && object_instance.getProperty (GluonConstants.OBJECT_CLASS).getProperty ("embedded") == null)
            {
                objects.add (object_instance.getBackingObject ());
            }
        }
        return (objects.toArray (new Object[0]));
    }

    public GluonInstance newInstance ()
    {
        return (new GluonInstance (parent));
    }

    public String getValue ()
    {
        return (string_value);
    }

    public Object getBackingObject ()
    {
        return (backing_object);
    }

    public List<GluonInstance> getEmbeddedObjects ()
    {
        return (object_values);
    }

    public boolean hasObjects ()
    {
        return (object_values != null);
    }
}

// EOF
