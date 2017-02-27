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

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GluonUtil
{
    private final static transient Logger log = LoggerFactory.getLogger (GluonUtil.class);

    private static void dump_instance (GluonInstance instance, Writer writer, int level)
        throws IOException
    {
        String indent = new String(new char [level * 8]).replace('\0', ' ');

        String[] keyset = instance.getPropertyKeys ();

        String class_name = instance.getBackingObject () == null?
            "": " (" + instance.getBackingObject ().getClass ().getName () + ")";

        writer.write (indent + "Backing Object: " +
            ((instance.getBackingObject () == null)? "<null>": instance.getBackingObject ()) + class_name + "\n");
        writer.write (indent + "Representation: " +
            ((instance.getValue () == null)? "<null>": instance.getValue ()) + "\n");

        // First write all X- properties
        if (keyset == null)
        {
            //writer.write (indent + "No Properties\n");
        }
        else
        {
            for (String key: keyset)
            {
                GluonInstance property = instance.getProperty (key);
                writer.write (indent + "Property: " + key + " (" +
                    ((property.getProperty (GluonConstants.OBJECT_CLASS) != null)? "Object": "Primitive") + ")\n");

                dump_instance (property, writer, level + 1);
            }
        }

        if (instance.getEmbeddedObjects () == null)
        {
            //writer.write (indent + "No Objects\n");
        }
        else
        {
            for (GluonInstance obj: instance.getEmbeddedObjects ())
            {
                writer.write (indent + "Object:\n");
                dump_instance (obj, writer, level + 1);
            }
        }
    }

    public static void dumpRepresentation (GluonInstance root, String filename)
    {
        Path userdir = FileSystems.getDefault ().getPath (System.getProperty ("rq.home"), "userdata");
        Path destination_path = userdir.resolve (filename);
        Charset cs = Charset.forName ("UTF-8");
        Writer writer = null;

        try
        {
            writer = Files.newBufferedWriter (destination_path, cs);
            dump_instance (root, writer, 1);
        }
        catch (Exception e)
        {
            log.error ("Exception on serialization", e);
        }
        finally
        {
            try
            {
                if (writer != null)
                {
                    writer.close();
                }
            }
            catch (Exception ignore) {};
        }
    }
}

// EOF