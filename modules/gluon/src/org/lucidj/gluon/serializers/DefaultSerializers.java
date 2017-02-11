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
            return (null);
        }

        @Override
        public boolean match (String charseq)
        {
            return (false);
        }
    }

    public static class IntSerializer implements GluonPrimitive
    {
        @Override
        public boolean serializeObject (SerializerInstance instance, Object object)
        {
            instance.setValue (Integer.toString ((Integer)object));
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
            return null;
        }

        @Override
        public boolean match (String charseq)
        {
            return (false);
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
            return null;
        }

        @Override
        public boolean match (String charseq)
        {
            return (false);
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
        @Override
        public boolean serializeObject (SerializerInstance instance, Object object)
        {
            instance.setValue (Long.toString ((Long)object) + "L");
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
