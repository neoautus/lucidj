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

import java.util.HashMap;
import java.util.Map;

import org.osgi.framework.Bundle;

public class DefaultManagedObjectInstance implements ManagedObjectInstance
{
    private ManagedObject managed_object;
    private Bundle provider_bundle;
    private Map<String, Object> properties;

    public DefaultManagedObjectInstance (ManagedObject object, Bundle bundle, Map<String, Object> properties)
    {
        this.properties = (properties != null)? properties: new HashMap<String, Object> ();
        this.provider_bundle = bundle;
        this.managed_object = object;
    }

    @Override
    public <A> A adapt (Class<A> type)
    {
        if (managed_object != null && type.isAssignableFrom (managed_object.getClass ()))
        {
            return (type.cast (managed_object));
        }

        return (null);
    }

    @Override
    public Bundle getBundle ()
    {
        return (provider_bundle);
    }

    @Override // ManagedObject
    public String[] getPropertyKeys ()
    {
        return (properties.keySet ().toArray (new String [0]));
    }

    @Override // ManagedObject
    public boolean containsKey (String key)
    {
        return (properties.containsKey (key));
    }

    @Override // ManagedObject
    public Object getProperty (String key)
    {
        return (properties.get (key));
    }

    @Override // ManagedObject
    public Class<?> getPropertyType (String key)
    {
        return (properties.containsKey (key)? properties.get (key).getClass (): null);
    }

    @Override // ManagedObject
    public void setProperty (String key, Object value)
    {
        properties.put (key, value);
    }

    @Override // ManagedObject
    public <T> T getObject (Class<T> type)
    {
        return (type.cast (properties.get (type.getCanonicalName ())));
    }

    @Override // ManagedObject
    public <T> void putObject (Class<T> type, T obj)
    {
        properties.put (type.getCanonicalName (), obj);
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
            provider_bundle = null;
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
