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

package org.lucidj.gluon;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.lucidj.gluon.GluonSerializer.GluonInstance;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

public class GluonReader
{
    private final transient static Logger log = LoggerFactory.getLogger (GluonReader.class);

    private Reader reader;
    private BufferedReader rd;

    private String boundary = null;
    private String final_boundary = null;

    public GluonReader (Reader reader)
    {
        this.reader = reader;
        rd = new BufferedReader (reader);
    }

    // Shamelessly copied from org.apache.felix.framework.util.manifestparser.ManifestParser.java
    public static List<String> parseDelimitedString(String value, String delim, boolean trim)
    {
        if (value == null)
        {
            value = "";
        }

        List<String> list = new ArrayList<> ();

        int CHAR = 1;
        int DELIMITER = 2;
        int STARTQUOTE = 4;
        int ENDQUOTE = 8;

        StringBuffer sb = new StringBuffer();

        int expecting = (CHAR | DELIMITER | STARTQUOTE);

        boolean isEscaped = false;
        for (int i = 0; i < value.length(); i++)
        {
            char c = value.charAt(i);

            boolean isDelimiter = (delim.indexOf(c) >= 0);

            if (!isEscaped && (c == '\\'))
            {
                isEscaped = true;
                continue;
            }

            if (isEscaped)
            {
                sb.append('\\');
                sb.append(c);
            }
            else if (isDelimiter && ((expecting & DELIMITER) > 0))
            {
                if (trim)
                {
                    list.add(sb.toString().trim());
                }
                else
                {
                    list.add(sb.toString());
                }
                sb.delete(0, sb.length());
                expecting = (CHAR | DELIMITER | STARTQUOTE);
            }
            else if ((c == '"') && ((expecting & STARTQUOTE) > 0))
            {
                sb.append(c);
                expecting = CHAR | ENDQUOTE;
            }
            else if ((c == '"') && ((expecting & ENDQUOTE) > 0))
            {
                sb.append(c);
                expecting = (CHAR | STARTQUOTE | DELIMITER);
            }
            else if ((expecting & CHAR) > 0)
            {
                sb.append(c);
            }
            else
            {
                log.error ("Invalid delimited string: {}", value);
                return (null);
            }

            isEscaped = false;
        }

        if (sb.length() > 0)
        {
            if (trim)
            {
                list.add(sb.toString().trim());
            }
            else
            {
                list.add(sb.toString());
            }
        }

        return (list);
    }

    private boolean read_attributes (GluonInstance property_instance, String str)
    {
        // We expect something like:
        //   [value;]attr1=value[;attr2=value2[;attrN=valueN]]
        List<String> attribute_list = parseDelimitedString (str, ";", true);

        if (attribute_list == null)
        {
            return (false);
        }

        for (String attribute: attribute_list)
        {
            attribute = attribute.trim ();

            // TODO: BETTER CHECKING STRUCTURE
            // Do we have a value or an attribute?
            if ("0123456789.\"".indexOf (attribute.charAt (0)) != -1
                || attribute.equals ("true")
                || attribute.equals ("false")
                || attribute.equals ("null"))
            {
                log.info ("# SET VALUE Attribute: {}", attribute);
                property_instance.setValue (attribute);
            }
            else // attr=value | boolean
            {
                int pos = attribute.indexOf ("=");

                // No '=' leads to special boolean handling
                if (pos == -1)
                {
                    log.info ("# SET SPECIAL Attribute: {}", attribute);
                    GluonInstance entry = (GluonInstance)property_instance.setProperty (attribute, null);
                    entry.setValue (attribute);

                    // TODO: FIND A BETTER WAY!!!
                    // Very ugly way to gain access to property name
                    entry.setBackingObject (property_instance);
                }
                else // We have '=', build param name and it's value
                {
                    String attr_name = attribute.substring (0, pos).trim ();
                    String attr_value = attribute.substring (pos + 1).trim ();

                    log.info ("# SET Normal Attribute: {}={}", attr_name, attr_value);
                    property_instance.setProperty (attr_name, null).setValue (attr_value);
                }
            }
        }
        return (true);
    }

    private boolean read_property_and_attributes (GluonInstance instance, String line)
    {
        log.info ("====> PROPERTY: {}", line);

        if (line.startsWith ("#"))
        {
            return (true);
        }

        int pos = line.indexOf (':');

        if (pos == -1)
        {
            return (false);
        }

        String property_name = line.substring(0, pos).trim();
        String right_hand = line.substring(pos + 1).trim();

        log.info ("Reading name=[" + property_name + "] right_hand=[" + right_hand + "]");

        List<String> attribute_groups = parseDelimitedString (right_hand, ",", true);

        if (attribute_groups == null)
        {
            return (false);
        }

        // Here we create the property entry
        GluonInstance property_instance = (GluonInstance)instance.setProperty (property_name, null);

        if (attribute_groups.size () == 1)
        {
            // We have a simple property with optional attributes attached
            read_attributes (property_instance, attribute_groups.get (0));
        }
        else // We have a property with nested objects, like a list, array or set
        {
            // Cycle all groups
            for (String attributes: attribute_groups)
            {
                // We add 1 nested object for each attribute group
                GluonInstance nested_object = (GluonInstance)property_instance.addObject (null);
                read_attributes (nested_object, attributes.trim ());
            }
        }
        return (true);
    }

    private boolean read_properties_section (GluonInstance instance)
        throws IOException
    {
        String line;

        // The properties section comprises properties and comments and ends with a blank line
        while ((line = rd.readLine()) != null)
        {
            line = line.trim ();

            if (line.isEmpty ())
            {
                // The empty line was consumed, the next readings will be the content itself
                return (true);
            }

            if (line.startsWith ("#"))
            {
                // Comments are ignored
                continue;
            }

            // The line must be a property
            if (!read_property_and_attributes (instance, line))
            {
                return (false);
            }
        }

        // Premature EOF
        return (false);
    }

    private boolean read_boundary ()
        throws IOException
    {
        // The first string after the blank line is the section boundary of this file
        if ((boundary = rd.readLine ()) == null)
        {
            return (false);
        }

        boundary = boundary.trim ();

        if (boundary.length () < 8)
        {
            return (false);
        }

        // We have a mostly valid boundary, set the final boundary also
        final_boundary = boundary + GluonConstants.EOF_MARKER;
        return (true);
    }

    public boolean readRepresentation (GluonInstance instance)
        throws IOException
    {
        String lf = System.getProperty("line.separator");

        // Root properties
        if (!read_properties_section (instance))
        {
            return (false);
        }

        // File boundary
        if (!read_boundary ())
        {
            return (false);
        }

        log.info ("BOUNDARY='{}'", boundary);
        log.info ("FINAL BOUNDARY='{}'", final_boundary);

        // Handle the condition of a blank file (properties only).
        // This condition appears when the properties are followed by a final boundary.
        if (boundary.endsWith (GluonConstants.EOF_MARKER))
        {
            return (true);
        }

        // We store the used boundary as a hidden property
        instance.setProperty (GluonConstants.CONTENT_BOUNDARY, boundary);

        // After root properties and the file boundary, we have all the objects
        for (;;)
        {
            // Start reading a new object
            GluonInstance reading_object = (GluonInstance)instance.addObject (null);

            // Read the properties of the object instance
            if (!read_properties_section (reading_object))
            {
                return (false);
            }

            StringBuilder reading_content = null;
            String line = null;
            reading_content = new StringBuilder ();

            while ((line = rd.readLine ()) != null)
            {
                if (line.trim ().equals (boundary))
                {
                    log.info ("--BOUNDARY--");
                    break;
                }
                else if (line.trim ().equals (final_boundary))
                {
                    log.info ("--FINAL BOUNDARY--");
                    line = null;
                    break;
                }

                // Append a newline if needed...
                if (reading_content.length () != 0)
                {
                    reading_content.append (lf);
                }

                // ... and append the line itself
                reading_content.append (line);
            }

            // The content was read
            reading_object.setValue (reading_content.toString ());

            log.info ("====> CONTENTS = [{}]", reading_content.toString ());

            // End of file?
            if (line == null)
            {
                break;
            }
        }

        log.info ("READ Finish!");

        // We have valid metadata loaded
        return (true);
    }
}

// EOF
