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

package org.rationalq.quark;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.factories.SerializerFactory;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.FieldSerializer;
import com.google.common.io.BaseEncoding;
import com.vaadin.ui.Component;
import org.objenesis.instantiator.ObjectInstantiator;
import org.objenesis.strategy.InstantiatorStrategy;
import org.objenesis.strategy.StdInstantiatorStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Field;

public class KryoSerializer
{
    private final transient Logger log = LoggerFactory.getLogger (KryoSerializer.class);

    private final Kryo kryo;

    // http://stackoverflow.com/questions/12485351/java-reflection-field-value-in-extends-class
    private Field findUnderlying(Class<?> clazz, String fieldName)
    {
        Class<?> current = clazz;
        do
        {
            try
            {
                return (current.getDeclaredField(fieldName));
            }
            catch(Exception ignore) {};
        }
        while((current = current.getSuperclass()) != null);

        return (null);
    }

    private boolean set_field (Object target, String field_name, Object field_value)
    {
        try
        {
            Field field = findUnderlying(target.getClass(), field_name);

            if (field != null)
            {
                field.setAccessible(true);
                field.set(target, field_value);
            }
            return (true);
        }
        catch (Exception e)
        {
            log.info("set_field: Exception {}", e);
        };
        return (false);
    }

    class VaadinFieldSerializer<T> extends FieldSerializer<T>
    {
        public VaadinFieldSerializer (Kryo kryo, Class type)
        {
            super(kryo, type);
        }

        public void write (Kryo kryo, Output output, T object)
        {
            if ("__KRYO_BARRIER__".equals (((Component)object).getId ()))
            {
                return;
            }

            super.write (kryo, output, object);
        }

        public T read (Kryo kryo, Input input, Class<T> type)
        {
            T object = super.read(kryo, input, type);

            log.info("<<<VAADIN>> {}", object.getClass().getCanonicalName());

            if (Component.class.isInstance(object))
            {
                log.info("<<<FIX>>> {}", object.getClass().getCanonicalName());

                // Fix connectorId on all Vaadin components
                set_field(object, "connectorId", null);
            }

            return (object);
        }
    }

    public KryoSerializer()
    {
        kryo = new Kryo();

        kryo.setDefaultSerializer(new SerializerFactory()
        {
            @Override
            public com.esotericsoftware.kryo.Serializer makeSerializer(Kryo kryo, Class<?> type)
            {
                String canonical_name = (type.getCanonicalName() != null)? type.getCanonicalName(): "(null)";

                log.info("makeSerializer: IN " + type + " canonical=" + canonical_name);

                Serializer serializer = null;

                if (Component.class.isAssignableFrom(type))
                {
                    serializer = new VaadinFieldSerializer(kryo, type);
                }
                else switch (canonical_name)
                {
                    case "org.apache.felix.ipojo.InstanceManager":
                    case "org.vaadin.peter.contextmenu.ContextMenu":
                    {
                        log.info("makeSerializer: Use empty serializer");
                        serializer = new Serializer()
                        {
                            @Override
                            public Object read(Kryo kryo, Input input, Class type)
                            {
                                return (null);
                            }

                            @Override
                            public void write(Kryo kryo, Output output, Object object)
                            {
                                // nothing
                            }
                        };
                        break;
                    }
                    case "com.vaadin.ui.Alignment":
                    {
                        log.info("makeSerializer: case Alignment");
                        serializer = new Serializer()
                        {
                            @Override
                            public Object read(Kryo kryo, Input input, Class type)
                            {
                                try
                                {
                                    return (new com.vaadin.ui.Alignment(0));
                                }
                                catch (Exception e)
                                {
                                    log.info("makeSerializer: " + type + ": " + e.toString());
                                }
                                return (null);
                            }

                            @Override
                            public void write(Kryo kryo, Output output, Object object)
                            {
                                // nothing
                            }
                        };
                        break;
                    }
                    case "(null)":
                    default:
                    {
                        log.info("makeSerializer: default");
                        FieldSerializer field_serializer = new FieldSerializer(kryo, type);
                        // Anonymous classes hack. Check how secure is this.
                        field_serializer.setIgnoreSyntheticFields(false);
                        serializer = field_serializer;
                        break;
                    }
                }

                log.info("makeSerializer: OUT serializer = {}", serializer);

                return (serializer);
            }
        });

        kryo.setInstantiatorStrategy(new InstantiatorStrategy ()
        {
            @SuppressWarnings("unchecked")
            @Override
            public <T> ObjectInstantiator<T> newInstantiatorOf(Class<T> type)
            {
                String canonical_name = (type.getCanonicalName() != null)? type.getCanonicalName(): "(null)";

                switch (canonical_name)
                {
                    case "java.util.ArrayList":
                    case "java.util.HashSet":
                    case "java.util.HashMap":
                    {
                        return (new Kryo.DefaultInstantiatorStrategy().newInstantiatorOf(type));
                    }
                }

                return (new StdInstantiatorStrategy().newInstantiatorOf(type));
            }
        });
    }

    public void setClassLoader (ClassLoader cld)
    {
        kryo.setClassLoader(cld);
    }

    public void addDefaultSerializer(Class type, Class <? extends Serializer> serializerClass)
    {
        if (kryo != null)
        {
            kryo.addDefaultSerializer(type, serializerClass);
        }
    }

    public byte[] serialize(Object object)
    {
        log.info("serialize: {}", object);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Output output = new Output(baos);
        kryo.writeClassAndObject(output, object);
        output.flush();
        output.close();

        byte[] serialized_obj = baos.toByteArray();

        log.debug ("serialized: {}", BaseEncoding.base64().encode(serialized_obj));

        return (serialized_obj);
    }

    public Object deserialize(byte[] serialized_obj)
    {
        Object object = null;

        log.debug ("deserialize: {}", BaseEncoding.base64().encode(serialized_obj));

        try
        {
            Input input = new Input(new ByteArrayInputStream(serialized_obj));
            object = kryo.readClassAndObject(input);
            input.close();
        }
        catch (Exception e)
        {
            log.error ("deserialize error: {}", e);
        }

        log.info("deserialized: {}", object);

        return (object);
    }
}

// EOF
