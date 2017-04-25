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

import com.vaadin.data.Property;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Label;

public class LabelEx extends Label
{
    public LabelEx ()
    {
        super ();
    }

    public LabelEx (String content)
    {
        super (content);
    }

    public LabelEx (Property contentSource)
    {
        super (contentSource);
    }

    public LabelEx (String content, ContentMode contentMode)
    {
        super (content, contentMode);
    }

    public LabelEx (Property contentSource, ContentMode contentMode)
    {
        super (contentSource, contentMode);
    }

    @Override
    public void setValue (String newStringValue)
    {
        try
        {
            Vaadin.lock ();
            super.setValue (newStringValue);
        }
        finally
        {
            Vaadin.unlock ();
        }
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
    public void setContentMode (ContentMode contentMode)
    {
        try
        {
            Vaadin.lock ();
            super.setContentMode (contentMode);
        }
        finally
        {
            Vaadin.unlock ();
        }
    }
}

// EOF
