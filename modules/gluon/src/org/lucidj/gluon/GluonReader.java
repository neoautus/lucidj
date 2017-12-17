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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

public class GluonReader
{
    private final static Logger log = LoggerFactory.getLogger (GluonReader.class);

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

    private boolean read_attributes (GluonInstance entry, String str)
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
                || (attribute.contains (".") && !attribute.contains ("=")) // :P
                || attribute.equals ("true")
                || attribute.equals ("false")
                || attribute.equals ("null"))
            {
                log.debug ("# SET VALUE Attribute: {}", attribute);
                entry._setRepresentation (attribute);
            }
            else // attr=value | boolean
            {
                int pos = attribute.indexOf ("=");

                // No '=' leads to special boolean handling
                if (pos == -1)
                {
                    log.debug ("# SET SPECIAL Attribute: {}", attribute);
                    entry._setPropertyRepresentation (attribute, attribute);
                }
                else // We have '=', build param name and it's value
                {
                    String attr_name = attribute.substring (0, pos).trim ();
                    String attr_value = attribute.substring (pos + 1).trim ();

                    log.debug ("# SET Normal Attribute: {}={}", attr_name, attr_value);
                    entry._setPropertyRepresentation (attr_name, attr_value);
                }
            }
        }
        return (true);
    }

    private boolean read_property_and_attributes (GluonInstance instance, String line)
    {
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

        log.debug ("Reading name=[" + property_name + "] right_hand=[" + right_hand + "]");

        List<String> attribute_groups = parseDelimitedString (right_hand, ",", true);

        if (attribute_groups == null)
        {
            return (false);
        }

        GluonInstance property_entry = instance.getOrCreatePropertyEntry (property_name);

        if (attribute_groups.size () == 1)
        {
            // We have a simple property with optional attributes attached
            read_attributes (property_entry, attribute_groups.get (0));
        }
        else // We have a property with nested objects, like a list, array or set
        {
            List<GluonInstance> object_list = new ArrayList<> ();

            // Cycle all groups
            for (String attributes: attribute_groups)
            {
                // We add 1 nested object for each attribute group
                GluonInstance child = property_entry.newChildInstance ();
                read_attributes (child, attributes.trim ());
                object_list.add (child);
            }
            property_entry._setValueObject (object_list.toArray (new GluonInstance[0]));
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

        // Blank file is valid
        return (true);
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

    private void check_for_embedded_object (GluonInstance instance)
    {
        Boolean embedding = (Boolean)instance.getAttribute (GluonConstants.OBJECT_CLASS, GluonConstants.EMBEDDING_FLAG);

        if (embedding != null && embedding)
        {
            // The embedded objects become hidden properties
            GluonObject object_ref = (GluonObject)instance._getProperty (GluonConstants.OBJECT_CLASS);
            instance.setPropertyKey (GluonConstants.HIDDEN + object_ref.getValue ());
        }
    }

    public boolean readRepresentation (GluonInstance instance)
        throws IOException
    {
        String lf = System.getProperty ("line.separator");

        // Root properties
        if (!read_properties_section (instance))
        {
            return (false);
        }

        // File boundary
        if (!read_boundary ())
        {
            // Is it an invalid boundary?
            if (boundary != null)
            {
                // TODO: BETTER HANDLING OF INVALID BOUNDARIES
                return (false);
            }

            // TODO: USE SERVICE INSTEAD OF DECLARED OBJECT

            // Handle the condition of a blank file, without any properties
            instance._setPropertyRepresentation (GluonConstants.OBJECT_CLASS, "org.lucidj.runtime.CompositeTask");
            return (true);
        }

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

            check_for_embedded_object (reading_object);

            StringBuilder reading_content = null;
            String line = null;
            reading_content = new StringBuilder ();

            while ((line = rd.readLine ()) != null)
            {
                if (line.trim ().equals (boundary))
                {
                    break;
                }
                else if (line.trim ().equals (final_boundary))
                {
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

            // End of file?
            if (line == null)
            {
                break;
            }
        }

        // We have valid metadata loaded
        return (true);
    }
}

// EOF
