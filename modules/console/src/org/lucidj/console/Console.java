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
import org.fusesource.jansi.HtmlAnsiOutputStream;
import org.lucidj.api.Quark;
import org.lucidj.api.Renderer;
import org.lucidj.renderer.SimpleObservable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Observer;

public class Console implements Quark, Renderer.Observable
{
    private final transient Logger log = LoggerFactory.getLogger (Console.class);

    private SimpleDateFormat timestamp_format = new SimpleDateFormat ("yyyy-MM-dd HH:mm:ss.SSS");

    private HashMap<String, Object> properties = new HashMap<>();
    private SimpleObservable observers = new SimpleObservable ();
    private StringBuilder contents = new StringBuilder ();

    private boolean append_line;
    private String last_tag;
    private String last_unfinished_tag;
    private int last_unfinished_tag_pos;

    public void output (String tag, String text)
    {
        if (text.isEmpty ())
        {
            return;
        }

        log.debug ("output: {}:{}", tag, text);

        synchronized (this)
        {
            if (contents == null)
            {
                contents = new StringBuilder (1024);
            }

            // Special case: stray newline
            if (text.equals ("\n") && tag.equals (last_unfinished_tag))
            {
                contents.insert (last_unfinished_tag_pos, "\\n");
                text = "";
            }

            if (!tag.equals (last_tag))
            {
                if (append_line)
                {
                    // Bad news... *.out.println actually writes the 'ln' on a separated
                    // call, then sometimes if we do out.println (bla);err.println(bla2) the
                    // out ln arrives after the err output: [OUTTXT][ERRTXT][OUTLN][ERRLN]
                    // So, keep track of the last unfinished line in case we need a simple fix.
                    last_unfinished_tag_pos = contents.length ();
                    last_unfinished_tag = last_tag;

                    // We don't have a newline following the last tag (OUT, ERR, etc),
                    // but we have different log entries since the tags changed
                    append_line = false;
                    contents.append ('\n');
                }

                last_tag = tag;
            }

            // Do we have an ordinary text to parse and output?
            while (!text.isEmpty ())
            {
                int nl = text.indexOf ('\n');

                // TODO: MAYBE WOULD BE USEFUL TO EMIT A NEW TIMESTAMP IF THE LAST LINE IS MORE THAN 1 SEC OLD

                // Avoid inserting timestamp?
                if (!append_line)
                {
                    contents.append (timestamp_format.format (new Date ()));
                    contents.append (" | ");
                    contents.append (tag);
                    contents.append (" | ");
                }

                // Just one line without \n
                if (nl == -1)
                {
                    contents.append (text);
                    append_line = true;

                    if (tag.equals (last_unfinished_tag))
                    {
                        last_unfinished_tag = null;
                    }
                    break;
                }

                String slice = text.substring (0, nl);
                log.debug ("APPEND(+nl):>{}<", slice);
                contents.append (slice);
                contents.append ("\\n"); // Literal newline (a real newline is inserted)
                contents.append ('\n');  // Logical newline (beautifier)
                append_line = false;

                text = text.substring (nl + 1);
            }
        }

        log.debug ("contents={}", contents.toString ());

        observers.notifyNow ();
    }

    public void clear ()
    {
        contents = null;
        append_line = false;
        last_unfinished_tag = null;
        observers.notifyNow ();
    }

    public String getContent ()
    {
        return (contents == null? "": contents.toString ());
    }

    public String getHtmlContent ()
    {
        BufferedReader full_content = new BufferedReader (new StringReader (getContent ()));
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

                    if (tag.equals ("ERR"))
                    {
                        content_out.append ("<font color='red'>");
                    }

                    if (tag.equals ("HTML"))
                    {
                        content_out.append (text);
                    }
                    else
                    {
                        content_out.append (safe_text);
                    }

                    if (tag.equals ("ERR"))
                    {
                        content_out.append ("</font>");
                    }
                }
            }
        }
        catch (Exception ignore) {};

        return (parsed_content.toString ());
    }

    @Override // Renderer.Observable
    public void addObserver (Observer observer)
    {
        observers.addObserver (observer);
    }

    @Override // Renderer.Observable
    public void deleteObserver (Observer observer)
    {
        observers.deleteObserver (observer);
    }

    @Override // Quark
    public Map<String, Object> serializeObject ()
    {
        // Complete content log including timestamps and tags
        properties.put ("/", contents.toString ());
        return (properties);
    }

    @Override // Quark
    public void deserializeObject (Map<String, Object> properties)
    {
        this.properties.putAll (properties);

        String stored_contents = (String)properties.get ("/");

        if (stored_contents != null)
        {
            contents = new StringBuilder (stored_contents);
        }
    }
}

// EOF
