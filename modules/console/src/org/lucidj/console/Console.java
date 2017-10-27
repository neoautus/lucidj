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

import org.lucidj.api.EventHelper;
import org.lucidj.api.vui.Renderer;  // <------ TODO: REMOVE THIS DEPENDENCY
import org.lucidj.api.Stdio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Console implements Stdio, Renderer.Observable
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
    @Override // Stdio
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

    @Override // Stdio
    public void stdout (String output)
    {
        output (STDOUT, output);
    }

    @Override // Stdio
    public void stderr (String output)
    {
        output (STDERR, output);
    }

    @Override // Stdio
    public void stdhtml (String output)
    {
        output (HTML, output);
    }

    @Override // DisplayManager.Clearable
    public void clear ()
    {
        contents = null;
        append_line = false;
        last_unfinished_tag = null;
        event_helper.publish (this);
    }

    @Override // Stdio
    public String getRawBuffer ()
    {
        return (contents == null? "": contents.toString ());
    }

    @Override // Stdio
    public void setRawBuffer (String value)
    {
        contents = new StringBuilder (value);
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
}

// EOF
