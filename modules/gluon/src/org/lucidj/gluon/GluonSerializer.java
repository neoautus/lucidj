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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.lang.model.type.NullType;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

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

    private Map<String, Serializer> serializer_lookup = new ConcurrentHashMap<> ();

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

        if (!(serializer instanceof GluonPrimitive)
            && obj != null
            && !obj.getClass ().isArray ()) // We also treat arrays as primitives
        {
            // Preset object type
            instance.setObjectClass (type_name);
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
                new GluonObject (this.getClass ()));
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

    @Override
    public boolean serializeObject (Path path, Object obj)
    {
        Charset cs = Charset.forName ("UTF-8");
        Writer writer = null;

        try
        {
            writer = Files.newBufferedWriter (path, cs);
            return (serializeObject (writer, obj));
        }
        catch (Exception e)
        {
            log.error ("Exception on serialization", e);
            return (false);
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

    private Object deserialize_object (GluonInstance instance)
    {
        GluonObject object_ref = (GluonObject)instance._getProperty (GluonConstants.OBJECT_CLASS);
        String type_name = object_ref.getClassName ();
        Serializer serializer = null;

        if (serializer_lookup.containsKey (type_name))
        {
            // Easy way... the class have an explicit serializer
            serializer = serializer_lookup.get (type_name);
        }
        else
        {
            // Hard way... we need to find some compatible serializer
            for (Map.Entry<String, Serializer> entry: serializer_lookup.entrySet ())
            {
                if (entry.getValue () instanceof GluonPrimitive)
                {
                    // Skip all primitives
                    continue;
                }

                Class type = classManager.loadClassUsingObject (entry.getValue (), type_name);
                Class entry_type = classManager.loadClassUsingObject (entry.getValue (), entry.getKey ());

                if (type != null
                    && entry_type != null
                    && entry_type.isAssignableFrom (type))
                {
                    serializer = entry.getValue ();
                    serializer_lookup.put (type_name, serializer);
                    break;
                }
            }
        }

        if (serializer == null)
        {
            throw (new IllegalStateException ("No matching serializer for '" + type_name + "'"));
        }
        return (serializer.deserializeObject (instance));
    }

    public boolean applyDeserializer (GluonInstance instance, String representation)
        throws IllegalStateException
    {
        try
        {
            if (representation == null) // An object
            {
                instance._setValueObject (deserialize_object (instance));
            }
            else // A primitive
            {
                instance._setValueObject (deserialize_primitive (instance, representation));
            }
            return (true);
        }
        catch (IllegalStateException e)
        {
            log.error ("Exception handling object deserialization", e);
            return (false);
        }
    }

    @Override
    public Map<String, Object> getProperties (Reader reader)
    {
        GluonInstance instance = new GluonInstance (this);

        try
        {
            GluonReader greader = new GluonReader (reader);

            if (!greader.readRepresentation (instance))
            {
                return (null);
            }
        }
        catch (IOException e)
        {
            log.error ("Exception reading {}", reader, e);
        }

        GluonUtil.dumpRepresentation (instance, "deserialize_properties.txt");

        Map<String, Object> properties = new HashMap<> ();

        for (String key: instance.getPropertyKeys ())
        {
            properties.put (key, instance.getProperty (key));
        }

        // List of all direct references to embedded object types
        Set<String> embedded_types = new HashSet<> ();

        // Always add the supertype as embedded object
        GluonObject object_ref = (GluonObject)instance._getProperty (GluonConstants.OBJECT_CLASS);
        embedded_types.add (object_ref.getClassName ());

        // Add all embedded object types
        for (GluonInstance entry: instance.getObjectEntries ())
        {
            if (instance.containsKey (GluonConstants.OBJECT_CLASS))
            {
                object_ref = (GluonObject)entry._getProperty (GluonConstants.OBJECT_CLASS);
                embedded_types.add (object_ref.getClassName ());
            }
        }

        // Store the embedded types extracted from serialization info
        properties.put (SerializerEngine.EMBEDDED_TYPES,
            embedded_types.toArray (new String [embedded_types.size ()]));
        return (properties);
    }

    @Override
    public Map<String, Object> getProperties (Path path)
    {
        Charset cs = Charset.forName ("UTF-8");
        Reader reader = null;

        try
        {
            reader = Files.newBufferedReader (path, cs);
            return (getProperties (reader));
        }
        catch (Exception e)
        {
            log.info ("Exception on deserialization", e);
            return (null);
        }
        finally
        {
            try
            {
                if (reader != null)
                {
                    reader.close();
                }
            }
            catch (Exception ignore) {};
        }
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

            GluonUtil.dumpRepresentation (instance, "deserialize_read.txt");
        }
        catch (IOException e)
        {
            log.error ("Exception reading {}", reader, e);
            return (null);
        }
        log.info ("-----> deserializeObject::_resolveObject()");
        Object obj = instance._resolveObject ();
        log.info ("-----> deserializeObject::_resolveObject() => {}", obj);
        GluonUtil.dumpRepresentation (instance, "deserialize_resolved.txt");
        return (obj);
    }

    @Override
    public Object deserializeObject (Path path)
    {
        Charset cs = Charset.forName ("UTF-8");
        Reader reader = null;

        try
        {
            reader = Files.newBufferedReader (path, cs);
            return (deserializeObject (reader));
        }
        catch (Exception e)
        {
            log.info ("Exception on deserialization", e);
            return (null);
        }
        finally
        {
            try
            {
                if (reader != null)
                {
                    reader.close();
                }
            }
            catch (Exception ignore) {};
        }
    }

    //------------------------------------------------------------------------------------------------------
    // Serializers registry
    //------------------------------------------------------------------------------------------------------

    private boolean _register (String type, Serializer serializer)
    {
        log.debug ("{} register: {} => {}", this, type, serializer);
        serializer_lookup.put (type, serializer);
        return (true);
    }

    private boolean _register (Class type, Serializer serializer)
    {
        return (_register (type.getName (), serializer));
    }

    @Override
    public boolean register (String type, Serializer serializer)
    {

        Bundle service_bundle = FrameworkUtil.getBundle (serializer.getClass ());
        BundleContext service_context = service_bundle.getBundleContext ();
        Dictionary<String, Object> props = new Hashtable<> ();
        props.put ("@type", type);
        service_context.registerService (Serializer.class.getName (), serializer, props);
        return (_register (type, serializer));
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

        _register (GluonObject.class, new GluonObjectSerializer ());
        _register (NullType.class, new DefaultSerializers.NullSerializer ());
        _register (int.class, new DefaultSerializers.IntSerializer ());
        _register (Integer.class, new DefaultSerializers.IntSerializer ());
        _register (String.class, new DefaultSerializers.StringSerializer ());
        _register (float.class, new DefaultSerializers.FloatSerializer ());
        _register (Float.class, new DefaultSerializers.FloatSerializer ());
        _register (boolean.class, new DefaultSerializers.BooleanSerializer ());
        _register (Boolean.class, new DefaultSerializers.BooleanSerializer ());
        _register (byte.class, new DefaultSerializers.ByteSerializer ());
        _register (char.class, new DefaultSerializers.CharSerializer ());
        _register (short.class, new DefaultSerializers.ShortSerializer ());
        _register (long.class, new DefaultSerializers.LongSerializer ());
        _register (Long.class, new DefaultSerializers.LongSerializer ());
        _register (double.class, new DefaultSerializers.DoubleSerializer ());
        _register (Double.class, new DefaultSerializers.DoubleSerializer ());
        _register (Object[].class, new DefaultArraySerializers.ObjectArraySerializer ());
        log.info ("ObjectSerializer started");
    }

    @Invalidate
    private void invalidate ()
    {
        log.info ("ObjectSerializer stopped");
    }
}

// EOF
