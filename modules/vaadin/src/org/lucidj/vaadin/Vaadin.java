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

import org.lucidj.api.ManagedObject;
import org.lucidj.api.ManagedObjectInstance;

import com.vaadin.data.Property;
import com.vaadin.server.VaadinSession;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Component;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import java.util.concurrent.locks.Lock;

public class Vaadin extends VerticalLayoutEx implements ManagedObject
{
    public Vaadin ()
    {
        setWidth (100, Unit.PERCENTAGE);
        setHeightUndefined ();
    }

    public static void lock ()
    {
        VaadinSession session = VaadinSession.getCurrent ();
        Lock lock = session.getLockInstance ();
        lock.lock(); // NOT session.unlock() to avois push changes
    }

    public static void unlock ()
    {
        VaadinSession session = VaadinSession.getCurrent ();
        Lock lock = session.getLockInstance ();
        lock.unlock();
    }

    public static Label newLabel ()
    {
        return (new LabelEx ());
    }

    public static Label newLabel (Property contentSource)
    {
        return (new LabelEx (contentSource));
    }

    public static Label newLabel (Property contentSource, ContentMode contentMode)
    {
        return (new LabelEx (contentSource, contentMode));
    }

    public static Label newLabel(String content)
    {
        return (new LabelEx (content));
    }

    public static Label newLabel (String content, ContentMode contentMode)
    {
        return (new LabelEx (content, contentMode));
    }

    public static VerticalLayout newVerticalLayout ()
    {
        return (new VerticalLayoutEx ());
    }

    public static VerticalLayout newVerticalLayout (Component... children)
    {
        return (new VerticalLayoutEx (children));
    }

    @Override
    public void validate (ManagedObjectInstance instance)
    {
        // Nop
    }

    @Override
    public void invalidate (ManagedObjectInstance instance)
    {
        // Nop
    }
}

// EOF
