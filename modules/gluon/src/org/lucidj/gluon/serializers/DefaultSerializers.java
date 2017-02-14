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

package org.lucidj.gluon.serializers;

import org.apache.commons.lang3.StringEscapeUtils;
import org.lucidj.api.SerializerInstance;
import org.lucidj.gluon.GluonConstants;
import org.lucidj.gluon.GluonPrimitive;
import org.lucidj.gluon.GluonSerializer;

import javax.lang.model.type.NullType;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.regex.Pattern;

public class DefaultSerializers
{
    public static class NullSerializer implements GluonPrimitive
    {
        @Override
        public boolean serializeObject (SerializerInstance instance, Object object)
        {
            instance.setValue ("null");
            return (true);
        }

        @Override
        public Object deserializeObject (SerializerInstance instance)
        {
            return (NullType.class);
        }

        @Override
        public boolean match (String charseq)
        {
            return (charseq.equals ("null"));
        }
    }

    public static class IntSerializer implements GluonPrimitive
    {
        Pattern INT_PATTERN = Pattern.compile ("^-?\\d{1,10}$");

        @Override
        public boolean serializeObject (SerializerInstance instance, Object object)
        {
            instance.setValue (Integer.toString ((Integer)object));
            return (true);
        }

        @Override
        public Object deserializeObject (SerializerInstance instance)
        {
            return (Integer.valueOf (instance.getValue ()));
        }

        @Override
        public boolean match (String charseq)
        {
            return (INT_PATTERN.matcher (charseq).matches ());
        }
    }

    public static class StringSerializer implements GluonPrimitive
    {
        @Override
        public boolean serializeObject (SerializerInstance instance, Object object)
        {
            instance.setValue ("\"" + StringEscapeUtils.escapeJava ((String)object) + "\"");
            return (true);
        }

        @Override
        public Object deserializeObject (SerializerInstance instance)
        {
            String str = instance.getValue ();
            return (StringEscapeUtils.unescapeJava (str.substring (1, str.length () - 1)));
        }

        @Override
        public boolean match (String charseq)
        {
            return (charseq.startsWith ("\"") && charseq.endsWith ("\""));
        }
    }

    public static class FloatSerializer implements GluonPrimitive
    {
        @Override
        public boolean serializeObject (SerializerInstance instance, Object object)
        {
            instance.setValue (Float.toString ((Float)object) + "f");
            return (true);
        }

        @Override
        public Object deserializeObject (SerializerInstance instance)
        {
            return null;
        }

        @Override
        public boolean match (String charseq)
        {
            return (false);
        }
    }

    public static class BooleanSerializer implements GluonPrimitive
    {
        @Override
        public boolean serializeObject (SerializerInstance instance, Object object)
        {
            instance.setValue ((Boolean)object?
                "true" + GluonConstants.MACRO_CHAR + "${attr.name}":
                "false" + GluonConstants.MACRO_CHAR + "!${attr.name}");
            return (true);
        }

        @Override
        public Object deserializeObject (SerializerInstance instance)
        {
            String str = instance.getValue ();

            if (str.startsWith ("!"))
            {
                // Soo ugly it hurts my eyes :[
                GluonSerializer.GluonInstance hack = (GluonSerializer.GluonInstance)instance;
                GluonSerializer.GluonInstance obj = (GluonSerializer.GluonInstance)hack.getBackingObject ();
                obj.renameProperty (str, str.substring (1));
            }

            return (!str.startsWith ("!") && !str.equals ("false"));
        }

        private boolean is_valid_identifier (String str)
        {
            StringCharacterIterator scan = new StringCharacterIterator (str);
            char ch = scan.first ();

            // The identifier may be preceded by '!'
            if (ch == '!')
            {
                ch = scan.next ();
            }

            if (!Character.isJavaIdentifierStart (ch))
            {
                return (false);
            }

            while ((ch = scan.next ()) != CharacterIterator.DONE)
            {
                if (!Character.isJavaIdentifierPart (ch))
                {
                    return (false);
                }
            }
            return (true);
        }

        @Override
        public boolean match (String charseq)
        {
            // The special case: null is not boolean
            if (charseq.equals ("null"))
            {
                return (false);
            }

            // The canonical cases
            if (charseq.equals ("true") || charseq.equals ("false"))
            {
                return (true);
            }

            // The special shorthand case (any valid java identifier)
            return (is_valid_identifier (charseq));
        }
    }

    public static class ByteSerializer implements GluonPrimitive
    {
        @Override
        public boolean serializeObject (SerializerInstance instance, Object object)
        {
            return false;
        }

        @Override
        public Object deserializeObject (SerializerInstance instance)
        {
            return null;
        }

        @Override
        public boolean match (String charseq)
        {
            return (false);
        }
    }

    public static class CharSerializer implements GluonPrimitive
    {
        @Override
        public boolean serializeObject (SerializerInstance instance, Object object)
        {
            return false;
        }

        @Override
        public Object deserializeObject (SerializerInstance instance)
        {
            return null;
        }

        @Override
        public boolean match (String charseq)
        {
            return (false);
        }
    }

    public static class ShortSerializer implements GluonPrimitive
    {
        @Override
        public boolean serializeObject (SerializerInstance instance, Object object)
        {
            return false;
        }

        @Override
        public Object deserializeObject (SerializerInstance instance)
        {
            return null;
        }

        @Override
        public boolean match (String charseq)
        {
            return (false);
        }
    }

    public static class LongSerializer implements GluonPrimitive
    {
        Pattern LONG_PATTERN = Pattern.compile("^-?\\d{1,19}L$");

        @Override
        public boolean serializeObject (SerializerInstance instance, Object object)
        {
            instance.setValue (Long.toString ((Long)object) + "L");
            return (true);
        }

        @Override
        public Object deserializeObject (SerializerInstance instance)
        {
            String str = instance.getValue ();
            return (Long.valueOf (str.substring (0, str.length () - 1)));
        }

        @Override
        public boolean match (String charseq)
        {
            return (LONG_PATTERN.matcher (charseq).matches ());
        }
    }

    public static class DoubleSerializer implements GluonPrimitive
    {
        @Override
        public boolean serializeObject (SerializerInstance instance, Object object)
        {
            instance.setValue (Double.toString ((Double)object) + "d");
            return (true);
        }

        @Override
        public Object deserializeObject (SerializerInstance instance)
        {
            return null;
        }

        @Override
        public boolean match (String charseq)
        {
            return (false);
        }
    }
}

// EOF
