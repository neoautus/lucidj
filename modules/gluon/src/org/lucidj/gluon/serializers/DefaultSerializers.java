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
            instance.setValue (Integer.toString ((Integer)object));
            return (true);
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
            instance.setValue ("\"" + StringEscapeUtils.escapeJava ((String)object) + "\"");
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
            instance.setValue ((Boolean)object? "true": "false");
            return (true);
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

}

// EOF
