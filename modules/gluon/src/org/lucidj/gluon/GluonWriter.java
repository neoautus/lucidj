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
    private String macro_attr_name;
    private String macro_attr_value;
    private String macro_attr_operator;

    public GluonWriter (Writer writer)
    {
        this.writer = writer;

        // Initialize our very exclusive macro processor :)
        macro_subst = new StrSubstitutor (new StrLookup ()
        {
            @Override
            public String lookup (String s)
            {
                if (s.equals ("attr.name"))
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
                return (null);
            }
        });
    }

    private String extract_macro (String value)
    {
        int macro_marker = value.indexOf (GluonConstants.MACRO_CHAR);
        int start_of_macro = macro_marker + GluonConstants.MACRO_CHAR.length ();
        return (value.substring (start_of_macro));
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
            String macro = extract_macro (attr_value);
            macro_attr_name = key;
            macro_attr_value = attr_value;
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

    private String strip_macros (String value)
    {
        // Macros are stored on the string AFTER the MACRO_CHAR marker
        int start_of_macro = value.indexOf (GluonConstants.MACRO_CHAR);
        return (value.substring (0, start_of_macro));
    }

    private void write_value_and_attributes (GluonInstance value)
        throws IOException
    {
        String[] attr_keyset = value.getPropertyKeys ();
        String semicolon = "";

        // Value
        if (value.isPrimitive ())
        {
            String prop_value = value.getValue ();

            // Output only an existing value
            if (prop_value != null)
            {
                // NEVER apply macrosubstitution on property values
                if (prop_value.contains (GluonConstants.MACRO_CHAR))
                {
                    writer.write (strip_macros (prop_value));
                }
                else
                {
                    writer.write (prop_value);
                }
                semicolon = "; ";
            }
        }

        // Attributes
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

    private void filter_and_write_value_and_attributes (GluonInstance property)
        throws IOException
    {
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
        write_value_and_attributes (property);
    }

    private void write_property (GluonInstance property, String property_name)
        throws IOException
    {
        writer.write (property_name);
        writer.write (": ");

        // We write either the object list OR the attributes
        // because we handle lists and arrays as primitive types,
        // that don't have any attributes. Of course you can
        // use SerializerInstance.setProperty() to set some
        // attribute, but it will be ignored.
        if (property.hasObjects ())
        {
            String comma = "";

            // Write all objects embedded into the property
            for (GluonInstance embedded_object: property.getObjects ())
            {
                writer.write (comma);
                filter_and_write_value_and_attributes (embedded_object);
                comma = ", ";
            }
        }
        else
        {
            // Write the object properties
            filter_and_write_value_and_attributes (property);
        }

        writer.write ("\n");
    }

    private void write_all_object_properties (GluonInstance object)
        throws IOException
    {
        if (!object.hasProperties ())
        {
            writer.write ("# No properties for this object\n");
            return;
        }

        String[] keyset = object.getPropertyKeys ();

        // First write all X- properties
        for (String key: keyset)
        {
            if (!key.startsWith ("X-") || key.startsWith ("."))
            {
                continue;
            }
            write_property (object.getProperty (key), key);

        }

        // Now write all other properties
        for (String key: keyset)
        {
            if (key.startsWith ("X-") || key.startsWith ("."))
            {
                continue;
            }
            write_property (object.getProperty (key), key);
        }
    }

    private void write_object (GluonInstance object)
        throws IOException
    {
        writer.write ("\n");
        writer.write (file_format_boundary);
        writer.write ("\n");

        write_all_object_properties (object);

        writer.write ("\n");
        writer.write (object.getValue ());
    }

    public boolean writeRepresentation (GluonInstance root)
        throws IOException
    {
        GluonInstance content_boundary = root.getProperty (GluonConstants.CONTENT_BOUNDARY);

        if (content_boundary != null && content_boundary.isPrimitive ())
        {
            file_format_boundary = (String)content_boundary.getBackingObject ();
        }

        // Minimum sanity please
        if (file_format_boundary == null
            || file_format_boundary.isEmpty ()
            || file_format_boundary.length () < 8)
        {
            file_format_boundary = GluonConstants.DEFAULT_BOUNDARY;
            root.setProperty (GluonConstants.CONTENT_BOUNDARY, file_format_boundary);
        }

        /************************/
        /* HEADER (ROOT OBJECT) */
        /************************/

        // Write the properties of root object
        write_all_object_properties (root);

        // The value of root is ignored since we use the first char sequence after the
        // newline break from properties as section boundary.

        /***************************/
        /* MAIN SERIALIZED OBJECTS */
        /***************************/

        // Write all top-level objects
        if (root.getObjects () != null)
        {
            for (GluonInstance object: root.getObjects ())
            {
                write_object (object);
            }
        }

        // Write all nested objects remaining
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
        writer.write ("//\n");
        return (true);
    }
}

// EOF
