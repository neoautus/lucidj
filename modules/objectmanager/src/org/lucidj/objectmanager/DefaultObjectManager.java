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

package org.lucidj.objectmanager;

import org.lucidj.api.ObjectManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// ECAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA
public class DefaultObjectManager implements ObjectManager
{
    private final transient static Logger log = LoggerFactory.getLogger (DefaultObjectManager.class);

    private List<ObjectManager.ObjectEventListener> objectevent_listeners = new ArrayList<> ();

    private List<Object> object_list = Collections.synchronizedList (new ArrayList<> ()); // Ugly :P
    private Map<String, Object> tagged_objects = new HashMap<> ();
    private Set<Object> dirty_objects = new HashSet<> ();

    private String get_object_hash (Object obj)
    {
        return (obj.getClass().getName() + "#" + Integer.toHexString (obj.hashCode()));
    }

    public void showObject (Object obj)
    {
        log.info ("### ObjectManager {} showObject ({})", this, get_object_hash (obj));

        // TODO: SUPPORT SHOW NULL OBJECTS

        // Add only NEW objects
        if (object_list.indexOf (obj) == -1)
        {
            showObject (object_list.size (), obj);
        }
    }

    public void showObject (int index, Object obj)
    {
        for (ObjectManager.ObjectEventListener listener: objectevent_listeners)
        {
            // TODO: MAKE ALL THESE Throables BE VISIBLE WHEN SHOWING OBJECTS
            try
            {
                listener.addingObject (obj, index);
            }
            catch (Throwable ignore) {};
        }

        object_list.add (index, obj);
    }

    public void setObjectTag (Object obj, String tag)
    {
        tagged_objects.put (tag, obj);
    }

    public void removeObject (Object obj)
    {
        for (ObjectManager.ObjectEventListener listener: objectevent_listeners)
        {
            try
            {
                listener.removingObject (obj, object_list.indexOf (obj));
            }
            catch (Throwable ignore) {};
        }
        object_list.remove (obj);
    }

    public void clearObjects ()
    {
        for (int i = 0; i < object_list.size (); i++)
        {
            if (object_list.get (i) != null)
            {
                for (ObjectManager.ObjectEventListener listener: objectevent_listeners)
                {
                    try
                    {
                        listener.removingObject (object_list.get (i), i);
                    }
                    catch (Throwable ignore) {};
                }
            }
        }

        object_list.clear ();
        tagged_objects.clear ();
    }

    public void restrain ()
    {
        for (ObjectManager.ObjectEventListener listener: objectevent_listeners)
        {
            try
            {
                listener.restrain ();
            }
            catch (Throwable ignore) {};
        }
    }

    public void release ()
    {
        for (ObjectManager.ObjectEventListener listener: objectevent_listeners)
        {
            try
            {
                listener.release ();
            }
            catch (Throwable ignore) {};
        }
    }

    public int available ()
    {
        return (object_list.size ());
    }

    public Object read ()
        throws InterruptedException
    {
        for (;;)
        {
            try
            {
                Object obj = object_list.remove (0);
                removeObject (obj);

                // Do NOT pipe throwables
                if (!(obj instanceof Throwable))
                {
                    return (obj);
                }
            }
            catch (IndexOutOfBoundsException ignore) {};

            Thread.sleep (10);
        }
    }

    public Object readAll ()
            throws InterruptedException
    {
        for (;;)
        {
            try
            {
                Object obj = object_list.remove (0);
                removeObject (obj);

                // Pipe everything, including throwables
                return (obj);
            }
            catch (IndexOutOfBoundsException ignore) {};

            Thread.sleep (10);
        }
    }

    public Object getObject (int index)
    {
        return (object_list.get (index));
    }

    public Object getObject (String tag)
    {
        return (tagged_objects.get (tag));
    }

    public Object[] getObjects ()
    {
        return (object_list.toArray ());
    }

    public void markAsDirty (Object obj)
    {
        // TODO: CHANGE THIS TO A PUSH-BASED MECHANISM
        // BY NOW, DON'T ADD OBJECTS TO AVOID LEAKS
        //dirty_objects.add (obj);

        log.info ("markAsDirty: om={} obj={}", this, obj);

        for (ObjectManager.ObjectEventListener listener: objectevent_listeners)
        {
            log.info ("markAsDirty: changingObject {}", obj);
            try
            {
                listener.changingObject (obj);
            }
            catch (Throwable ignore) {};
        }
    }

    public void markAsClean (Object obj)
    {
        // TODO: CHANGE THIS TO A PUSH-BASED MECHANISM
        //dirty_objects.remove (obj);
    }

    public Set<Object> getDirtyObjects ()
    {
        return (dirty_objects);
    }

    @Override
    public void setObjectEventListener (ObjectManager.ObjectEventListener listener)
    {
        objectevent_listeners.add (listener);

        log.info ("setObjectEventListener: {} object_list.size={}", listener, object_list.size ());

        // Add all existing objects
        for (int i = 0; i < object_list.size (); i++)
        {
            log.info ("setObjectEventListener: adding {} {}", i, object_list.get (i));
            listener.addingObject (object_list.get (i), i);
        }
    }
}

// EOF
