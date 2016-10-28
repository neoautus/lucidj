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

package org.lucidj.console;

import com.google.gwt.safehtml.shared.SafeHtmlUtils;
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
public class ConsoleRenderer implements Renderer
{
    private Label console_out_err = new Label ();
    private Console console;

    public ConsoleRenderer ()
    {
        console_out_err.setWidth (100, Sizeable.Unit.PERCENTAGE);
        console_out_err.setContentMode (ContentMode.HTML);
        console_out_err.setHeightUndefined ();
    }

    @Override
    public boolean compatibleObject (Object obj_to_check)
    {
        // TODO: BE COMPATIBLE WITH DIFFERENT OBJECT VERSIONS
        return (obj_to_check instanceof org.lucidj.console.Console);
    }

    @Override
    public void objectLinked (Object obj)
    {
        console = (Console)obj;
    }

    @Override
    public void objectUnlinked ()
    {
        console = null;
    }

    @Override
    public AbstractComponent renderingComponent ()
    {
        return (console_out_err);
    }

    @Override
    public void objectUpdated ()
    {
        String html =
            "<div style=\"white-space: pre-wrap; font: 14px/normal 'Monaco', 'Menlo', 'Ubuntu Mono', 'Consolas', 'source-code-pro', monospace\">" +
                console.getHtmlContent () +
            "</div>";
        console_out_err.setValue (html);
    }
}

// EOF
