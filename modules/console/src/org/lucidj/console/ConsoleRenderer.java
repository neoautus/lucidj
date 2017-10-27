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

package org.lucidj.console;

import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import org.lucidj.api.vui.Renderer;
import org.lucidj.api.Stdio;

import com.vaadin.server.Sizeable;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.AbstractComponent;
import com.vaadin.ui.Label;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.StringReader;

public class ConsoleRenderer implements Renderer
{
    private Label console_out_err = new Label ();
    private Stdio console;

    public ConsoleRenderer ()
    {
        console_out_err.setWidth (100, Sizeable.Unit.PERCENTAGE);
        console_out_err.setContentMode (ContentMode.HTML);
        console_out_err.setHeightUndefined ();
    }

    public static boolean isCompatible (Object object)
    {
        return (object instanceof Stdio);
    }

    @Override
    public void objectLinked (Object obj)
    {
        console = (Stdio)obj;
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

    public String format_as_html (String contents)
    {
        BufferedReader full_content = new BufferedReader (new StringReader (contents));
        ByteArrayOutputStream parsed_content = new ByteArrayOutputStream ();
        // TODO: REQUEST FEATURE CHANGE ON jansi FOR OPTIONAL HTML FILTERING
        AnsiHtmlOutputStream html_content = new AnsiHtmlOutputStream (parsed_content);
        PrintStream content_out = new PrintStream (html_content);
        String line;

        try
        {
            while ((line = full_content.readLine()) != null )
            {
                int tag_pos = line.indexOf ('|');
                int text_pos = line.indexOf ('|', tag_pos + 1);

                if (tag_pos == -1 || text_pos == -1)
                {
                    // Alien format is grey :)
                    content_out.append ("<font color='grey'>");
                    content_out.append (SafeHtmlUtils.htmlEscape (line));
                    content_out.append ("<br/></font>");
                }
                else
                {
                    // Native format, strip timestamp and color stderr accordingly
                    String tag = line.substring (tag_pos + 1, text_pos).trim ();
                    int skip_format_space = line.charAt (text_pos + 1) == ' '? 1 : 0;
                    String text = line.substring (text_pos + 1 + skip_format_space);
                    String safe_text = SafeHtmlUtils.htmlEscape (text).replace ("\\n", "<br/>");

                    if (tag.equals (Stdio.STDERR))
                    {
                        content_out.append ("<font color='red'>");
                    }

                    if (tag.equals (Stdio.HTML))
                    {
                        content_out.append (text);
                    }
                    else
                    {
                        content_out.append (safe_text);
                    }

                    if (tag.equals (Stdio.STDERR))
                    {
                        content_out.append ("</font>");
                    }
                }
            }
        }
        catch (Exception ignore) {};

        return (parsed_content.toString ());
    }

    @Override
    public void objectUpdated ()
    {
        String html =
            "<div style=\"white-space: pre-wrap; font: 14px/normal 'Monaco', 'Menlo', 'Ubuntu Mono', 'Consolas', 'source-code-pro', monospace\">" +
                format_as_html (console.getRawBuffer ()) +
            "</div>";
        console_out_err.setValue (html);
    }
}

// EOF
