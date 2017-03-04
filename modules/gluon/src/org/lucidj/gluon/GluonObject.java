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

import java.util.concurrent.atomic.AtomicInteger;

public class GluonObject
{
    private static final AtomicInteger NEXT_ID = new AtomicInteger (1);
    private String class_name;
    private int id;

    public GluonObject (Class clazz)
    {
        class_name = clazz.getName ();
    }

    public GluonObject ()
    {
        // Nothing set
    }

    public GluonObject generateId ()
    {
        if (id == 0)
        {
            id = NEXT_ID.getAndIncrement();
        }
        return (this);
    }

    public int getId ()
    {
        return (id);
    }

    public void setId (int id)
    {
        this.id = id;
    }

    public String getClassName ()
    {
        return (class_name);
    }

    public String getValue ()
    {
        return (class_name + (id == 0? "": "@" + id));
    }

    public boolean setValue (String value)
    {
        if (value.contains ("@"))
        {
            // a.b.c@id
            int pos = value.indexOf ('@');
            class_name = value.substring (0, pos);
            id = Integer.valueOf (value.substring (pos + 1));
        }
        else
        {
            // a.b.c
            class_name = value;
        }
        return (true);
    }
}

// EOF
