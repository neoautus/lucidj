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

package org.lucidj.api;

public interface Aggregate
{
    // Identity is the front of the object.
    // If the object is a Car, identity() is Car
    default Object identity ()
    {
        return (this);
    }

    static Object identity (Object object)
    {
        if (object instanceof Aggregate)
        {
            return (((Aggregate)object).identity ());
        }
        return (object);
    }

    // Elements are all the parts that comprise the Car.
    // Wheels, motor, windshield etc
    // elements[0] is the identity
    // TODO: ADD WEAK ELEMENTS
    default Object[] elements ()
    {
        return (new Object[] { this });
    }

    static Object[] elements (Object object)
    {
        if (object instanceof Aggregate)
        {
            return (((Aggregate)object).elements ());
        }
        return (new Object[] { object });
    }

    // Fetch and adapt an specific part for use
    default <T> T adapt (Class<T> type)
    {
        if (type == null)
        {
            return (null);
        }
        else if (type.isAssignableFrom (this.getClass ()))
        {
            return (type.cast (this));
        }
        else
        {
            Object object;

            for (Object element: elements ())
            {
                if (element != this && (object = Aggregate.adapt (type, element)) != null)
                {
                    return (type.cast (object));
                }
            }
        }
        return (null);
    }

    static <T> T adapt (Class<T> type, Object object)
    {
        if (object == null || type == null)
        {
            return (null);
        }
        else if (object instanceof Aggregate)
        {
            return (((Aggregate)object).adapt (type));
        }
        else if (type.isAssignableFrom (object.getClass ()))
        {
            return (type.cast (object));
        }
        return (null);
    }
}

// EOF
