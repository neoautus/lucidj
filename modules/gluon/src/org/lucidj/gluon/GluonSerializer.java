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

    private GluonInstance build_representation_tree (Object obj)
    {
        GluonInstance instance = new GluonInstance ();
        Class type = (obj == null)? NullType.class: obj.getClass ();
        String type_name = type.getName ();
        Serializer serializer = null;

        instance.setBackingObject (obj);

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

        log.debug ("build_representation_tree: instance={} obj={} serializer={}", instance, obj, serializer);

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
        GluonInstance instance = build_representation_tree (obj);

        // Build the object representation including all nested known objects
        if (instance != null)
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

    private boolean resolve_object (GluonInstance object)
    {
        String object_class = (String)object.getPropertyObject (GluonConstants.OBJECT_CLASS);
        Serializer serializer = serializer_lookup.get (object_class);

        log.info ("{} ====> {} serializer={}", object, object_class, serializer);

        Object obj = serializer.deserializeObject (object);

        log.info ("DESERIALIZED OBJECT: {}", obj);
        object.setBackingObject (obj);

        return (true);
    }

    private boolean build_object_tree (GluonInstance instance)
    {
        HashMap<Integer, GluonInstance> embedded_objects = new HashMap<> ();
        List<GluonInstance> object_list = instance.getEmbeddedObjects ();

        // TODO: ITERATIVE RESOLUTION
        for (GluonInstance object: object_list)
        {
            if (object.getBackingObject () == null)
            {
                resolve_object (object);
            }
        }

        // Return root object resolution
        return (resolve_object (instance));
    }

    private Object deserialize_primitive (GluonInstance primitive)
        throws IllegalStateException
    {
        String representation = primitive.getValue ();

        // We need to walk the deserializers trying to match the representation
        for (Map.Entry<String, Serializer> entry: serializer_lookup.entrySet ())
        {
            if (entry.getValue () instanceof GluonPrimitive)
            {
                GluonPrimitive deserializer = (GluonPrimitive)entry.getValue ();

                if (deserializer.match (representation))
                {
                    Object obj = deserializer.deserializeObject (primitive);

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

    private boolean deserialize_primitives (GluonInstance instance)
    {
        // It's good to be optimistic, however nullius in verba :)
        boolean success = true;

        // Walk all properties and attributes
        String[] property_keys = instance.getPropertyKeys ();

        if (property_keys != null)
        {
            for (String key: property_keys)
            {
                GluonInstance property = instance.getProperty (key);

                try
                {
                    property.setBackingObject (deserialize_primitive (property));
                }
                catch (IllegalStateException e)
                {
                    log.error ("Error deserializing {}: {}", key, e.getMessage ());
                    success = false;
                }

                // Walk into any attributes
                success &= deserialize_primitives (property);
            }
        }

        // Walk all embeded objects
        if (instance.getEmbeddedObjects () != null)
        {
            for (GluonInstance obj: instance.getEmbeddedObjects ())
            {
                success &= deserialize_primitives (obj);
            }
        }

        return (success);
    }

    @Override
    public Object deserializeObject (Reader reader)
    {
        GluonInstance instance = new GluonInstance ();

        try
        {
            GluonReader greader = new GluonReader (reader);

            if (!greader.readRepresentation (instance))
            {
                return (null);
            }

            deserialize_primitives (instance);

            GluonUtil.dumpRepresentation (instance, "deserialize_dump.txt");

            build_object_tree (instance);

            GluonUtil.dumpRepresentation (instance, "objectree_dump.txt");
        }
        catch (IOException e)
        {
            log.error ("Exception reading {}", reader, e);
            return (null);
        }
        return (instance.getBackingObject ());
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

    public class GluonInstance implements SerializerInstance
    {
        private Map<String, GluonInstance> properties = null;
        private Object backing_object = null;
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
                return ((String)properties.get (GluonConstants.OBJECT_CLASS).getBackingObject ());
            }
            return (null);
        }

        public boolean isPrimitive ()
        {
            return (getProperty (GluonConstants.OBJECT_CLASS) == null);
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

            GluonInstance instance = build_representation_tree (object);

            if (instance != null)
            {
                properties.put (key, instance);
            }
            return (instance);
        }

        public void renameProperty (String old_name, String new_name)
        {
            GluonInstance property = properties.get (old_name);
            properties.remove (old_name);
            properties.put (new_name, property);
        }

        public GluonInstance getProperty (String key)
        {
            return ((properties == null)? null: properties.get (key));
        }

        public Object getPropertyObject (String key)
        {
            if (properties == null)
            {
                return (null);
            }

            GluonInstance property = properties.get (key);
            return ((property == null)? null: property.getBackingObject ());
        }

        @Override
        public void setValue (String representation)
        {
            string_value = representation;
        }

        public void setBackingObject (Object value)
        {
            backing_object = value;
        }

        @Override
        public SerializerInstance addObject (Object object)
        {
            GluonInstance instance = build_representation_tree (object);

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

        @Override
        public Object[] getObjects ()
        {
            List<Object> objects = new ArrayList<> ();

            for (GluonInstance object_instance: getEmbeddedObjects ())
            {
                if (object_instance.getBackingObject () != null
                    && object_instance.getProperty (GluonConstants.OBJECT_CLASS) != null
                    && object_instance.getProperty (GluonConstants.OBJECT_CLASS).getProperty ("embedded") == null)
                {
                    objects.add (object_instance.getBackingObject ());
                }
            }
            return (objects.toArray (new Object[0]));
        }

        public GluonInstance newInstance ()
        {
            return (new GluonInstance ());
        }

        public String getValue ()
        {
            return (string_value);
        }

        public Object getBackingObject ()
        {
            return (backing_object);
        }

        public List<GluonInstance> getEmbeddedObjects ()
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
