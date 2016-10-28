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

package org.lucidj.quark;

import com.google.common.base.Throwables;
import com.google.common.io.BaseEncoding;
import org.apache.commons.lang3.StringEscapeUtils;
import org.lucidj.api.Quark;
import org.lucidj.api.QuarkException;
import org.lucidj.api.QuarkSerializable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.*;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public class QuarkSerializer implements ServiceTrackerCustomizer<QuarkSerializable, QuarkSerializable>
{
    private final transient Logger log = LoggerFactory.getLogger (QuarkSerializer.class);

    private int ref_counter = 1;

    private String file_format_boundary;

    private BaseEncoding base64 = BaseEncoding.base64().withSeparator("\n", 76);
    private KryoSerializer kryo = new KryoSerializer();

    private ClassLoader qcl;

    private BundleContext ctx;
    private ServiceTracker<QuarkSerializable, QuarkSerializable> tracker;
    private Map<ServiceReference<QuarkSerializable>, QuarkSerializable> custom_serializers = new HashMap<> ();

    public QuarkSerializer (BundleContext ctx, ClassLoader cld)
    {
        this.ctx = ctx;
        this.qcl = cld;

        kryo.setClassLoader (cld);

        log.info ("QuarkSerializer-NG: {} [qcl={}, kryo={}]", this, qcl, kryo);

        tracker = new ServiceTracker<> (ctx, QuarkSerializable.class, this);
        tracker.open ();
    }

    private QuarkSerializable find_serializer (Class cls)
    {
        for (ServiceReference<QuarkSerializable> sref: custom_serializers.keySet ())
        {
            QuarkSerializable qs = custom_serializers.get (sref);

            if (qs.compatibleClass (cls))
            {
                return (qs);
            }
        }

        return (null);
    }

    /****************************/
    /* OBJECT => REPRESENTATION */
    /****************************/

    public Map<String, Object> build_representation(Object obj)
    {
        Map<String, Object> object_properties = null;
        String simple_object = null;
        Object complex_object = null;

        log.info ("build_representation: obj={}", obj);

        if (obj instanceof Quark)
        {
            log.info ("build_representation: Quark obj={}", obj);

            // TODO: HANDLE LEAKING EXCEPTIONS THROWN FROM serializeObject()
            object_properties = ((Quark)obj).serializeObject ();

            log.info ("build_representation: Quark object_properties=[ {} ]", object_properties);

            complex_object = object_properties.get ("/");

            object_properties.put (QuarkFields.OBJECT_CLASS, obj.getClass().getCanonicalName());
            object_properties.put (QuarkFields.CONTENT_TYPE, "text/plain");
        }
        else
        {
            object_properties = new HashMap<>();
            QuarkSerializable qs = null;

            if (obj == null)
            {
                simple_object = "null";
            }
            else if ((qs = find_serializer (obj.getClass ())) != null)
            {
                log.info ("CustomSerializer: {}", qs);

                // TODO: HANDLE LEAKING EXCEPTIONS THROWN FROM serializeObject()
                object_properties = qs.serializeObject (obj);

                log.info ("build_representation: Quark object_properties=[ {} ]", object_properties);

                complex_object = object_properties.get ("/");

                object_properties.put (QuarkFields.OBJECT_CLASS, obj.getClass().getCanonicalName());
                object_properties.put (QuarkFields.CONTENT_TYPE, "text/plain");
            }
            else if (obj instanceof Object[])
            {
                // Deal with an array of objects
                //...
                simple_object = "(Holy crap, it's an Array!)";
            }
            else if (obj instanceof String)
            {
                simple_object = "\"" + StringEscapeUtils.escapeJava ((String)obj) + "\"";
            }
            else if (obj instanceof Boolean)
            {
                simple_object = (Boolean)obj? "true": "false";
            }
            else if (obj instanceof Integer)
            {
                simple_object = Integer.toString((Integer)obj);
            }
            else if (obj instanceof Long)
            {
                simple_object = Long.toString((Long)obj) + "L";
            }
            else if (obj instanceof Float)
            {
                simple_object = Float.toString((Float)obj) + "f";
            }
            else if (obj instanceof Double)
            {
                simple_object = Double.toString((Double)obj) + "d";
            }
            else // Serialize an unknown object
            {
                // Opaque serialized object:
                // object_properties: serialization info
                object_properties = new HashMap<String, Object>();
                object_properties.put (QuarkFields.CONTENT_TYPE, "application/java-serialized-object");
                object_properties.put (QuarkFields.OBJECT_CLASS, obj.getClass().getCanonicalName());

                try
                {
                    // TODO: PLUGGABLE SERIALIZER
                    complex_object = ByteBuffer.wrap(kryo.serialize(obj));
                }
                catch (Exception e)
                {
                    log.info ("build_representation: Error serializing", e);
                }
            }
        }

        log.info("simple_object = [" + simple_object + "]");
        log.info("complex_object = [" + complex_object + "]");
        log.info("object_properties = [" + object_properties + "]");

        if (complex_object != null)
        {
            if (complex_object instanceof String)
            {
                // Use the string as-is
                object_properties.put (QuarkFields.COMPLEX_REPRESENTATION, (String)complex_object);
            }
            else if (complex_object instanceof ByteBuffer)
            {
                // Convert buffer to base64-encoded string
                ByteBuffer object_body_bf = (ByteBuffer)complex_object;
                object_properties.put (QuarkFields.COMPLEX_REPRESENTATION, base64.encode(object_body_bf.array()));
                object_properties.put (QuarkFields.CONTENT_ENCODING, "base64");
            }
        }
        else if (simple_object != null)
        {
            object_properties.put (QuarkFields.SIMPLE_REPRESENTATION, simple_object);
        }
        else
        {
            object_properties.put (QuarkFields.SIMPLE_REPRESENTATION, "(Unknown)");
        }

        return (object_properties);
    }

    private void write_representation (Writer wrt, Object obj, List<Map <String, Object>> serialization_queue)
        throws Exception
    {
        Map<String, Object> representation = build_representation(obj);

        if (representation.containsKey (QuarkFields.SIMPLE_REPRESENTATION))
        {
            // Simple representation is always string
            wrt.write((String)representation.get (QuarkFields.SIMPLE_REPRESENTATION));
        }
        else if (representation.containsKey (QuarkFields.COMPLEX_REPRESENTATION))
        {
            // Insert reference data into the object representation
            representation.put (QuarkFields.OBJECT_CLASS_EMBEDDED, true);
            representation.put (QuarkFields.OBJECT_CLASS_ID, ref_counter);

            // Write the reference to the embedded object
            wrt.write("embedded; refid=");
            wrt.write(Integer.toString (ref_counter));

            // Store the complex representation
            serialization_queue.add(representation);
            ref_counter++;
        }
        else
        {
            wrt.write("(Something is wrong...)");
        }
    }

    private void write_token_property (Writer wrt, QuarkToken token, String key,
        List<Map <String, Object>> serialization_queue)
        throws Exception
    {
        log.info ("write_token_property() key=[{}] value=[{}]", key, token);

        // PropertyKey: PropertyValue
        wrt.write (key);
        wrt.write (": ");
        wrt.write (token.getName ());

        Map<String, Object> parameters = token.getProperties ();

        // Look for property parameters <Property>/<Parameter>
        for (Map.Entry<String, Object> param: parameters.entrySet())
        {
            String param_name = param.getKey ();
            Object param_value = param.getValue ();

            log.info ("Write param name=[{}] value=[{}]", param_name, param_value);

            wrt.write ("; ");

            // Boolean parameters have special shortcuts
            if (param_value instanceof Boolean)
            {
                if (!(Boolean)param_value)
                {
                    wrt.write("!");
                }
                wrt.write(param_name);
            }
            else
            {
                // Normal parameter
                wrt.write(param_name);
                wrt.write("=");
                write_representation(wrt, param_value, serialization_queue);
            }
        }

        wrt.write("\n");
    }

    private void write_primitive_property (Writer wrt, Map<String, Object> props, String key,
        List<Map <String, Object>> serialization_queue)
        throws Exception
    {
        log.info ("write_property() key=[" + key + "] value=[" + props.get(key) + "]");

        // PropertyKey: PropertyValue
        wrt.write (key);
        wrt.write (": ");
        write_representation (wrt, props.get(key), serialization_queue);

        String property_param = key + "/";

        // Look for property parameters <Property>/<Parameter>
        for (Map.Entry<String, Object> param: props.entrySet())
        {
            if (param.getKey().startsWith(property_param))
            {
                String param_key = param.getKey();
                String param_name = param_key.substring(property_param.length());
                Object param_value = props.get(param_key);

                log.info ("Write param name=[" + param_name +
                        "] value=[" + param_value + "]");

                wrt.write ("; ");

                // Boolean parameters have special shortcuts
                if (param_value instanceof Boolean)
                {
                    if (!(Boolean)param_value)
                    {
                        wrt.write("!");
                    }
                    wrt.write(param_name);
                }
                else
                {
                    // Normal parameter
                    wrt.write(param_name);
                    wrt.write("=");
                    write_representation(wrt, param_value, serialization_queue);
                }
            }
        }

        wrt.write("\n");
    }

    private void write_property (Writer wrt, Map<String, Object> props, String key,
        List<Map <String, Object>> serialization_queue)
        throws Exception
    {
        Object obj = props.get (key);
        Map<String, Object> representation = build_representation (obj);

        if (representation.containsKey (QuarkFields.COMPLEX_REPRESENTATION))
        {
            QuarkToken embedded = new QuarkToken ("Embedded");
            embedded.setProperty ("refid", ref_counter);
            representation.put (QuarkFields.SIMPLE_REPRESENTATION, embedded);

            // Insert reference data into the object representation
            representation.put (QuarkFields.OBJECT_CLASS_EMBEDDED, true);
            representation.put (QuarkFields.OBJECT_CLASS_ID, ref_counter);

            // Store the complex representation
            serialization_queue.add(representation);
            ref_counter++;

            // Write token
            write_token_property (wrt, embedded, key, serialization_queue);
        }
        else
        {
            write_primitive_property (wrt, props, key, serialization_queue);
        }
    }

    private void write_properties (Writer wrt, Map<String, Object> props, List<Map <String, Object>> serialization_queue)
        throws Exception
    {
        log.info("Will write_properties()");
        dump_properties(props);

        // First write all Q-quark properties
        for (Map.Entry<String, Object> property: props.entrySet())
        {
            if (property.getKey().contains("/") ||
                property.getKey().startsWith("/") ||
                !property.getKey().startsWith("Q-"))
            {
                continue;
            }

            write_property(wrt, props, property.getKey(), serialization_queue);
        }

        // Now write all other properties (skipping all parameters)
        for (Map.Entry<String, Object> property: props.entrySet())
        {
            if (property.getKey().contains("/") ||
                property.getKey().startsWith("/") ||
                property.getKey().startsWith("Q-"))
            {
                // Skip property parameters <Property>/<Parameter> and Q-properties
                continue;
            }

            write_property(wrt, props, property.getKey(), serialization_queue);
        }
    }

    public boolean serializeObject(Writer wrt, Object obj, Map<String, Object> obj_prop)
        throws Exception
    {
        file_format_boundary = (String)obj_prop.get (QuarkFields.CONTENT_BOUNDARY);

        // Minimum sanity please
        if (file_format_boundary == null ||
            file_format_boundary.isEmpty() ||
            file_format_boundary.length() < 4)
        {
            file_format_boundary = QuarkFields.DEFAULT_BOUNDARY;
        }

        /*********************/
        /* QUARK FILE HEADER */
        /*********************/

        if (obj_prop.containsKey (QuarkFields.FILE_HANDLER))
        {
            wrt.write ((String)obj_prop.get (QuarkFields.FILE_HANDLER));
        }
        else
        {
            wrt.write (QuarkFields.DEF_FILE_HANDLER);
        }
        wrt.write("\n");

        // Build root object properties
        HashMap<String, Object> properties = new HashMap<>();
        properties.put (QuarkFields.QUARK_VERSION, "1.0.0");
        properties.put (QuarkFields.CONTENT_TYPE, "multipart/mixed");
        properties.put (QuarkFields.CONTENT_BOUNDARY, QuarkFields.DEFAULT_BOUNDARY);
        properties.putAll (obj_prop);
        properties.put (QuarkFields.OBJECT_CLASS, obj.getClass().getCanonicalName());

        List<Map <String, Object>> serialization_queue = new ArrayList<>();

        write_properties (wrt, properties, serialization_queue);

        /***************************/
        /* MAIN SERIALIZED OBJECTS */
        /***************************/

        if (obj instanceof List)
        {
            List obj_list = (List)obj;

            log.info("serializeObject: obj_list = " + obj_list);

            // Serialize each object
            for (int i = 0; i < obj_list.size(); i++)
            {
                Object source = obj_list.get(i);
                log.info("source = " + source);

                Map<String, Object> obj_representation = build_representation(source);
                dump_properties(obj_representation);

                wrt.write("\n");
                wrt.write(file_format_boundary);
                wrt.write("\n");

                write_properties(wrt, obj_representation, serialization_queue);

                // Dump /complex representation only if available
                if (obj_representation.get (QuarkFields.COMPLEX_REPRESENTATION) != null)
                {
                    wrt.write("\n"); // Empty line
                    wrt.write((String)obj_representation.get (QuarkFields.COMPLEX_REPRESENTATION));
                }
            }
        }
        else
        {
            // Serialize just this one
        }

        if (!serialization_queue.isEmpty())
        {
            for (int i = 0; i < serialization_queue.size(); i++)
            {
                Map<String, Object> obj_representation = serialization_queue.get(i);

                dump_properties(obj_representation);

                wrt.write("\n");
                wrt.write(file_format_boundary);
                wrt.write("\n");

                write_properties(wrt, obj_representation, serialization_queue);

                wrt.write("\n"); // Empty line
                wrt.write((String)obj_representation.get (QuarkFields.COMPLEX_REPRESENTATION));
            }
        }

        /*******************/
        /* WRITE SIGNATURE */
        /*******************/

        /***********/
        /* THE END */
        /***********/

        wrt.write("\n");
        wrt.write(file_format_boundary);
        wrt.write("--\n");

        return (true);
    }

    public boolean serializeObject(Writer wrt, Object obj)
        throws Exception
    {
        if (obj instanceof Quark)
        {
            return (serializeObject (wrt, obj, ((Quark)obj).serializeObject ()));
        }
        return (false);
    }

    /****************************/
    /* REPRESENTATION => OBJECT */
    /****************************/

    private Object build_object_instance (Map<String, Object> obj_repr)
    {
        Object new_object = null;
        String q_object;

        // Get object class name
        if (obj_repr.containsKey (QuarkFields.OBJECT_CLASS) &&
            obj_repr.get (QuarkFields.OBJECT_CLASS) instanceof String)
        {
            q_object = (String) obj_repr.get (QuarkFields.OBJECT_CLASS);
        }
        else
        {
            return (null);
        }

        try
        {
            Class cls = null;

            try
            {
                cls = qcl.loadClass (q_object);
            }
            catch (ClassNotFoundException ignore) {};

            if (cls == null)
            {
                // TODO: BETTER REPRESENTATION FOR NON-EXISTING CLASSES
                new_object = new QuarkException ("Class '" + q_object + "' not available");
            }
            else if (Quark.class.isAssignableFrom (cls))
            {
                log.info("new_object: Quark class {}", cls);

                new_object = cls.newInstance();
                ((Quark)new_object).deserializeObject (obj_repr);

                log.info("new_object: Quark object {}", new_object);
            }
            else if (obj_repr.containsKey (QuarkFields.CONTENT_TYPE) &&
                "application/java-serialized-object".equals (obj_repr.get (QuarkFields.CONTENT_TYPE)))
            {
                Object content = obj_repr.get("/");

                if (content instanceof ByteBuffer)
                {
                    byte[] bin_content = ((ByteBuffer)content).array();

                    try
                    {
                        // TODO: MAKE IT ASYNCHRONOUS
                        new_object = kryo.deserialize(bin_content);

                        log.info ("OBJECT: {}", new_object);
                    }
                    catch (Exception e)
                    {
                        log.info ("Deserializer exception: {}", e);
                    }
                }
                else
                {
                    log.info("ERROR: Missing ByteBuffer");
                }
            }
            else // Perhaps a custom serializer??
            {
                QuarkSerializable qs = find_serializer (cls);

                if (qs != null)
                {
                    log.info ("Custom Deserializer: {}", qs);

                    new_object = qs.deserializeObject (obj_repr);
                    log.info("new_object: {}", new_object);
                }
            }
        }
        catch (Exception e)
        {
            StringWriter errors = new StringWriter();
            e.printStackTrace(new PrintWriter(errors));
            String error_message = Throwables.getStackTraceAsString(e);
            log.info("Error creating object: " + error_message);
        }

        return (new_object);
    }

    private boolean build_object (Map<String, Object> obj_repr, QuarkMetadata obj_meta)
    {
        boolean object_ready = true;

        // Scan all object properties...
        for (Map.Entry<String, Object> props: obj_repr.entrySet())
        {
            // Any embedding?
            if (props.getValue () instanceof QuarkToken)
            {
                QuarkToken inline = (QuarkToken)props.getValue ();
                int refid = (Integer)inline.getProperty ("refid");
                Map<String, Object> obj_ref = obj_meta.embedded_objects.get (refid);

                log.info("Object have embedding at " + props.getKey());

                if (obj_ref == null) // Does the embedding exists?
                {
                    // Stores a null object
                    obj_repr.put(props.getKey(), null);
                }
                else if (obj_ref.containsKey("/object")) // Is the requested object available?
                {
                    // It is, replace "embedded" with the object itself
                    obj_repr.put(props.getKey(), obj_ref.get("/object"));
                    log.info("Object embedding ready");
                }
                else
                {
                    // The requested object still not available
                    object_ready = false;
                    log.info("Object embedding pending");
                }
            }
        }

        if (!object_ready)
        {
            log.info("Object NOT ready");
            return(false);
        }

        log.info("Object ready");

        if (!obj_repr.containsKey("/object"))
        {
            log.info("Object ready => Create instance: " + obj_repr);
            // Create the object
            obj_repr.put("/object", build_object_instance(obj_repr));
        }

        return(true);
    }

    private Object build_objects (QuarkMetadata obj_meta)
    {
        boolean all_objects_created;
        Object root_object = null;

        if (build_object (obj_meta.getRootProperties (), obj_meta))
        {
            root_object = obj_meta.getRootProperties ().get ("/object");
        }
        else
        {
            // TODO: USE A DORMANT OBJECT
        }

        // Cycle all objects resolving the dependencies
        // TODO: COUNT CREATIONS TO AVOID INFINITE LOOP
        do
        {
            all_objects_created = true;

            log.info("1>>> CHECK ALL OBJECTS");

            // Scan embedded map
            for (Map.Entry<Integer, Map<String, Object>> obj: obj_meta.embedded_objects.entrySet())
            {
                Map<String, Object> obj_repr = obj.getValue();

                log.info("Embedded Object: {}", obj_repr);

                all_objects_created &= build_object(obj_repr, obj_meta);
            }

            // Scan sequential objects
            for (int i = 0; i < obj_meta.serialization_queue.size(); i++)
            {
                Map<String, Object> obj_repr = obj_meta.serialization_queue.get(i);

                log.info("Sequential Object [{}] = {}", i, obj_repr);

                all_objects_created &= build_object(obj_repr, obj_meta);
            }

            log.info("Scanning finished: all_objects_created = " + all_objects_created);
        }
        while (!all_objects_created);

        if (root_object != null && root_object instanceof List)
        {
            List<Object> object_list = (List<Object>)root_object;

            // Copy created objects to object list
            for (Map<String, Object> obj_repr : obj_meta.serialization_queue)
            {
                log.info("ADDING " + obj_repr.get("/object"));
                object_list.add(obj_repr.get("/object"));
            }
        }

        if (root_object instanceof Quark)
        {
            ((Quark)root_object).deserializeObject (obj_meta.getRootProperties ());
        }

        return (root_object);
    }

    public Object deserializeObject(Reader reader)
        throws Exception
    {
        QuarkMetadata qm = new QuarkMetadata ();
        //QuarkMetadata.InternalObjectMetadata obj_meta;

        try
        {
            qm.readObjectMetadata (reader);
        }
        catch (Exception e)
        {
            log.error ("Error reading quark file: {}", e);
            return (null);
        }
        finally
        {
            try
            {
                // TODO: DO WE REALLY NEED TO CLOSE THIS??
                reader.close();
            }
            catch (Exception ignore) {};
        }

        if (qm.isValid ())
        {
            return (build_objects (qm));
        }

        return (null);
    }

    @Override // ServiceTrackerCustomizer
    public QuarkSerializable addingService (ServiceReference<QuarkSerializable> serviceReference)
    {
        QuarkSerializable serializer = ctx.getService (serviceReference);
        custom_serializers.put (serviceReference, serializer);

        log.info ("addingService Serializer: {}: {}", serviceReference, serializer);

        // We need to return the object in order to track it
        return (serializer);
    }

    @Override // ServiceTrackerCustomizer
    public void modifiedService (ServiceReference<QuarkSerializable> serviceReference, QuarkSerializable quarkSerializable)
    {
        log.info ("modifiedService Serializer: {}: {}", serviceReference, quarkSerializable);
        custom_serializers.put (serviceReference, quarkSerializable);
    }

    @Override // ServiceTrackerCustomizer
    public void removedService (ServiceReference<QuarkSerializable> serviceReference, QuarkSerializable quarkSerializable)
    {
        ctx.ungetService (serviceReference);
        custom_serializers.remove (serviceReference);
        log.info ("removedService Serializer: {}: {}", serviceReference, quarkSerializable);
    }

    /*************/
    /* UTILITIES */
    /*************/

    void dump (byte[] packet_data, String title)
    {
        String buffer;
        int line;
        int fills = 30;
        int packet_size = packet_data.length;

        buffer = "===";

        if (title != null)
        {
            buffer += "[" + title + "]";

            fills = 30 - title.length() - 2;
        }
        while (fills-- != 0)
        {

            buffer = buffer + "=";
        }

        String tmp = "0000" + Integer.toString (packet_size);
        String size = tmp.substring(tmp.length () - 5);

        buffer = buffer + "========================[size=" + size + "]===";

        log.info (buffer);

        for (line = 0; line < packet_size; line += 16)
        {
            int col;
            int num_cols = packet_size - line > 16? 16: packet_size - line;
            String temp = "000" + Integer.toHexString(line);
            buffer = temp.substring(temp.length () - 4) + "  ";

            for (col = 0; col < 16; col++)
            {
                if (col < num_cols)
                {
                    temp = "0" + Integer.toHexString(packet_data [line + col]);
                    buffer += temp.substring(temp.length () - 2) + " ";
                }
                else
                {
                    buffer += "   ";
                }

                if (col == 7)
                {
                    buffer += " ";
                }
            }

            buffer += " ";

            for (col = 0; col < 16; col++)
            {
                if (col < num_cols)
                {
                    buffer += ((packet_data [line + col] >= 0x20) && (packet_data [line +col]) < 0x7f)?
                            Character.toString((char)(packet_data [line+col])): ".";
                }
                else
                {
                    buffer += " ";
                }
            }

            log.info (buffer);
        }

        log.info ("------------------------------------------------------------------------");
    }

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
