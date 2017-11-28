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

package org.lucidj.palette;

import org.lucidj.api.core.ComponentDescriptor;
import org.lucidj.api.core.ComponentInterface;
import org.lucidj.api.core.ComponentSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

// ManagedObject?
public class PaletteSet implements ComponentSet, ComponentInterface.ComponentListener
{
    private final static Logger log = LoggerFactory.getLogger (PaletteSet.class);

    private Set<ComponentDescriptor> components = new HashSet<> ();
    private Set<ComponentInterface.ComponentListener> listeners = new HashSet<> ();

    private void notify_adding_component (ComponentDescriptor component)
    {
        log.info ("notify_adding_component (component={})", component);
        for (ComponentInterface.ComponentListener listener: listeners)
        {
            log.info ("{}.addingComponent (component={})", listener, component);
            listener.addingComponent (component);
        }
    }

    private void notify_removing_component (ComponentDescriptor component)
    {
        log.info ("notify_removing_component (component={})", component);
        for (ComponentInterface.ComponentListener listener: listeners)
        {
            log.info ("{}.removingComponent (component={})", listener, component);
            listener.removingComponent (component);
        }
    }

    @Override
    public Set<ComponentDescriptor> getComponentSet ()
    {
        log.info ("getComponentSet() = {}", components);
        return (components);
    }

    @Override
    public void addListener (ComponentInterface.ComponentListener listener)
    {
        log.info ("addListener (listener={})", listener);
        listeners.add (listener);

        // Add all existing components
        for (ComponentDescriptor component: components)
        {
            listener.addingComponent (component);
        }
    }

    @Override
    public void removeListener (ComponentInterface.ComponentListener listener)
    {
        log.info ("removeListener (listener={})", listener);
        listeners.remove (listener);
    }

    @Override // ComponentInterface.ComponentListener
    public void addingComponent (ComponentDescriptor component)
    {
        log.info ("addingComponent (component={})", component);
        components.add (component);
        notify_adding_component (component);
    }

    @Override // ComponentInterface.ComponentListener
    public void removingComponent (ComponentDescriptor component)
    {
        log.info ("removingComponent (component={})", component);
        notify_removing_component (component);
        components.remove (component);
    }
}

// EOF
