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
import org.lucidj.api.EventHelper;
import org.lucidj.api.ManagedObject;
import org.lucidj.api.ManagedObjectInstance;
import org.lucidj.api.Renderer;
import org.lucidj.api.Stdio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Console implements Stdio, ManagedObject, Renderer.Observable
{
    private final transient Logger log = LoggerFactory.getLogger (Console.class);

    private SimpleDateFormat timestamp_format = new SimpleDateFormat ("yyyy-MM-dd HH:mm:ss.SSS");

    private EventHelper event_helper;
    private StringBuilder contents = new StringBuilder ();

    private boolean append_line;
    private String last_tag;
    private String last_unfinished_tag;
    private int last_unfinished_tag_pos;

    public Console (EventHelper event_helper)
    {
        this.event_helper = event_helper;
    }

    // TODO: AUTODISCOVERY DATA STRUCTURE FROM TEXT (EX. CSV, TABLE, SERIES, ETC)
    @Override
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

        event_helper.publish (this);
    }

    @Override
    public void stdout (String output)
    {
        output (STDOUT, output);
    }

    @Override
    public void stderr (String output)
    {
        output (STDERR, output);
    }

    @Override
    public void stdhtml (String output)
    {
        output (HTML, output);
    }

    @Override
    public void clear ()
    {
        contents = null;
        append_line = false;
        last_unfinished_tag = null;
        event_helper.publish (this);
    }

    public String getValue ()
    {
        return (contents == null? "": contents.toString ());
    }

    public void setValue (String value)
    {
        contents = new StringBuilder (value);
    }

    public String getHtmlContent ()
    {
        BufferedReader full_content = new BufferedReader (new StringReader (getValue ()));
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

                    if (tag.equals (STDERR))
                    {
                        content_out.append ("<font color='red'>");
                    }

                    if (tag.equals (HTML))
                    {
                        content_out.append (text);
                    }
                    else
                    {
                        content_out.append (safe_text);
                    }

                    if (tag.equals (STDERR))
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
    public void addObserver (EventHelper.Subscriber observer)
    {
        event_helper.subscribe (observer);
    }

    @Override // Renderer.Observable
    public void deleteObserver (EventHelper.Subscriber observer)
    {
        event_helper.unsubscribe (observer);
    }

    @Override // ManagedObject
    public void validate (ManagedObjectInstance instance)
    {
        // Nop
    }

    @Override // ManagedObject
    public void invalidate (ManagedObjectInstance instance)
    {
        // Nop
    }
}

// EOF
