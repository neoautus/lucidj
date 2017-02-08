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

import com.google.common.io.BaseEncoding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.lucidj.gluon.GluonSerializer.GluonInstance;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GluonWriter
{
    private final static transient Logger log = LoggerFactory.getLogger (GluonWriter.class);

    private Writer writer;
    private int ref_counter = 1;
    private String file_format_boundary;

    private BaseEncoding base64 = BaseEncoding.base64 ().withSeparator ("\n", 76);

    List<Map<String, Object>> serialization_queue = new ArrayList<> ();

    public GluonWriter (Writer writer)
    {
        this.writer = writer;
    }

//    private void write_token_property (QuarkToken token, String key,
//                                       List<Map<String, Object>> serialization_queue)
//        throws IOException
//    {
//        log.info ("write_token_property() key=[{}] value=[{}]", key, token);
//
//        // PropertyKey: PropertyValue
//        writer.write (key);
//        writer.write (": ");
//        writer.write (token.getName ());
//
//        Map<String, Object> parameters = token.getProperties ();
//
//        // Look for property parameters <Property>/<Parameter>
//        for (Map.Entry<String, Object> param : parameters.entrySet ())
//        {
//            String param_name = param.getKey ();
//            Object param_value = param.getValue ();
//
//            log.info ("Write param name=[{}] value=[{}]", param_name, param_value);
//
//            writer.write ("; ");
//
//            // Boolean parameters have special shortcuts
//            if (param_value instanceof Boolean)
//            {
//                if (!(Boolean)param_value)
//                {
//                    writer.write ("!");
//                }
//                writer.write (param_name);
//            }
//            else
//            {
//                // Normal parameter
//                writer.write (param_name);
//                writer.write ("=");
//                write_representation (param_value, serialization_queue);
//            }
//        }
//
//        writer.write ("\n");
//    }

    private void write_primitive_property (GluonInstance instance, String key, String[] keyset)
        throws IOException
    {
        writer.write (key);
        writer.write (": ");

        GluonInstance entry = instance.getProperty (key);
        String[] attr_keyset = entry.getPropertyKeys ();
        String semicolon = "";

        if (entry.isPrimitive ())
        {
            writer.write (entry.getValue ());
            semicolon = "; ";
        }

        if (attr_keyset != null)
        {
            for (String attr_key : attr_keyset)
            {
                writer.write (semicolon);
                writer.write (attr_key);
                writer.write ("=");
                writer.write ((entry.getProperty (attr_key)).getValue ());
                semicolon = "; ";
            }
        }
        writer.write ("\n");
    }

//    private void write_property (Map<String, Object> props, String key,
//                                 List<Map<String, Object>> serialization_queue)
//            throws IOException
//    {
//        Object obj = props.get (key);
//        Map<String, Object> representation = build_representation (obj);
//
//        if (representation.containsKey (QuarkConstants.COMPLEX_REPRESENTATION))
//        {
//            QuarkToken embedded = new QuarkToken ("Embedded");
//            embedded.setProperty ("refid", ref_counter);
//            representation.put (QuarkConstants.SIMPLE_REPRESENTATION, embedded);
//
//            // Insert reference data into the object representation
//            representation.put (QuarkConstants.OBJECT_CLASS_EMBEDDED, true);
//            representation.put (QuarkConstants.OBJECT_CLASS_ID, ref_counter);
//
//            // Store the complex representation
//            serialization_queue.add (representation);
//            ref_counter++;
//
//            // Write token
//            write_token_property (embedded, key, serialization_queue);
//        }
//        else
//        {
//            write_primitive_property (props, key, serialization_queue);
//        }
//    }

    private void write_properties (GluonInstance instance)
        throws IOException
    {
        String[] keyset = instance.getPropertyKeys ();

        if (keyset == null)
        {
            writer.write ("# No properties for this object\n");
            return;
        }

        // First write all X- properties
        for (String key: keyset)
        {
            if (!key.startsWith ("X-"))
            {
                continue;
            }

            write_primitive_property (instance, key, keyset);
        }

        // Now write all other properties
        for (String key: keyset)
        {
            if (key.startsWith ("X-"))
            {
                continue;
            }

            write_primitive_property (instance, key, keyset);
        }
    }

    public boolean writeRepresentation (GluonInstance instance)
        throws IOException
    {
        log.info ("writeRepresentation: representation={}", instance);

        GluonInstance content_boundary = instance.getProperty (GluonConstants.CONTENT_BOUNDARY);

        if (content_boundary != null && content_boundary.isPrimitive ())
        {
            file_format_boundary = content_boundary.getValue ();
        }

        // Minimum sanity please
        if (file_format_boundary == null
            || file_format_boundary.isEmpty ()
            || file_format_boundary.length () < 4)
        {
            file_format_boundary = GluonConstants.DEFAULT_BOUNDARY;
            instance.setProperty (GluonConstants.CONTENT_BOUNDARY, file_format_boundary);
        }

        // Build root object properties
//        HashMap<String, Object> properties = new HashMap<> ();
//        properties.put (GluonConstants.VERSION, "1.0.0");
//        properties.put (GluonConstants.CONTENT_TYPE, "multipart/mixed");
//        properties.put (QuarkConstants.CONTENT_BOUNDARY, QuarkConstants.DEFAULT_BOUNDARY);
//        properties.putAll (representation);
//        properties.put (QuarkConstants.OBJECT_CLASS, obj.getClass ().getCanonicalName ());


        // Write the properties of root object
        write_properties (instance);

        /***************************/
        /* MAIN SERIALIZED OBJECTS */
        /***************************/

        if (instance.getObjects () != null)
        {
            for (GluonInstance object: instance.getObjects ())
            {
                log.info ("object = " + object);

                writer.write ("\n");
                writer.write (file_format_boundary);
                writer.write ("\n");

                write_properties (object);

                writer.write ("\n"); // Empty line
                writer.write (object.getValue ());
            }
        }
        else
        {
            // Serialize just this one
        }

        if (!serialization_queue.isEmpty ())
        {
            for (int i = 0; i < serialization_queue.size (); i++)
            {
                Map<String, Object> obj_representation = serialization_queue.get (i);

                writer.write ("\n");
                writer.write (file_format_boundary);
                writer.write ("\n");

//                write_properties (obj_representation, serialization_queue);

                writer.write ("\n"); // Empty line
                writer.write ((String)obj_representation.get (GluonConstants.COMPLEX_REPRESENTATION));
            }
        }

        /*******************/
        /* WRITE SIGNATURE */
        /*******************/

        /***********/
        /* THE END */
        /***********/

        writer.write ("\n");
        writer.write (file_format_boundary);
        writer.write ("--\n");

        return (true);
    }
}

// EOF
