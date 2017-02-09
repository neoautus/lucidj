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

import org.apache.commons.lang3.text.StrLookup;
import org.apache.commons.lang3.text.StrSubstitutor;
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

    private StrSubstitutor macro_subst;
    private GluonInstance macro_attr_entry;
    private String macro_attr_name;
    private String macro_attr_value;
    private String macro_attr_operator;

    public GluonWriter (Writer writer)
    {
        this.writer = writer;

        macro_subst = new StrSubstitutor (new StrLookup ()
        {
            @Override
            public String lookup (String s)
            {
                if (s == null)
                {
                    return ("null");
                }
                else if (s.equals ("attr.name"))
                {
                    return (macro_attr_name);
                }
                else if (s.equals ("attr.value"))
                {
                    return (macro_attr_value);
                }
                else if (s.equals ("attr.operator"))
                {
                    return (macro_attr_operator);
                }
                else
                {
                    GluonInstance property = macro_attr_entry.getProperty (s);

                    if (property.isPrimitive ())
                    {
                        return (property.getValue ());
                    }
                }
                return ("nil");
            }
        });
    }

    private void write_key_value (String key, String operator, GluonInstance entry)
        throws IOException
    {
        GluonInstance attr_entry = entry.getProperty (key);
        String attr_value = "\"(embedded)\"";

        // TODO: WHAT TO DO IF THE ENTRY IS AN OBJECT?
        if (attr_entry != null && attr_entry.isPrimitive ())
        {
            attr_value = attr_entry.getValue ();
        }

        // Do we have a macro here?
        if (attr_value.contains (GluonConstants.MACRO_CHAR))
        {
            int start_of_macro = attr_value.indexOf (GluonConstants.MACRO_CHAR);
            String macro = attr_value.substring (start_of_macro + 1);
            macro_attr_name = key;
            macro_attr_value = attr_value;
            macro_attr_entry = attr_entry;
            macro_attr_operator = operator;
            writer.write (macro_subst.replace (macro));
        }
        else
        {
            writer.write (key);
            writer.write (operator);
            writer.write (attr_value);
        }
    }

    private void write_primitive_properties (String key, GluonInstance value)
        throws IOException
    {
        //------------------------------------
        // key: value [; attribute=value ]...
        // (1)   (2)           (3)
        //------------------------------------

        // Key (1)
        writer.write (key);
        writer.write (": ");

        String[] attr_keyset = value.getPropertyKeys ();
        String semicolon = "";

        // Value (2)
        if (value.isPrimitive ())
        {
            String prop_value = value.getValue ();

            // Output only an existing value
            if (prop_value != null)
            {
                // NEVER apply macrosubstitution on property values
                if (prop_value.contains (GluonConstants.MACRO_CHAR))
                {
                    // Discard the macro portion of the value
                    int start_of_macro = prop_value.indexOf (GluonConstants.MACRO_CHAR);
                    writer.write (prop_value.substring (0, start_of_macro));
                }
                else
                {
                    // No macros, output everything
                    writer.write (prop_value);
                }
                semicolon = "; ";
            }
        }

        // Attributes (3)
        if (attr_keyset != null)
        {
            for (String attr_key : attr_keyset)
            {
                writer.write (semicolon);
                write_key_value (attr_key, "=", value);
                semicolon = "; ";
            }
        }
    }

    private void write_property (GluonInstance instance, String key)
        throws IOException
    {
        GluonInstance property = instance.getProperty (key);

        // Filter all complex objects swapping them with a reference
        // and moving the object into the serialization queue
        if (!property.isPrimitive ())
        {
            GluonInstance reference = property_to_reference.get (property);

            if (reference == null)
            {
                // Append embedding data into Object-Class property
                GluonInstance object_class = property.getProperty (GluonConstants.OBJECT_CLASS);
                object_class.setProperty ("embedded", true);
                object_class.setProperty ("id", ref_counter);

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

        // Write either a property or a reference to an object
        write_primitive_properties (key, property);
        writer.write ("\n");
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
