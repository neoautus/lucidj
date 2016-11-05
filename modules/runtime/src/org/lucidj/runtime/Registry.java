/*
 * Copyright 2016 NEOautus Ltd. (http://neoautus.com)
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.WeakHashMap;

public class Registry
{
    private final static transient Logger log = LoggerFactory.getLogger (Registry.class);

    private Set<Object> registry = Collections.newSetFromMap (new WeakHashMap<Object, Boolean> ());

    public void register (Object o)
    {
        log.debug ("Registry.register ({})", o);
        registry.add (o);
    }

    public void unregister (Object o)
    {
        log.debug ("Registry.unregister ({})", o);
        registry.remove (o);
    }

    public <A> Set<A> select (Class<A> type)
    {
        log.debug ("Registry.select ({})", type);

        HashSet<A> matching_objects = new HashSet<> ();

        for (Object o: registry)
        {
            log.debug ("Registry: object {}", o);

            if (o != null && type.isAssignableFrom (o.getClass ()))
            {
                log.debug ("Registry: object {} matches {}", o, type);

                matching_objects.add ((A)o);
            }
        }

        return (matching_objects);
    }
}

// EOF
