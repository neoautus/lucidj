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

package org.lucidj.displaymanager;

import org.lucidj.api.DisplayManager;
import org.lucidj.api.ServiceContext;
import org.lucidj.api.ServiceObject;

import java.util.Map;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;

@Component (immediate = true, publicFactory = false)
@Instantiate
@Provides
public class DefaultDisplayManagerProvider implements ServiceObject.Provider
{
    @Requires
    private ServiceContext serviceContext;

    @Validate
    private void validate ()
    {
        serviceContext.register (DisplayManager.class, this);
    }

    @Override
    public Object newObject (String objectClassName, Map<String, Object> properties)
    {
        return (new DefaultDisplayManager ());
    }
}

// EOF
