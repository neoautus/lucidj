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

import org.lucidj.api.Serializer;
import org.lucidj.api.SerializerEngine;
import org.lucidj.api.SerializerInstance;
import org.lucidj.gluon.serializers.DefaultSerializers;
import org.lucidj.gluon.serializers.ListSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.lang.model.type.NullType;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.apache.felix.ipojo.annotations.Bind;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Unbind;
import org.apache.felix.ipojo.annotations.Validate;

@Component (immediate = true, publicFactory = false)
@Instantiate
@Provides
public class GluonSerializer implements SerializerEngine
{
    private final static transient Logger log = LoggerFactory.getLogger (GluonSerializer.class);

    private HashMap<Class, Serializer> serializer_lookup = new HashMap<> ();

    @Override
    public boolean register (Class clazz, Serializer serializer)
    {
        log.debug ("{} register: {} => {}", this, clazz, serializer);
        serializer_lookup.put (clazz, serializer);
        return (true);
    }

    private GluonInstance build_representation (Object obj)
    {
        GluonInstance instance = new GluonInstance ();
        Class type = (obj == null)? NullType.class: obj.getClass ();
        Serializer serializer = null;

        if (obj instanceof Serializer)
        {
            // Shortest way, the object knows how to serialize itself
            serializer = (Serializer)obj;
        }
        else if (serializer_lookup.containsKey (type))
        {
            // Easy way... the class have an explicit serializer
            serializer = serializer_lookup.get (type);
        }
        else
        {
            // Hard way... we need to find some compatible serializer
            for (Map.Entry<Class, Serializer> entry: serializer_lookup.entrySet ())
            {
                if (entry.getKey ().isAssignableFrom (type))
                {
                    serializer = entry.getValue ();
                    serializer_lookup.put (type, serializer);
                    break;
                }
            }
        }

        log.debug ("build_representation: instance={} obj={} serializer={}", instance, obj, serializer);

        if (serializer == null)
        {
            log.error ("Serializer not found for {}", type);
            return (null);
        }
        else if (!serializer.serializeObject (instance, obj))
        {
            log.error ("Serialization failed for {}", type);
            return (null);
        }
        return (instance);
    }

    @Override
    public boolean serializeObject (Writer writer, Object obj)
    {
        GluonInstance instance = build_representation (obj);

        // Build the object representation including all nested known objects
        if (instance != null)
        {
            // Our signature -------------------------------------------------------------------
            SerializerInstance se = instance.setProperty (SerializerEngine.SERIALIZATION_ENGINE,
                this.getClass ().getName ());
            se.setProperty ("version", "1.0");
            // ---------------------------------------------------------------------------------

            try
            {
                GluonWriter qwriter = new GluonWriter (writer);
                return (qwriter.writeRepresentation (instance));
            }
            catch (IOException e)
            {
                log.error ("Exception on serializeObject()", e);
            }
        }
        return (false);
    }

    @Override
    public Object deserializeObject (Reader reader)
    {
        return null;
    }

    @Bind (aggregate=true, optional=true, specification = Serializer.class)
    private void bindSerializer (Serializer serializer)
    {
        // The serializer itself must call register() to set the classes it handles
        log.info ("Adding serializer: {}", serializer);
    }

    private void clear_serializers_by_bundle (Bundle bnd)
    {
        Iterator<Map.Entry<Class, Serializer>> it = serializer_lookup.entrySet ().iterator ();

        while (it.hasNext ())
        {
            Map.Entry<Class, Serializer> entry = it.next ();

            if (FrameworkUtil.getBundle (entry.getValue ().getClass ()) == bnd)
            {
                log.info ("Removing serializer: {} for {}", entry.getValue (), entry.getKey ().getName ());
                it.remove ();
            }
        }
    }

    @Unbind
    private void unbindSerializer (Serializer serializer)
    {
        clear_serializers_by_bundle (FrameworkUtil.getBundle (serializer.getClass ()));
        log.info ("Removed serializer: {}", serializer);
    }

    @Validate
    private void validate ()
    {
//        register (byte[].class, ByteArraySerializer.class);
//        register (char[].class, CharArraySerializer.class);
//        register (short[].class, ShortArraySerializer.class);
//        register (int[].class, IntArraySerializer.class);
//        register (long[].class, LongArraySerializer.class);
//        register (float[].class, FloatArraySerializer.class);
//        register (double[].class, DoubleArraySerializer.class);
//        register (boolean[].class, BooleanArraySerializer.class);
//        register (String[].class, StringArraySerializer.class);
//        register (Object[].class, ObjectArraySerializer.class);
//        register (KryoSerializable.class, KryoSerializableSerializer.class);
//        register (BigInteger.class, BigIntegerSerializer.class);
//        register (BigDecimal.class, BigDecimalSerializer.class);
//        register (Class.class, ClassSerializer.class);
//        register (Date.class, DateSerializer.class);
//        register (Enum.class, EnumSerializer.class);
//        register (EnumSet.class, EnumSetSerializer.class);
//        register (Currency.class, CurrencySerializer.class);
//        register (StringBuffer.class, StringBufferSerializer.class);
//        register (StringBuilder.class, StringBuilderSerializer.class);
//        register (Collections.EMPTY_LIST.getClass(), CollectionsEmptyListSerializer.class);
//        register (Collections.EMPTY_MAP.getClass(), CollectionsEmptyMapSerializer.class);
//        register (Collections.EMPTY_SET.getClass(), CollectionsEmptySetSerializer.class);
//        register (Collections.singletonList(null).getClass(), CollectionsSingletonListSerializer.class);
//        register (Collections.singletonMap(null, null).getClass(), CollectionsSingletonMapSerializer.class);
//        register (Collections.singleton(null).getClass(), CollectionsSingletonSetSerializer.class);
//        register (TreeSet.class, TreeSetSerializer.class);
//        register (Collection.class, CollectionSerializer.class);
//        register (TreeMap.class, TreeMapSerializer.class);
//        register (Map.class, MapSerializer.class);
//        register (TimeZone.class, TimeZoneSerializer.class);
//        register (Calendar.class, CalendarSerializer.class);
//        register (Locale.class, LocaleSerializer.class);

        register (NullType.class, new DefaultSerializers.NullSerializer ());
        register (int.class, new DefaultSerializers.IntSerializer ());
        register (Integer.class, new DefaultSerializers.IntSerializer ());
        register (String.class, new DefaultSerializers.StringSerializer ());
        register (float.class, new DefaultSerializers.FloatSerializer ());
        register (boolean.class, new DefaultSerializers.BooleanSerializer ());
        register (Boolean.class, new DefaultSerializers.BooleanSerializer ());
        register (byte.class, new DefaultSerializers.ByteSerializer ());
        register (char.class, new DefaultSerializers.CharSerializer ());
        register (short.class, new DefaultSerializers.ShortSerializer ());
        register (long.class, new DefaultSerializers.LongSerializer ());
        register (double.class, new DefaultSerializers.DoubleSerializer ());

        register (List.class, new ListSerializer ());

        log.info ("ObjectSerializer started");
    }

    @Invalidate
    private void invalidate ()
    {
        log.info ("ObjectSerializer stopped");
    }

    public class GluonInstance implements SerializerInstance
    {
        private Map<String, GluonInstance> properties = null;
        private String string_value = null;
        private List<GluonInstance> object_values = null;

        public Map<String, GluonInstance> getProperties ()
        {
            return (properties);
        }

        public boolean hasProperties ()
        {
            return (properties != null);
        }

//        @Override
//        public String toString ()
//        {
//            return ("[value=" + string_value + " | objects=" + object_values + " | properties=" + properties + "]");
//        }

        @Override
        public SerializerInstance setObjectClass (Class clazz)
        {
            return (setProperty (GluonConstants.OBJECT_CLASS, clazz.getName ()));
        }

        public String getObjectClass ()
        {
            if (properties != null && properties.containsKey (GluonConstants.OBJECT_CLASS))
            {
                return (properties.get (GluonConstants.OBJECT_CLASS).getValue ());
            }
            return (null);
        }

        public boolean isPrimitive ()
        {
            return (string_value != null && getProperty (GluonConstants.OBJECT_CLASS) == null);
        }

        public String[] getPropertyKeys ()
        {
            return ((properties == null)? null: properties.keySet ().toArray (new String [0]));
        }

        public boolean containsKey (String key)
        {
            return (properties.containsKey (key));
        }

        @Override
        public SerializerInstance setProperty (String key, Object object)
        {
            if (properties == null)
            {
                properties = new HashMap<> ();
            }

            GluonInstance instance = build_representation (object);

            if (instance != null)
            {
                properties.put (key, instance);
            }
            return (instance);
        }

        public GluonInstance getProperty (String key)
        {
            return ((properties == null)? null: properties.get (key));
        }

        @Override
        public void setValue (String representation)
        {
            string_value = representation;
        }

        @Override
        public SerializerInstance addObject (Object object)
        {
            GluonInstance instance = build_representation (object);

            if (instance != null)
            {
                if (object_values == null)
                {
                    object_values = new ArrayList<> ();
                }

                object_values.add (instance);
            }
            return (instance);
        }

        public GluonInstance newInstance ()
        {
            return (new GluonInstance ());
        }

        public String getValue ()
        {
            return (string_value);
        }

        public List<GluonInstance> getObjects ()
        {
            return (object_values);
        }

        public boolean hasObjects ()
        {
            return (object_values != null);
        }
    }
}

// EOF
