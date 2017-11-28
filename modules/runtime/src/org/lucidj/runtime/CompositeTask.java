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

package org.lucidj.runtime;

import org.lucidj.api.core.Aggregate;
import org.lucidj.api.core.ManagedObject;
import org.lucidj.api.core.ManagedObjectInstance;

import java.util.ArrayList;
import java.util.HashMap;

public class CompositeTask implements ManagedObject, Aggregate
{
    private ArrayList<Object> obj_list = new ArrayList<> ();
    private HashMap<String, Object> properties = new HashMap<>();

    @Override // Aggregate
    public Object[] elements ()
    {
        return (new Object[] { this, obj_list });
    }

    @Override
    public void validate (ManagedObjectInstance instance)
    {
        // Not used
    }

    @Override
    public void invalidate (ManagedObjectInstance instance)
    {
        // Not used
    }
}

// EOF
