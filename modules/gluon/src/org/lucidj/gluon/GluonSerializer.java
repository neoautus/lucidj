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

import org.lucidj.api.ClassManager;
import org.lucidj.api.Serializer;
import org.lucidj.api.SerializerEngine;
import org.lucidj.api.SerializerInstance;
import org.lucidj.gluon.serializers.DefaultArraySerializers;
import org.lucidj.gluon.serializers.DefaultSerializers;
import org.lucidj.gluon.serializers.GluonObjectSerializer;
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
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.apache.felix.ipojo.annotations.Bind;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Context;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Unbind;
import org.apache.felix.ipojo.annotations.Validate;

@Component (immediate = true, publicFactory = false)
@Instantiate
@Provides
public class GluonSerializer implements SerializerEngine
{
    private final static transient Logger log = LoggerFactory.getLogger (GluonSerializer.class);

    private HashMap<String, Serializer> serializer_lookup = new HashMap<> ();

    @Context
    private BundleContext context;

    @Requires
    ClassManager classManager;

    //------------------------------------------------------------------------------------------------------
    // Serialization
    //------------------------------------------------------------------------------------------------------

    public boolean applySerializer (GluonInstance instance, Object obj)
    {
        Class type = (obj == null)? NullType.class: obj.getClass ();
        String type_name = type.getName ();
        Serializer serializer = null;

        instance._setValueObject (obj);

        if (obj instanceof Serializer)
        {
            // Shortest way, the object knows how to serialize itself
            serializer = (Serializer)obj;
        }
        else if (serializer_lookup.containsKey (type_name))
        {
            // Easy way... the class have an explicit serializer
            serializer = serializer_lookup.get (type_name);
        }
        else
        {
            // Hard way... we need to find some compatible serializer
            for (Map.Entry<String, Serializer> entry: serializer_lookup.entrySet ())
            {
                Class entry_type = classManager.loadClassUsingObject (entry.getValue (), entry.getKey ());

                if (entry_type != null && entry_type.isAssignableFrom (type))
                {
                    serializer = entry.getValue ();
                    serializer_lookup.put (type_name, serializer);
                    break;
                }
            }
        }

        log.debug ("applySerializer: instance={} obj={} serializer={}", instance, obj, serializer);

        if (serializer == null)
        {
            log.error ("Serializer not found for {}", type);
            return (false);
        }
        return (serializer.serializeObject (instance, obj));
    }

    @Override
    public boolean serializeObject (Writer writer, Object obj)
    {
        GluonInstance instance = new GluonInstance (this);

        // Build the object representation including all nested known objects
        if (applySerializer (instance, obj))
        {
            // Our signature -------------------------------------------------------------------
            SerializerInstance se = instance.setProperty (GluonConstants.SERIALIZATION_ENGINE,
                this.getClass ().getName ());
            se.setProperty ("version", "1.0");
            // ---------------------------------------------------------------------------------

            GluonUtil.dumpRepresentation (instance, "serialize_dump.txt");

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

    //------------------------------------------------------------------------------------------------------
    // Deserialization
    //------------------------------------------------------------------------------------------------------

    private Object deserialize_primitive (GluonInstance instance, String representation)
        throws IllegalStateException
    {
        instance.setValue (representation);

        // We need to walk the deserializers trying to match the representation
        for (Map.Entry<String, Serializer> entry: serializer_lookup.entrySet ())
        {
            if (entry.getValue () instanceof GluonPrimitive)
            {
                GluonPrimitive deserializer = (GluonPrimitive)entry.getValue ();

                if (deserializer.match (representation))
                {
                    Object obj = deserializer.deserializeObject (instance);

                    // Even with a match, eventually the representation might fail and yield null
                    if (obj == null)
                    {
                        String message =
                            deserializer.getClass ().getName () +   // The deserializer
                            " failed to deserialize " +             // The problem
                            "'" + representation + "'";             // The culprit
                        throw (new IllegalStateException (message));
                    }
                    return (obj);
                }
            }
        }
        throw (new IllegalStateException ("No matching serializer for '" + representation + "'"));
    }

    public boolean applyDeserializer (GluonInstance instance, String representation)
        throws IllegalStateException
    {
        try
        {
            instance._setValueObject (deserialize_primitive (instance, representation));
            return (true);
        }
        catch (IllegalStateException e)
        {
            log.error ("Exception handling object representation: {}", representation, e);
            return (false);
        }
    }

    private boolean resolve_object (GluonInstance object)
    {
//        boolean object_resolved = true;
//
//        for (String key: object.getPropertyKeys ())
//        {
//            if (object.getAttribute (key, "embedded") == true)
//            {
//
//            }
//        }

        String object_class = (String)object.getProperty (GluonConstants.OBJECT_CLASS);
        Serializer serializer = serializer_lookup.get (object_class);

        log.info ("{} ====> {} serializer={}", object, object_class, serializer);

        Object obj = serializer.deserializeObject (object);

        log.info ("DESERIALIZED OBJECT: {}", obj);
        object._setValueObject (obj);

        return (true);
    }

    private boolean build_object_tree (GluonInstance instance)
    {
        HashMap<Integer, GluonInstance> embedded_objects = new HashMap<> ();
        List<GluonInstance> object_list = instance.getObjectEntries ();

        // TODO: ITERATIVE RESOLUTION
        for (GluonInstance object: object_list)
        {
            if (object._getValueObject () == null)
            {
                resolve_object (object);
            }
        }

        // Return root object resolution
        return (resolve_object (instance));
    }

    @Override
    public Object deserializeObject (Reader reader)
    {
        // TODO: HANDLE NULL HERE?
        GluonInstance instance = new GluonInstance (this);

        try
        {
            GluonReader greader = new GluonReader (reader);

            if (!greader.readRepresentation (instance))
            {
                return (null);
            }

            GluonUtil.dumpRepresentation (instance, "deserialize_dump.txt");

            build_object_tree (instance);

            GluonUtil.dumpRepresentation (instance, "objectree_dump.txt");
        }
        catch (IOException e)
        {
            log.error ("Exception reading {}", reader, e);
            return (null);
        }
        return (instance._getValueObject ());
    }

    //------------------------------------------------------------------------------------------------------
    // Serializers registry
    //------------------------------------------------------------------------------------------------------

    @Override
    public boolean register (String type, Serializer serializer)
    {
        log.debug ("{} register: {} => {}", this, type, serializer);
        serializer_lookup.put (type, serializer);
        return (true);
    }

    @Override
    public boolean register (Class type, Serializer serializer)
    {
        return (register (type.getName (), serializer));
    }

    @Bind (aggregate=true, optional=true, specification = Serializer.class)
    private void bindSerializer (Serializer serializer)
    {
        // The serializer itself must call register() to set the classes it handles
        log.info ("Adding serializer: {}", serializer);
    }

    private void clear_serializers_by_bundle (Bundle bnd)
    {
        Iterator<Map.Entry<String, Serializer>> it = serializer_lookup.entrySet ().iterator ();

        while (it.hasNext ())
        {
            Map.Entry<String, Serializer> entry = it.next ();

            if (FrameworkUtil.getBundle (entry.getValue ().getClass ()) == bnd)
            {
                log.info ("Removing serializer: {} for {}", entry.getValue (), entry.getKey ());
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

        register (GluonObject.class, new GluonObjectSerializer ());
        register (NullType.class, new DefaultSerializers.NullSerializer ());
        register (int.class, new DefaultSerializers.IntSerializer ());
        register (Integer.class, new DefaultSerializers.IntSerializer ());
        register (String.class, new DefaultSerializers.StringSerializer ());
        register (float.class, new DefaultSerializers.FloatSerializer ());
        register (Float.class, new DefaultSerializers.FloatSerializer ());
        register (boolean.class, new DefaultSerializers.BooleanSerializer ());
        register (Boolean.class, new DefaultSerializers.BooleanSerializer ());
        register (byte.class, new DefaultSerializers.ByteSerializer ());
        register (char.class, new DefaultSerializers.CharSerializer ());
        register (short.class, new DefaultSerializers.ShortSerializer ());
        register (long.class, new DefaultSerializers.LongSerializer ());
        register (Long.class, new DefaultSerializers.LongSerializer ());
        register (double.class, new DefaultSerializers.DoubleSerializer ());
        register (Double.class, new DefaultSerializers.DoubleSerializer ());

        register (Object[].class, new DefaultArraySerializers.ObjectArraySerializer ());
        register (List.class, new ListSerializer ());

        log.info ("ObjectSerializer started");
    }

    @Invalidate
    private void invalidate ()
    {
        log.info ("ObjectSerializer stopped");
    }
}

// EOF
