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
import org.apache.commons.lang3.StringEscapeUtils;
import org.lucidj.api.Quark;
import org.lucidj.api.QuarkSerializable;
import org.lucidj.api.SerializerInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.lucidj.gluon.GluonSerializer.GluonInstance;

import java.io.IOException;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
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

//    public Map<String, Object> build_representation (Object obj)
//    {
//        Map<String, Object> object_properties = null;
//        String simple_object = null;
//        Object complex_object = null;
//
//        log.info ("build_representation: obj={}", obj);
//
//        if (obj instanceof Quark)
//        {
//            log.info ("build_representation: Quark obj={}", obj);
//
//            // TODO: HANDLE LEAKING EXCEPTIONS THROWN FROM serializeObject()
//            object_properties = ((Quark)obj).serializeObject ();
//
//            log.info ("build_representation: Quark object_properties=[ {} ]", object_properties);
//
//            complex_object = object_properties.get ("/");
//
//            object_properties.put (GluonConstants.OBJECT_CLASS, obj.getClass ().getCanonicalName ());
//            object_properties.put (GluonConstants.CONTENT_TYPE, "text/plain");
//        }
//        else
//        {
//            object_properties = new HashMap<> ();
//            QuarkSerializable qs = null;
//
//            if (obj == null)
//            {
//                simple_object = "null";
//            }
//            else if ((qs = find_serializer (obj.getClass ())) != null)
//            {
//                log.info ("CustomSerializer: {}", qs);
//
//                // TODO: HANDLE LEAKING EXCEPTIONS THROWN FROM serializeObject()
//                object_properties = qs.serializeObject (obj);
//
//                log.info ("build_representation: Quark object_properties=[ {} ]", object_properties);
//
//                complex_object = object_properties.get ("/");
//
//                object_properties.put (GluonConstants.OBJECT_CLASS, obj.getClass ().getCanonicalName ());
//                object_properties.put (GluonConstants.CONTENT_TYPE, "text/plain");
//            }
//            else if (obj instanceof Object[])
//            {
//                // Deal with an array of objects
//                //...
//                simple_object = "(Holy crap, it's an Array!)";
//            }
//            else if (obj instanceof String)
//            {
//                simple_object = "\"" + StringEscapeUtils.escapeJava ((String)obj) + "\"";
//            }
//            else if (obj instanceof Boolean)
//            {
//                simple_object = (Boolean)obj? "true" : "false";
//            }
//            else if (obj instanceof Integer)
//            {
//                simple_object = Integer.toString ((Integer)obj);
//            }
//            else if (obj instanceof Long)
//            {
//                simple_object = Long.toString ((Long)obj) + "L";
//            }
//            else if (obj instanceof Float)
//            {
//                simple_object = Float.toString ((Float)obj) + "f";
//            }
//            else if (obj instanceof Double)
//            {
//                simple_object = Double.toString ((Double)obj) + "d";
//            }
//            else // Serialize an unknown object
//            {
//                // Opaque serialized object:
//                // object_properties: serialization info
//                object_properties = new HashMap<String, Object> ();
//                object_properties.put (GluonConstants.CONTENT_TYPE, "application/java-serialized-object");
//                object_properties.put (GluonConstants.OBJECT_CLASS, obj.getClass ().getCanonicalName ());
//
//                try
//                {
//                    // TODO: PLUGGABLE SERIALIZER
//                    complex_object = ByteBuffer.wrap (kryo.serialize (obj));
//                }
//                catch (Exception e)
//                {
//                    log.info ("build_representation: Error serializing", e);
//                }
//            }
//        }
//
//        log.info ("simple_object = [" + simple_object + "]");
//        log.info ("complex_object = [" + complex_object + "]");
//        log.info ("object_properties = [" + object_properties + "]");
//
//        if (complex_object != null)
//        {
//            if (complex_object instanceof String)
//            {
//                // Use the string as-is
//                object_properties.put (GluonConstants.COMPLEX_REPRESENTATION, (String)complex_object);
//            }
//            else if (complex_object instanceof ByteBuffer)
//            {
//                // Convert buffer to base64-encoded string
//                ByteBuffer object_body_bf = (ByteBuffer)complex_object;
//                object_properties.put (GluonConstants.COMPLEX_REPRESENTATION, base64.encode (object_body_bf.array ()));
//                object_properties.put (GluonConstants.CONTENT_ENCODING, "base64");
//            }
//        }
//        else if (simple_object != null)
//        {
//            object_properties.put (GluonConstants.SIMPLE_REPRESENTATION, simple_object);
//        }
//        else
//        {
//            object_properties.put (GluonConstants.SIMPLE_REPRESENTATION, "(Unknown)");
//        }
//
//        return (object_properties);
//    }

//    private void write_representation (Object obj, List<Map<String, Object>> serialization_queue)
//        throws IOException
//    {
//        Map<String, Object> representation = build_representation (obj);
//
//        if (representation.containsKey (GluonConstants.SIMPLE_REPRESENTATION))
//        {
//            // Simple representation is always string
//            writer.write ((String)representation.get (GluonConstants.SIMPLE_REPRESENTATION));
//        }
//        else if (representation.containsKey (GluonConstants.COMPLEX_REPRESENTATION))
//        {
//            // Insert reference data into the object representation
//            representation.put (GluonConstants.OBJECT_CLASS_EMBEDDED, true);
//            representation.put (GluonConstants.OBJECT_CLASS_ID, ref_counter);
//
//            // Write the reference to the embedded object
//            writer.write ("embedded; refid=");
//            writer.write (Integer.toString (ref_counter));
//
//            // Store the complex representation
//            serialization_queue.add (representation);
//            ref_counter++;
//        }
//        else
//        {
//            writer.write ("(Something is wrong...)");
//        }
//    }
//
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

//    private void write_primitive_property (Map<String, Object> props, String key,
//                                           List<Map<String, Object>> serialization_queue)
//        throws IOException
//    {
//        log.info ("write_property() key=[" + key + "] value=[" + props.get (key) + "]");
//
//        // PropertyKey: PropertyValue
//        writer.write (key);
//        writer.write (": ");
//        write_representation (props.get (key), serialization_queue);
//
//        String property_param = key + "/";
//
//        // Look for property parameters <Property>/<Parameter>
//        for (Map.Entry<String, Object> param : props.entrySet ())
//        {
//            if (param.getKey ().startsWith (property_param))
//            {
//                String param_key = param.getKey ();
//                String param_name = param_key.substring (property_param.length ());
//                Object param_value = props.get (param_key);
//
//                log.info ("Write param name=[" + param_name +
//                        "] value=[" + param_value + "]");
//
//                writer.write ("; ");
//
//                // Boolean parameters have special shortcuts
//                if (param_value instanceof Boolean)
//                {
//                    if (!(Boolean)param_value)
//                    {
//                        writer.write ("!");
//                    }
//                    writer.write (param_name);
//                }
//                else
//                {
//                    // Normal parameter
//                    writer.write (param_name);
//                    writer.write ("=");
//                    write_representation (param_value, serialization_queue);
//                }
//            }
//        }
//
//        writer.write ("\n");
//    }
//
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
        log.info ("Will write_properties()");

        // First write all Q-quark properties
//        for (Map.Entry<String, Object> property : props.entrySet ())
//        {
//            if (property.getKey ().contains ("/") ||
//                    property.getKey ().startsWith ("/") ||
//                    !property.getKey ().startsWith ("Q-"))
//            {
//                continue;
//            }
//
//            write_property (props, property.getKey (), serialization_queue);
//        }
        String[] keys = instance.getPropertyKeys ();

        // Now write all other properties (skipping all parameters)
        if (keys != null)
        {
            for (String key: keys)
            {
                Object value = instance.getProperty (key);

                writer.write (key);
                writer.write ("=");

                if (value instanceof String)
                {
                    writer.write ((String)value);
                }
                else
                {
                    writer.write ("INSTANCE");
                }

                writer.write ("\n");
//            if (property.getKey ().contains ("/") ||
//                    property.getKey ().startsWith ("/") ||
//                    property.getKey ().startsWith ("Q-"))
//            {
//                // Skip property parameters <Property>/<Parameter> and Q-properties
//                continue;
//            }
//
//            write_property (props, property.getKey (), serialization_queue);
            }
        }
    }

    public boolean writeRepresentation (GluonInstance instance)
        throws IOException
    {
        log.info ("writeRepresentation: representation={}", instance);

        file_format_boundary = (String)instance.getProperty (GluonConstants.CONTENT_BOUNDARY);

        // Minimum sanity please
        if (file_format_boundary == null
            || file_format_boundary.isEmpty ()
            || file_format_boundary.length () < 4)
        {
            file_format_boundary = GluonConstants.DEFAULT_BOUNDARY;
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
