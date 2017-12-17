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

package org.lucidj.serviceobject;

import org.lucidj.api.core.Aggregate;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;

public class ServiceReferenceEx implements Aggregate, ServiceReference, Map<String, Object>
{
    private Map<String, Object> properties = new HashMap<> ();
    private Object service;
    private ServiceReference reference;
    private Object[] elements;

    public ServiceReferenceEx (Object service, ServiceReference reference)
    {
        this.service = service;
        this.reference = reference;

        // Copy all base_reference properties into local map
        for (String key: reference.getPropertyKeys ())
        {
            properties.put (key, reference.getProperty (key));
        }

        elements = new Object[] { service, this };
    }

    @Override // Aggregate
    public Object identity ()
    {
        return (service);
    }

    @Override // Aggregate
    public Object[] elements ()
    {
        return (elements);
    }

    //-----------------------------------------------------------------------------------------------------------------
    // WRAPPED ServiceReference
    //-----------------------------------------------------------------------------------------------------------------

    @Override // ServiceReference
    public Object getProperty (String s)
    {
        return (properties.get (s));
    }

    @Override // ServiceReference
    public String[] getPropertyKeys ()
    {
        Set<String> keys = properties.keySet ();
        return (keys.toArray (new String [keys.size ()]));
    }

    @Override // ServiceReference
    public Bundle getBundle ()
    {
        return (reference.getBundle ());
    }

    @Override // ServiceReference
    public Bundle[] getUsingBundles ()
    {
        return (reference.getUsingBundles ());
    }

    @Override // ServiceReference
    public boolean isAssignableTo (Bundle bundle, String s)
    {
        return (reference.isAssignableTo (bundle, s));
    }

    @Override // ServiceReference
    public int compareTo (Object o)
    {
        return (reference.compareTo (o));
    }

    //-----------------------------------------------------------------------------------------------------------------
    // WRAPPED Map
    //-----------------------------------------------------------------------------------------------------------------

    @Override // Map
    public int size ()
    {
        return (properties.size ());
    }

    @Override // Map
    public boolean isEmpty ()
    {
        return (properties.isEmpty ());
    }

    @Override // Map
    public boolean containsKey (Object key)
    {
        return (properties.containsKey (key));
    }

    @Override // Map
    public boolean containsValue (Object value)
    {
        return (properties.containsValue (value));
    }

    @Override // Map
    public Object get (Object key)
    {
        return (properties.get (key));
    }

    @Override // Map
    public Object put (String key, Object value)
    {
        return (properties.put (key, value));
    }

    @Override // Map
    public Object remove (Object key)
    {
        return (properties.remove (key));
    }

    @Override // Map
    public void putAll (Map<? extends String, ?> m)
    {
        properties.putAll (m);
    }

    @Override // Map
    public void clear ()
    {
        properties.clear ();
    }

    @Override // Map
    public Set<String> keySet ()
    {
        return (properties.keySet ());
    }

    @Override // Map
    public Collection<Object> values ()
    {
        return (properties.values ());
    }

    @Override // Map
    public Set<Entry<String, Object>> entrySet ()
    {
        return (properties.entrySet ());
    }
}

// EOF
