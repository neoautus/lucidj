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

package org.lucidj.objectmanager;

import org.lucidj.api.ManagedObject;
import org.lucidj.api.ManagedObjectFactory;
import org.lucidj.api.ManagedObjectInstance;
import org.lucidj.api.ManagedObjectProvider;
import org.lucidj.api.ObjectManager;

import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;

@org.apache.felix.ipojo.annotations.Component (immediate = true)
@Instantiate
@Provides
public class DefaultObjectManagerProvider implements ManagedObjectProvider
{
    @Requires
    private ManagedObjectFactory objectFactory;

    @Validate
    private void validate ()
    {
        objectFactory.register (ObjectManager.class, this, null);
    }

    @Override
    public ManagedObject newObject (String clazz, ManagedObjectInstance instance)
    {
        return (new DefaultObjectManager ());
    }
}

// EOF
