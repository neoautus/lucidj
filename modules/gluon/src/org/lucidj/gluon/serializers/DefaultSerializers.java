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

import org.lucidj.api.Serializer;
import org.lucidj.api.SerializerInstance;

public class DefaultSerializers
{
    public static class NullSerializer implements Serializer
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
    }

    public static class IntSerializer implements Serializer
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
    }

    public static class StringSerializer implements Serializer
    {
        @Override
        public boolean serializeObject (SerializerInstance instance, Object object)
        {
            instance.setValue ("\"" + object + "\"");
            return (true);
        }

        @Override
        public Object deserializeObject (SerializerInstance instance)
        {
            return null;
        }
    }

    public static class FloatSerializer implements Serializer
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
    }

    public static class BooleanSerializer implements Serializer
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
    }

    public static class ByteSerializer implements Serializer
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
    }

    public static class CharSerializer implements Serializer
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
    }

    public static class ShortSerializer implements Serializer
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
    }

    public static class LongSerializer implements Serializer
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
    }

    public static class DoubleSerializer implements Serializer
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
    }

}

// EOF
