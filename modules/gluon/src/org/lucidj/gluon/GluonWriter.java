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

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GluonWriter
{
    private final static transient Logger log = LoggerFactory.getLogger (GluonWriter.class);

    private int ref_counter = 1;
    private Writer writer;
    private String file_format_boundary;

    private List<GluonInstance> serialization_queue = new ArrayList<> ();
    private Map<GluonInstance, GluonInstance> property_to_reference = new HashMap<> ();

    public GluonWriter (Writer writer)
    {
        this.writer = writer;
    }

    private void write_primitive_property (String key, GluonInstance value)
        throws IOException
    {
        writer.write (key);
        writer.write (": ");

        String[] attr_keyset = value.getPropertyKeys ();
        String semicolon = "";

        if (value.isPrimitive ())
        {
            writer.write (value.getValue ());
            semicolon = "; ";
        }

        if (attr_keyset != null)
        {
            for (String attr_key : attr_keyset)
            {
                writer.write (semicolon);
                writer.write (attr_key);
                writer.write ("=");
                writer.write ((value.getProperty (attr_key)).getValue ());
                semicolon = "; ";
            }
        }
        writer.write ("\n");
    }

    private void write_property (GluonInstance instance, String key)
        throws IOException
    {
        GluonInstance property = instance.getProperty (key);

        // Filter all complex objects swapping them with a reference
        // and putting they into the serialization queue
        if (!property.isPrimitive ())
        {
            GluonInstance reference = property_to_reference.get (property);

            if (reference == null)
            {
                // Append embedding data into Object-Class property
                GluonInstance object_class = property.getProperty (GluonConstants.OBJECT_CLASS);
                object_class.setProperty ("id", ref_counter);
                object_class.setProperty ("embedded", true);

                // Create a reference pointing back to object
                reference = property.newInstance ();
                reference.setProperty ("embedded", true);
                reference.setProperty ("refid", ref_counter);

                // Insert property into serialization queue for embedded objects
                property_to_reference.put (property, reference);
                serialization_queue.add (property);
                ref_counter++;
            }
            property = reference;
        }
        write_primitive_property (key, property);
    }

    private void write_properties (GluonInstance instance)
        throws IOException
    {
        if (!instance.hasProperties ())
        {
            writer.write ("# No properties for this object\n");
            return;
        }

        String[] keyset = instance.getPropertyKeys ();

        // First write all X- properties
        for (String key: keyset)
        {
            if (!key.startsWith ("X-"))
            {
                continue;
            }
            write_property (instance, key);
        }

        // Now write all other properties
        for (String key: keyset)
        {
            if (key.startsWith ("X-"))
            {
                continue;
            }
            write_property (instance, key);
        }
    }

    private void write_object (GluonInstance object)
        throws IOException
    {
        writer.write ("\n");
        writer.write (file_format_boundary);
        writer.write ("\n");

        write_properties (object);

        writer.write ("\n"); // Empty line
        writer.write (object.getValue ());
    }

    public boolean writeRepresentation (GluonInstance root)
        throws IOException
    {
        GluonInstance content_boundary = root.getProperty (GluonConstants.CONTENT_BOUNDARY);

        if (content_boundary != null && content_boundary.isPrimitive ())
        {
            file_format_boundary = content_boundary.getValue ();
        }

        // Minimum sanity please
        if (file_format_boundary == null
            || file_format_boundary.isEmpty ()
            || file_format_boundary.length () < 8)
        {
            file_format_boundary = GluonConstants.DEFAULT_BOUNDARY;
            root.setProperty (GluonConstants.CONTENT_BOUNDARY, file_format_boundary);
        }

        // Write the properties of root object
        write_properties (root);

        /***************************/
        /* MAIN SERIALIZED OBJECTS */
        /***************************/

        if (root.getObjects () != null)
        {
            for (GluonInstance object: root.getObjects ())
            {
                write_object (object);
            }
        }
        else
        {
            // Serialize just this one
        }

        for (GluonInstance object: serialization_queue)
        {
            write_object (object);
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
