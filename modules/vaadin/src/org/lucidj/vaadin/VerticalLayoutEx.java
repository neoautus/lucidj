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

package org.lucidj.vaadin;

import com.vaadin.ui.Component;
import com.vaadin.ui.ComponentContainer;
import com.vaadin.ui.VerticalLayout;

public class VerticalLayoutEx extends VerticalLayout
{
    public VerticalLayoutEx ()
    {
        super ();
    }

    public VerticalLayoutEx (Component... children)
    {
        super (children);
    }

    @Override
    public void attach ()
    {
        try
        {
            Vaadin.lock ();
            super.attach ();
        }
        finally
        {
            Vaadin.unlock ();
        }
    }

    @Override
    public void detach ()
    {
        try
        {
            Vaadin.lock ();
            super.detach ();
        }
        finally
        {
            Vaadin.unlock ();
        }
    }

    @Override
    public void addComponent (Component c)
    {
        try
        {
            Vaadin.lock ();
            super.addComponent (c);
        }
        finally
        {
            Vaadin.unlock ();
        }
    }

    @Override
    public void addComponentAsFirst (Component c)
    {
        try
        {
            Vaadin.lock ();
            super.addComponentAsFirst (c);
        }
        finally
        {
            Vaadin.unlock ();
        }
    }

    @Override
    public void addComponent (Component c, int index)
    {
        try
        {
            Vaadin.lock ();
            super.addComponent (c, index);
        }
        finally
        {
            Vaadin.unlock ();
        }
    }

    @Override
    public void removeComponent (Component c)
    {
        try
        {
            Vaadin.lock ();
            super.removeComponent (c);
        }
        finally
        {
            Vaadin.unlock ();
        }
    }

    @Override
    public void replaceComponent (Component oldComponent, Component newComponent)
    {
        try
        {
            Vaadin.lock ();
            super.replaceComponent (oldComponent, newComponent);
        }
        finally
        {
            Vaadin.unlock ();
        }
    }

    @Override
    public void addComponents (Component... components)
    {
        try
        {
            Vaadin.lock ();
            super.addComponents (components);
        }
        finally
        {
            Vaadin.unlock ();
        }
    }

    @Override
    public void removeAllComponents ()
    {
        try
        {
            Vaadin.lock ();
            super.removeAllComponents ();
        }
        finally
        {
            Vaadin.unlock ();
        }
    }

    @Override
    public void moveComponentsFrom (ComponentContainer source)
    {
        try
        {
            Vaadin.lock ();
            super.moveComponentsFrom (source);
        }
        finally
        {
            Vaadin.unlock ();
        }
    }
}

// EOF
