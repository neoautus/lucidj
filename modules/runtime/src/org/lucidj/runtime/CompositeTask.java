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

import org.lucidj.api.Task;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

public class CompositeTask implements Task, List
{
    private ArrayList<Object> obj_list = new ArrayList<> ();
    private HashMap<String, Object> properties = new HashMap<>();

    //==============
    // Main methods
    //==============

    //...

    //===============
    // Quark methods
    //===============

    @Override
    public void deserializeObject (Map<String, Object> properties)
    {
        this.properties.putAll (properties);
    }

    @Override
    public Map<String, Object> serializeObject ()
    {
        return(properties);
    }

    //==============
    // List methods
    //==============

    @Override
    public int size ()
    {
        return (obj_list.size ());
    }

    @Override
    public boolean isEmpty ()
    {
        return (obj_list.isEmpty ());
    }

    @Override
    public boolean contains (Object o)
    {
        return (obj_list.contains (o));
    }

    @Override
    public Iterator iterator ()
    {
        return (obj_list.iterator ());
    }

    @Override
    public Object[] toArray ()
    {
        return (obj_list.toArray ());
    }

    @Override
    public boolean add (Object o)
    {
        return (obj_list.add (o));
    }

    @Override
    public boolean remove (Object o)
    {
        return (obj_list.remove (o));
    }

    @Override
    public boolean addAll (Collection c)
    {
        return (obj_list.addAll (c));
    }

    @Override
    public boolean addAll (int index, Collection c)
    {
        return (obj_list.addAll (index, c));
    }

    @Override
    public void clear ()
    {
        obj_list.clear ();
    }

    @Override
    public Object get (int index)
    {
        return (obj_list.get (index));
    }

    @Override
    public Object set (int index, Object element)
    {
        return (obj_list.set (index, element));
    }

    @Override
    public void add (int index, Object element)
    {
        obj_list.add (index, element);
    }

    @Override
    public Object remove (int index)
    {
        return (obj_list.remove (index));
    }

    @Override
    public int indexOf (Object o)
    {
        return (obj_list.indexOf (o));
    }

    @Override
    public int lastIndexOf (Object o)
    {
        return (obj_list.lastIndexOf (o));
    }

    @Override
    public ListIterator listIterator ()
    {
        return (obj_list.listIterator ());
    }

    @Override
    public ListIterator listIterator (int index)
    {
        return (obj_list.listIterator (index));
    }

    @Override
    public List subList (int fromIndex, int toIndex)
    {
        return (obj_list.subList (fromIndex, toIndex));
    }

    @Override
    public boolean retainAll (Collection c)
    {
        return (obj_list.retainAll (c));
    }

    @Override
    public boolean removeAll (Collection c)
    {
        return (obj_list.removeAll (c));
    }

    @Override
    public boolean containsAll (Collection c)
    {
        return (obj_list.containsAll (c));
    }

    @Override
    public Object[] toArray (Object[] a)
    {
        return (obj_list.toArray (a));
    }
}

// EOF
