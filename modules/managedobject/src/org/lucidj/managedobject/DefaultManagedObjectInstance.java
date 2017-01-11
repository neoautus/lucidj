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

package org.lucidj.managedobject;

import org.lucidj.api.ManagedObject;
import org.lucidj.api.ManagedObjectInstance;

import java.util.Map;

public class DefaultManagedObjectInstance implements ManagedObjectInstance
{
    private ManagedObject managed_object;

    public DefaultManagedObjectInstance (ManagedObject object)
    {
        managed_object = object;
    }

    @Override
    public <A> A adapt (Class<A> type)
    {
        if (type.isAssignableFrom (managed_object.getClass ()))
        {
            return (type.cast (managed_object));
        }

        return (null);
    }

    @Override
    public String[] getPropertyKeys ()
    {
        return ((managed_object == null)? new String [0]: managed_object.getPropertyKeys ());
    }

    @Override
    public boolean containsKey (String key)
    {
        return (managed_object != null && managed_object.containsKey (key));
    }

    @Override
    public Object getProperty (String key)
    {
        return ((managed_object == null)? null: managed_object.getProperty (key));
    }

    @Override
    public Class<?> getPropertyType (String key)
    {
        return ((managed_object == null)? null: managed_object.getPropertyType (key));
    }

    @Override
    public void setProperty (String key, Object value)
    {
        if (managed_object != null)
        {
            managed_object.setProperty (key, value);
        }
    }

    @Override
    public <T> T getObject (Class<T> type)
    {
        return ((managed_object == null)? null: managed_object.getObject (type));
    }

    @Override
    public <T> void putObject (Class<T> type, T obj)
    {
        if (managed_object != null)
        {
            managed_object.putObject (type, obj);
        }
    }

    @Override
    public void validate (ManagedObjectInstance instance)
    {
        if (managed_object != null)
        {
            // Notify object
            managed_object.validate (instance /* this! */);
        }
    }

    @Override
    public void invalidate ()
    {
        if (managed_object != null)
        {
            // Notify object
            managed_object.invalidate ();

            // Unlink object from deactivating service
            managed_object = null;
        }
    }

    @Override
    public Map<String, Object> serializeObject ()
    {
        return ((managed_object == null)? null: managed_object.serializeObject ());
    }

    @Override
    public boolean deserializeObject (Map<String, Object> properties)
    {
        return (managed_object != null && managed_object.deserializeObject (properties));
    }
}

// EOF
