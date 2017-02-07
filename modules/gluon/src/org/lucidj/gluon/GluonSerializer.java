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

    public Map<String, Object> build_representation (SerializerInstance instance, Object obj)
    {
        Serializer serializer = null;
        Class type = (obj == null)? NullType.class: obj.getClass ();

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
                log.debug ("{} ==?== {}", entry.getKey(), type);
                if (entry.getKey ().isAssignableFrom (type))
                {
                    log.debug ("{} =====> {}", entry.getKey(), type);
                    serializer = entry.getValue ();
                    serializer_lookup.put (type, serializer);
                    log.debug ("{} cache: {} => {}", this, type, serializer);
                    break;
                }
            }
        }

        log.debug ("build_representation: instance={} obj={} serializer={}", instance, obj, serializer);

        // Null or the serialized object representation
        return ((serializer == null)? null: serializer.serializeObject (instance, obj));
    }

    @Override
    public boolean serializeObject (Writer writer, Object obj)
    {
        SerializerInstance instance = new GluonInstance ();
        Map<String, Object> map_representation;

        // Build the object representation including all nested known objects
        if ((map_representation = build_representation (instance, obj)) == null)
        {
            return (false);
        }

        try
        {
            GluonWriter qwriter = new GluonWriter (writer);
            return (qwriter.writeRepresentation (map_representation));
        }
        catch (IOException e)
        {
            log.error ("Exception on serializeObject()", e);
            return (false);
        }
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
        register (String.class, new DefaultSerializers.StringSerializer ());
        register (float.class, new DefaultSerializers.FloatSerializer ());
        register (boolean.class, new DefaultSerializers.BooleanSerializer ());
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
        @Override
        public Map<String, Object> serializeObject (Object to_serialize)
        {
            return (build_representation (this, to_serialize));
        }

        @Override
        public Object deserializeObject (Map<String, Object> properties)
        {
            return (null);
        }
    }
}

// EOF
