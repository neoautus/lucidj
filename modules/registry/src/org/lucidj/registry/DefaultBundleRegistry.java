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

package org.lucidj.registry;

import org.lucidj.api.BundleRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

@Component (immediate = true)
@Instantiate
@Provides (strategy = "SERVICE")
public class DefaultBundleRegistry implements BundleRegistry
{
    private final static transient Logger log = LoggerFactory.getLogger (DefaultBundleRegistry.class);

    private Map<String, Object> registry;

    public DefaultBundleRegistry ()
    {
        registry = new HashMap<> ();
    }

    @Override
    public String[] getPropertyKeys ()
    {
        return (registry.keySet ().toArray (new String [0]));
    }

    @Override
    public boolean containsKey (String key)
    {
        return (registry.containsKey (key));
    }

    @Override
    public Object getProperty (String key)
    {
        return (registry.get (key));
    }

    @Override
    public Class<?> getPropertyType (String key)
    {
        return (registry.get (key).getClass ());
    }

    @Override
    public void setProperty (String key, Object value)
    {
        registry.put (key, value);
    }
    @Override
    public <T> T getObject (Class<T> type)
    {
        Object obj = registry.get (type.getName ());

        if (obj != null && obj.getClass ().isAssignableFrom (type))
        {
            return ((T)obj);
        }
        return (null);
    }

    @Override
    public <T> void putObject (Class<T> type, T obj)
    {
        registry.put (type.getName (), obj);
    }
}

// EOF
