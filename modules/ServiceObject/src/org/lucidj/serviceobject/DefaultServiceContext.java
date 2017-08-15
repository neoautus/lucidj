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

package org.lucidj.serviceobject;

import org.lucidj.api.ServiceContext;
import org.lucidj.api.ServiceLocator;
import org.lucidj.api.ServiceObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.FrameworkUtil;
import org.osgi.util.tracker.BundleTracker;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Context;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Validate;

@Component (immediate = true, publicFactory = false)
@Instantiate
@Provides
public class DefaultServiceContext implements ServiceContext
{
    private final static Logger log = LoggerFactory.getLogger (DefaultServiceContext.class);

    private BundleTracker bundle_cleaner;

    private Map<Bundle, Set<WeakReference<Object>>> instance_set_cache = new HashMap<> ();
    private Map<String, Long> service_classes_to_bundleid = new HashMap<> ();
    private Map<String, ServiceObject.Provider> service_object_providers = new HashMap<> ();
    private Map<Long, Map<String, Object>> properties_by_bundleid = new HashMap<> ();
    private Set<ServiceObject.Listener> listener_list = new HashSet<> ();

    @Context
    private BundleContext context;

    @Override // ServiceContext
    public ServiceLocator newServiceLocator ()
    {
        log.info ("newServiceLocator context={}", context);
        ServiceLocatorWrapper wrapper = new ServiceLocatorWrapper (context);
        log.info ("newServiceLocator wrapper={}", wrapper);
        return (wrapper);
    }

    private void call_annotated (Class annotation, Object obj, Object... args)
    {
        for (Method m: obj.getClass ().getDeclaredMethods ())
        {
            if (m.isAnnotationPresent (annotation))
            {
                try
                {
                    m.setAccessible (true);
                    m.invoke (obj, args);
                }
                catch (InvocationTargetException | IllegalAccessException e)
                {
                    log.error ("Exception injecting ServiceContext on {}", obj, e);
                }
                catch (IllegalArgumentException e)
                {
                    log.error ("Illegal arguments calling @{} on {}: {}", annotation.getSimpleName (), obj, e.getMessage ());
                }
                catch (Throwable e)
                {
                    log.error ("Unhandled exception from Validate on {}", obj, e);
                }
                return;
            }
        }
    }

    private void broadcast_event (int type, Object service_object)
    {
        for (ServiceObject.Listener listener: listener_list)
        {
            listener.event (type, service_object);
        }
    }

    private synchronized Set<WeakReference<Object>> get_instance_set (Bundle bnd, boolean create)
    {
        Set<WeakReference<Object>> instance_set = instance_set_cache.get (bnd);

        if (instance_set == null && create)
        {
            instance_set = new HashSet<> ();
            instance_set_cache.put (bnd, instance_set);
        }
        return (instance_set);
    }

    private void store_service_object_ref (Object obj)
    {
        if (obj == null)
        {
            log.error ("Attempting to register null object");
            return;
        }

        Bundle obj_bundle = FrameworkUtil.getBundle (obj.getClass ());

        if (obj_bundle == null)
        {
            log.warn ("Attempting to register stray object: {}", obj);
            return;
        }

        Set<WeakReference<Object>> instance_set = get_instance_set (obj_bundle, true);

        if (instance_set == null)
        {
            log.error ("Object registration failed: {} from {}", obj, obj_bundle);
            return;
        }
        instance_set.add (new WeakReference<> (obj));
    }

    private Object internal_wrap_object (Object serviceObject)
    {
        //---------------------------
        // INIT ALL ANNOTATED FIELDS
        //---------------------------
        for (Field f: serviceObject.getClass ().getDeclaredFields ())
        {
            if (f.isAnnotationPresent (ServiceObject.Context.class))
            {
                try
                {
                    f.setAccessible (true);
                    f.set (serviceObject, this);
                }
                catch (IllegalAccessException e)
                {
                    log.error ("Exception injecting ServiceContext on {}", serviceObject, e);
                }
            }
        }

        //------------------------------
        // REGISTER THE OBJECT INSTANCE
        //------------------------------
        store_service_object_ref (serviceObject);

        //---------------------------------------------------------------
        // CALL ANNOTATED OBJECT METHODS, THEN NOTIFY UPSTREAM LISTENERS
        //---------------------------------------------------------------
        call_annotated (ServiceObject.Validate.class, serviceObject);
        broadcast_event (ServiceObject.VALIDATE, serviceObject);

        // We return the same object, however it could have changed
        return (serviceObject);
    }

    @Override // ServiceContext
    public <T> T wrapObject (Class<T> serviceClass, Object serviceObject)
    {
        log.info ("wrapObject: obj={}", serviceObject);
        return (serviceClass.cast (internal_wrap_object (serviceObject)));
    }

    @Override // ServiceContext
    public void register (Class objectClass)
    {
        long source_bundle_id = FrameworkUtil.getBundle (objectClass).getBundleId ();
        service_classes_to_bundleid.put (objectClass.getName (), source_bundle_id);
    }

    @Override
    public void register (Class objectClass, ServiceObject.Provider provider)
    {
        service_object_providers.put (objectClass.getName (), provider);
    }

    @Override // ServiceContext
    public void addListener (ServiceObject.Listener listener, Class filterClass)
    {
        // TODO: ADD FILTER
        listener_list.add (listener);
    }

    static Class[] VALID_PARAMETERS = new Class[]
    {
        ServiceContext.class,
        BundleContext.class,
        Map.class
    };

    private Constructor find_constructor (Class cls)
    {
        Constructor[] constructors = cls.getConstructors ();
        Constructor best_match = null;
        int best_match_num_parms = 0;

        for (Constructor c: constructors)
        {
            int num_matches = 0;

            // Verify if every parameter on the constructor is compatible
            for (Class param_class: c.getParameterTypes ())
            {
                boolean match_found = false;

                for (int i = 0; i < VALID_PARAMETERS.length; i++)
                {
                    if (param_class.isAssignableFrom (VALID_PARAMETERS [i]))
                    {
                        match_found = true;
                        break;
                    }
                }

                if (match_found)
                {
                    num_matches++;
                }
                else
                {
                    // We found a incompatible class, this is a wrong match
                    num_matches = 0;
                    break;
                }
            }

            // Assign only if we have more parameters matching for this constructor
            if (num_matches > best_match_num_parms)
            {
                best_match = c;
                best_match_num_parms = num_matches;
            }
        }
        // TODO: CACHE THIS?
        return (best_match);
    }

    private Object create_using_reflection (String objectClassName, Map<String, Object> properties)
    {
        if (!service_classes_to_bundleid.containsKey (objectClassName))
        {
            log.error ("ServiceObject {} not found", objectClassName);
            return (null);
        }

        long bundleid = service_classes_to_bundleid.get (objectClassName);
        Bundle source_bundle = context.getBundle (bundleid);
        Class serviceClass;

        try
        {
            serviceClass = source_bundle.loadClass (objectClassName);
        }
        catch (ClassNotFoundException e)
        {
            log.error ("Error loading {} from {}", objectClassName, source_bundle);
            return (null);
        }

        Object new_object = null;

        try
        {
            Constructor constructor;

            if ((constructor = find_constructor (serviceClass)) != null)
            {
                Class[] arg_types = constructor.getParameterTypes ();
                Object[] arg_list = new Object [arg_types.length];

                log.info ("FOUND constructor = {} / {}", constructor, arg_types);

                // Fill in all the arguments regardless of ordering
                for (int i = 0; i < arg_list.length; i++)
                {
                    if (arg_types [i].equals (ServiceContext.class))
                    {
                        arg_list [i] = this;
                    }
                    else if (arg_types [i].equals (BundleContext.class))
                    {
                        arg_list [i] = source_bundle.getBundleContext ();
                    }
                    else if (arg_types [i].equals (Map.class))
                    {
                        arg_list [i] = properties;
                    }
                    else
                    {
                        // Will stay null, but issue a warning anyway
                        log.warn ("Unknown type inside constructor {}: {}",
                                objectClassName, arg_types [i].getName ());
                    }
                }
                log.info ("NEW INSTANCE: {} / {}", constructor, arg_list);
                new_object = constructor.newInstance (arg_list);
                log.info ("NEW INSTANCE = {}", new_object);
            }
            else // TODO: IS THIS NEEDED??
            {
                log.info ("_NO_ CONSTRUCTOR");
                new_object = serviceClass.newInstance ();
            }
        }
        catch (InstantiationException e)
        {
            log.error ("Exception creating ServiceObject {}: {}", objectClassName, e.getCause ().toString ());
        }
        catch (IllegalAccessException | InvocationTargetException e)
        {
            log.error ("Exception creating ServiceObject {}", objectClassName, e);
        }
        catch (Throwable e)
        {
            log.error ("Unhandled exception creating ServiceObject {}", objectClassName, e);
        }
        return (new_object);
    }

    @Override // ServiceContext
    public Object newServiceObject (String objectClassName, Map<String, Object> properties)
    {
        Object new_object = null;

        if (service_object_providers.containsKey (objectClassName))
        {
            ServiceObject.Provider provider = service_object_providers.get (objectClassName);
            new_object = provider.newObject (objectClassName, properties);
        }
        else
        {
            new_object = create_using_reflection (objectClassName, properties);
        }
        return ((new_object == null)? null: internal_wrap_object (new_object));
    }

    @Override
    public Object newServiceObject (String objectClassName)
    {
        return (newServiceObject (objectClassName, Collections.EMPTY_MAP));
    }

    @Override
    public <T> T newServiceObject (Class<T> serviceClass, Map<String, Object> properties)
    {
        return (serviceClass.cast (newServiceObject (serviceClass.getName (), properties)));
    }

    @Override // ServiceContext
    public <T> T newServiceObject (Class<T> serviceClass)
    {
        return (serviceClass.cast (newServiceObject (serviceClass.getName ())));
    }

    private synchronized Map<String, Object> get_properties (long bundle_id)
    {
        Map<String, Object> properties = properties_by_bundleid.get (bundle_id);

        if (properties == null)
        {
            properties = new ConcurrentHashMap<> ();
            properties_by_bundleid.put (bundle_id, properties);
        }
        return (properties);
    }

    @Override // ServiceContext
    public <T> T getService (BundleContext context, Class<T> type)
    {
        Map<String, Object> properties = get_properties (context.getBundle ().getBundleId ());
        Object service = properties.get (type.getName ());
        log.info ("(***) getService: bundle={} type={} service={}", context.getBundle (), type, service);
        return (type.cast (service));
    }

    @Override // ServiceContext
    public <T> void putService (BundleContext context, Class<T> type, T service)
    {
        Map<String, Object> properties = get_properties (context.getBundle ().getBundleId ());
        properties.put (type.getName (), service);
        log.info ("(***) putService: bundle={} type={} service={}", context.getBundle (), type, service);
    }

    private void bundle_cleanup (Bundle departing_bundle)
    {
        //-----------------
        // CLEAR LISTENERS
        //-----------------

        Iterator<ServiceObject.Listener> it_provider = listener_list.iterator ();

        while (it_provider.hasNext ())
        {
            ServiceObject.Listener entry = it_provider.next ();

            if (FrameworkUtil.getBundle (entry.getClass ()) == departing_bundle)
            {
                log.info ("Cleaning listener: {}", entry);
                it_provider.remove ();
            }
        }

        //-------------------------------------------------
        // CLEAR ALL SERVICE OBJECTS FROM DEPARTING BUNDLE
        //-------------------------------------------------

        Set<WeakReference<Object>> instance_set = get_instance_set (departing_bundle, false);

        log.info ("bundle_cleanup: instance_set={} size={}",
            instance_set, instance_set == null? 0: instance_set.size ());

        if (instance_set != null)
        {
            instance_set_cache.remove (departing_bundle);

            for (WeakReference<Object> obj_wref: instance_set)
            {
                Object obj = obj_wref.get ();

                log.info ("WR: {} obj: {}", obj_wref, obj);

                if (obj != null)
                {
                    log.info ("Cleaning {}", obj);
                    broadcast_event (ServiceObject.INVALIDATE, obj);
                    call_annotated (ServiceObject.Invalidate.class, obj);
                }
            }
        }
    }

    @Validate
    private void validate ()
    {
        log.info ("----->> DefaultServiceContext started ({})", this);
        bundle_cleaner = new BundleCleanup (context);
        bundle_cleaner.open ();
    }

    @Invalidate
    private void invalidate ()
    {
        bundle_cleaner.close ();
        bundle_cleaner = null;
        log.info ("----->> DefaultServiceContext terminated ({})", this);
    }

    class BundleCleanup extends BundleTracker
    {
        BundleCleanup (BundleContext context)
        {
            super (context, Bundle.ACTIVE, null);
            log.info ("NEW BundleTracker instance = {}", this);
        }

        @Override
        public void removedBundle (Bundle bundle, BundleEvent event, Object object)
        {
            log.info ("removedBundle (bundle={} event={} object={})", bundle, event, object);
            bundle_cleanup (bundle);
        }
    }
}

// EOF
