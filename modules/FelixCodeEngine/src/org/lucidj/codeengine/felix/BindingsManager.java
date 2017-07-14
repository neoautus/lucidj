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

package org.lucidj.codeengine.felix;

import org.lucidj.api.CodeContext;
import org.lucidj.api.ServiceBinding;
import org.lucidj.api.ServiceBindingsManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Context;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

@Component (immediate = true, publicFactory = false)
@Instantiate
@Provides
public class BindingsManager implements ServiceBindingsManager
{
    private final static Logger log = LoggerFactory.getLogger (BindingsManager.class);
    private Map<String, ServiceBinding> bindings = new ConcurrentHashMap<> ();

    @Context
    private BundleContext context;

    @Override
    public void register (ServiceBinding binding)
    {
        bindings.put (binding.getBindingName (), binding);
    }

    private ServiceReference find_service (String binding_name)
    {
        ServiceReference[] refs = null;

        //---------------------------------------------------------
        // Try to locate specific service instances first (faster)
        //---------------------------------------------------------

        try
        {
            // First try to locate a service instance, where the name can be freely composed,
            // like: someService, SomeService, SOMESERVICE, SOME_SERVICE, some_service, etc
            refs = context.getServiceReferences ((String)null, "(@instance=" + binding_name + ")");

            if (refs != null)
            {
                // We should find exactly ONE instance
                if (refs.length == 1)
                {
                    log.info ("# find_service: @instance match {}", binding_name);
                    return (refs [0]);
                }
                else
                {
                    // Avoid ambiguity if multiple instances are found
                    return (null);
                }
            }
        }
        catch (InvalidSyntaxException ignore) {};

        //-------------------------------------------------------------------------------
        // Nothing specific, so let's try to find the service using it's class/interface
        //-------------------------------------------------------------------------------

        // We never return a valid service to a name which starts with uppercase,
        // since by convention such names are used for classes
        //
        // Examples:
        //      binding_name="SomeService" -> null (it's a class name)
        //      binding_name="someService" -> will search (it's a variable name)
        //
        if (!Character.isLowerCase (binding_name.charAt (0)))
        {
            log.info ("# find_service: NOT starting lowercase {}", binding_name);
            return (null);
        }

        // Build the service's probable class name
        //
        // Example:
        //      someService -> SomeService
        //
        String class_name = Character.toUpperCase (binding_name.charAt (0)) + binding_name.substring (1);

        try
        {
            // Get all available (and compatible) services
            refs = context.getServiceReferences ((String)null, null);
        }
        catch (InvalidSyntaxException ignore) {};

        if (refs == null)
        {
            // No services here is quite unusual...
            return (null);
        }

        ServiceReference found_ref = null;

        for (ServiceReference ref: refs)
        {
            String[] object_classes = (String[])ref.getProperty (Constants.OBJECTCLASS);

            if (object_classes == null)
            {
                continue;
            }

            for (String object_class: object_classes)
            {
                log.debug ("# find_service: testing {}", object_class);

                if (class_name.equals (object_class))
                {
                    // Full match, return immediately
                    return (ref);
                }

                // Match the name of the class without the package
                if (object_class.contains (".")
                    && object_class.substring (object_class.lastIndexOf ('.') + 1).equals (class_name))
                {
                    if (found_ref == null)
                    {
                        // We found a possible service
                        log.info ("# find_service: possible match {} -> {}", object_class, ref);
                        found_ref = ref;
                    }
                    else
                    {
                        // We found a possible service before, so the class name is unfortunately ambiguous
                        log.info ("# find_service: AMBIGUOUS with {} -> {}", object_class, ref);
                        return (null);
                    }
                }
            }
        }
        log.info ("# find_service: RETURNING {} -> {}", class_name, found_ref);
        // Return whatever we found (or not)
        return (found_ref);
    }

    private boolean locate_and_add_osgi_service_binding (String binding_name)
    {
        ServiceReference ref = find_service (binding_name);

        if (ref == null)
        {
            return (false);
        }
        register (new OSGiBinding (binding_name, ref));
        return (true);
    }

    @Override
    public ServiceBinding getService (String name)
    {
        return (serviceExists (name)? bindings.get (name): null);
    }

    @Override
    public boolean serviceExists (String name)
    {
        log.info ("# serviceExists (name={})", name);
        return (bindings.containsKey (name) || locate_and_add_osgi_service_binding (name));
    }

    class OSGiBinding implements ServiceBinding
    {
        private String name;
        private ServiceReference ref;

        public OSGiBinding (String name, ServiceReference ref)
        {
            this.name = name;
            this.ref = ref;
        }

        @Override
        public String getBindingName ()
        {
            return (name);
        }

        @Override
        public Object getService (CodeContext code_context)
        {
            // TODO: APPLY code_context TO SERVICE CREATION
            return (context.getService (ref));
        }
    }
}

// EOF
