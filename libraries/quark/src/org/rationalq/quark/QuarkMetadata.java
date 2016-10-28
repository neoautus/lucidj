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

package org.rationalq.quark;

import com.google.common.base.CharMatcher;
import com.google.common.io.BaseEncoding;
import org.apache.commons.lang3.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class QuarkMetadata
{
    private final transient static Logger log = LoggerFactory.getLogger (QuarkMetadata.class);

    private BaseEncoding base64 = BaseEncoding.base64 ().withSeparator ("\n", 76);

    public List<Map <String, Object>> serialization_queue = new LinkedList<> ();
    public Map<Integer, Map <String, Object>> embedded_objects = new HashMap<>();
    private HashMap<String, Object> root_properties = new HashMap<> ();
    private boolean valid_metadata;

    private Object decode_from_string (String object_repr)
    {
        if (object_repr.equals("null"))
        {
            return(null);
        }
        else if (object_repr.equals("true"))
        {
            return(true);
        }
        else if (object_repr.equals("false"))
        {
            return(false);
        }
        else if (object_repr.startsWith("\""))
        {
            String escaped_str = object_repr.substring(1, object_repr.length() - 1);
            return (StringEscapeUtils.unescapeJava (escaped_str));
        }
        else if (object_repr.matches("[0-9]+"))
        {
            return (Integer.valueOf(object_repr));
        }
        else if (object_repr.matches("[0-9]+L"))
        {
            // Strip L and convert. "L" ALWAYS UPPERCASE to avoid 1/l mistakes
            return (Long.valueOf(object_repr.substring(0, object_repr.length() - 1)));
        }
        else if (object_repr.matches ("[A-Za-z_]+[A-Za-z_0-9]*"))
        {
            // TODO: HANDLE NUMBERS
            return (new QuarkToken (object_repr));
        }
        else
        {
            return ("__UNKNOWN__");
        }
    }

    private void read_properties (Map<String, Object> props, String property_string)
    {
        int pos = property_string.indexOf (':');

        if (pos == -1)
        {
            return;
        }

        Map<String, Object> parameters = new HashMap<> ();
        String name = property_string.substring(0, pos).trim();
        String value = property_string.substring(pos + 1).trim();
        Object obj_value;

        log.info ("Reading name=[" + name + "] value=[" + value + "]");

        pos = value.indexOf(';');

        if (pos != -1)
        {
            String params = value.substring(pos + 1);
            value = value.substring(0, pos);

            String[] param_list = params.split("\\;");

            for (String param: param_list)
            {
                String param_name, param_value;
                pos = param.indexOf ('=');

                // No '=' leads to special boolean handling
                if (pos == -1)
                {
                    boolean is_negated = param.substring(0, 1).equals("!");
                    param_name = (is_negated? param.substring(1): param).trim();
                    obj_value = !is_negated;
                }
                else // We have '=', build param name and it's value
                {
                    param_name = param.substring (0, pos).trim();
                    param_value = param.substring(pos + 1).trim();
                    obj_value = decode_from_string (param_value);
                }

                parameters.put(param_name, obj_value);
                log.info ("Param name=[" + param_name + "] value=[" + obj_value +
                        "] type=[" + (obj_value == null? "null" : obj_value.getClass().getCanonicalName()) + "]");
            }
        }

        // Obtains and stores the property value (Object or primitive)
        obj_value = decode_from_string (value);
        props.put (name, obj_value);

        if (obj_value instanceof QuarkToken)
        {
            // Set additional parameters for Object
            ((QuarkToken)obj_value).setProperties (parameters);
        }
        else
        {
            // Set additional parameters for primitive type
            for (Map.Entry<String, Object> param: parameters.entrySet())
            {
                props.put (name + "/" + param.getKey (), param.getValue ());
            }
        }

        log.info ("Property name=[" + name + "] value=[" + obj_value +
                "] type=[" + (obj_value == null? "null" : obj_value.getClass().getCanonicalName()) + "]");
    }

    private void handle_section(Map<String, Object> reading_properties, StringBuilder reading_content)
    {
        String q_object = (String)reading_properties.get (QuarkFields.OBJECT_CLASS);

        log.info("q_object = " + q_object);
        log.info("reading_content = " + reading_content);
        log.info("reading_properties = " + reading_properties);

        //-------------------------------
        // Handle content representation
        //-------------------------------

        if (reading_properties.containsKey (QuarkFields.CONTENT_ENCODING) &&
                reading_properties.get (QuarkFields.CONTENT_ENCODING).equals ("base64"))
        {
            // Store ByteBuffer
            String clean_base64 = CharMatcher.WHITESPACE.removeFrom (reading_content.toString ());

            try
            {
                byte[] bin_content = base64.decode (clean_base64);
                reading_properties.put("/", ByteBuffer.wrap(bin_content));
            }
            catch (Exception e)
            {
                // TODO: GENERATE EXCEPTION DATA TO INFORM PROBLEM
                reading_properties.put("/", null);
            }
        }
        else
        {
            // Store String
            reading_properties.put("/", reading_content.toString());
        }

        //------------------
        // Handle embedding
        //------------------

        if (reading_properties.containsKey (QuarkFields.OBJECT_CLASS_EMBEDDED) &&
            (Boolean)reading_properties.get (QuarkFields.OBJECT_CLASS_EMBEDDED))
        {
            if (reading_properties.containsKey (QuarkFields.OBJECT_CLASS_ID))
            {
                embedded_objects.put ((Integer)reading_properties.get (QuarkFields.OBJECT_CLASS_ID),
                                      reading_properties);
                log.info("Object is EMBEDDED");
            }
            else
            {
                log.info("EMBEDDED Object without ID");
            }
        }
        else if (q_object != null && !reading_properties.containsKey (QuarkFields.QUARK_VERSION))
        {
            // Store the full object representation
            serialization_queue.add (reading_properties);
            log.info("Object NOT embedded");
        }
    }

    public boolean readObjectMetadata (Reader reader)
        throws Exception
    {
        String lf = System.getProperty("line.separator");
        BufferedReader rd = new BufferedReader (reader);

        valid_metadata = false;

        // Start reading the given obj_prop (top level properties)
        Map<String, Object> reading_properties = root_properties;

        StringBuilder quark_header = new StringBuilder();
        StringBuilder reading_content = null;
        String boundary = null;
        String line = null;

        do
        {
            boundary = (String)root_properties.get (QuarkFields.CONTENT_BOUNDARY);
            line = rd.readLine();

            log.debug ("LINE: [" + line + "] boundary=[" + boundary + "]");

            if (line != null && line.trim().equals (boundary + "--"))
            {
                // Stop reading the file like an EOF
                log.debug ("--FINAL BOUNDARY--");
                line = null;
            }

            if (line == null || line.trim().equals (boundary))
            {
                log.debug ("--BOUNDARY--");

                // Do we have content ready to handle?
                if (reading_content != null)
                {
                    //---------------------------------
                    // We have a entire section ready!
                    //---------------------------------
                    handle_section(reading_properties, reading_content);
                }
                else
                {
                    // Record the complete header from this quark file
                    reading_properties.put("/header", (quark_header == null)? "": quark_header.toString());
                    quark_header = null;
                }

                // Next step: read properties for the next section
                reading_properties = new HashMap<> ();
                reading_content = null;
            }
            else if (reading_content != null)
            {
                // We are reading content until we reach boundary (previous if)
                if (reading_content.length() != 0)
                {
                    reading_content.append(lf);
                }
                reading_content.append(line);
            }
            else if (line.isEmpty())
            {
                // We are reading properties, and we reached the start of content
                // (marked by an empty line)
                log.debug ("New content");
                dump_properties(reading_properties);

                // Now we are reading content
                reading_content = new StringBuilder();
            }
            else // This must be a property
            {
                if (quark_header != null)
                {
                    quark_header.append(line);
                    quark_header.append("\n");
                }
                // All properties have ':'
                if (line.contains(":"))
                {
                    read_properties(reading_properties, line);
                }
            }
        }
        while (line != null);

        log.debug ("Finish!");

        // We have valid metadata loaded
        valid_metadata = true;
        return (true);
    }

    public HashMap<String, Object> getRootProperties ()
    {
        return (root_properties);
    }

    public boolean isValid ()
    {
        return (valid_metadata);
    }

    public class Token
    {
    }

    /**** EXTRAS ****/

    private void dump_properties (Map<String, Object> props)
    {
        for (Map.Entry<String, Object> property: props.entrySet())
        {
            log.info("Property name=[" + property.getKey() +
                    "] value=[" + property.getValue() + "]");
        }
    }
}

// EOF
