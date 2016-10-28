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

package org.rationalq.exception;

import com.google.common.base.Throwables;
import org.lucidj.api.Renderer;

import com.vaadin.server.Sizeable;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.AbstractComponent;
import com.vaadin.ui.Label;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

@Component (immediate = true)
@Instantiate
@Provides
public class ExceptionRenderer implements Renderer
{
    private Label exception_description = new Label ();
    private Exception exception;

    public ExceptionRenderer ()
    {
        exception_description.setWidth (100, Sizeable.Unit.PERCENTAGE);
        exception_description.setContentMode (ContentMode.HTML);
        exception_description.setHeightUndefined ();
    }

    @Override
    public boolean compatibleObject (Object obj_to_check)
    {
        return (obj_to_check instanceof Throwable);
    }

    @Override
    public void objectLinked (Object obj)
    {
        exception = (Exception)obj;
    }

    @Override
    public void objectUnlinked ()
    {
        exception = null;
    }

    @Override
    public AbstractComponent renderingComponent ()
    {
        return (exception_description);
    }

    @Override
    public void objectUpdated ()
    {
        Throwable current = exception;
        String message = "";

        // Cycle exception and it's causes
        do
        {
            // Dump current exception
            message += current.getMessage() + "<br />";
        }
        while ((current = current.getCause ()) != null);

        exception_description.setValue (message);
    }
}

// EOF
