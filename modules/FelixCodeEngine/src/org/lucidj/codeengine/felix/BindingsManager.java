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

package org.lucidj.codeengine.felix;

import org.lucidj.api.ServiceBinding;
import org.lucidj.api.ServiceBindingsManager;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

@Component (immediate = true, publicFactory = false)
@Instantiate
@Provides
public class BindingsManager implements ServiceBindingsManager
{
    private Map<String, ServiceBinding> bindings = new ConcurrentHashMap<> ();

    @Override
    public void register (ServiceBinding binding)
    {
        bindings.put (binding.getBindingName (), binding);
    }

    @Override
    public ServiceBinding getService (String name)
    {
        return (bindings.get (name));
    }

    @Override
    public boolean serviceExists (String name)
    {
        return (bindings.containsKey (name));
    }
}

// EOF
