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

package org.lucidj.runtime;

import org.lucidj.api.TaskContext;
import org.lucidj.api.TaskManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.Factory;
import org.apache.felix.ipojo.InstanceManager;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.configuration.Configuration;

@Component (immediate = true, public_factory = false)
@Provides
@Instantiate
@Configuration
public class Kernel
{
    private static final long serialVersionUID = 1L;

    // Only the major/minor is useful for API runtime level purposes
    public static final String VERSION = "1.0.0";

    private static final transient Logger log = LoggerFactory.getLogger (Kernel.class);

    private static TaskManager task_manager_instance_cache;
    private static AtomicInteger instance_counter;

    @Requires
    private TaskManager task_manager_component;

    private Kernel ()
    {
        task_manager_instance_cache = task_manager_component;
        instance_counter = new AtomicInteger (1);
        log.info ("<<KERNEL>> TaskManager component = {}", task_manager_instance_cache);
    }

    public static String apiLevel ()
    {
        return (VERSION);
    }

    public static TaskManager taskManager ()
    {
        return (task_manager_instance_cache);
    }

    public static TaskContext currentTaskContext ()
    {
        return ((task_manager_instance_cache == null)? null: task_manager_instance_cache.currentTaskContext ());
    }

    public static void bindTaskContext (TaskContext tctx)
    {
        task_manager_instance_cache.bindTaskContext (tctx);
    }

    public static TaskContext createTaskContext ()
    {
        return (task_manager_instance_cache.createTaskContext ());
    }

    //=============================================
    // TODO: FIND A NEW HOME FOR COMPONENT METHODS
    //=============================================

    public static ComponentInstance newComponentInstance (Class component_class, Dictionary properties)
    {
        if (properties == null)
        {
            properties = new Properties ();
        }

        if (properties.get ("instance.name") == null)
        {
            // Provide a default name for our new components: K-class-nnn
            properties.put ("instance.name",
                "K-" + component_class.getCanonicalName () + "-" + instance_counter.getAndIncrement ());
        }

        // We'll build the new component using its registered factory, so it
        // can do proper initialization on all iPojo annotations and stuff
        try
        {
            BundleContext ctx = FrameworkUtil.getBundle (component_class).getBundleContext ();
            ServiceReference[] references = ctx.getServiceReferences (Factory.class.getCanonicalName (),
                "(factory.name=" + component_class.getCanonicalName () + ")");
            Factory instance_factory = Factory.class.cast (ctx.getService (references [0]));
            return (instance_factory.createComponentInstance (properties));
        }
        catch (Exception e)
        {
            // Sooo many things can go wrong :)
            log.error ("Exception on newComponentInstance service lookup", e);
        }

        return (null);
    }

    public static <A> A newComponent (Class<A> type, Dictionary properties)
    {
        ComponentInstance component_instance = newComponentInstance (type, properties);

        if (component_instance != null)
        {
            Object pojo = ((InstanceManager)component_instance).getPojoObject();

            if (pojo != null && pojo.getClass ().isAssignableFrom (type))
            {
                return (type.cast (pojo));
            }
        }

        return (null);
    }

    public static <A> A newComponent (Class<A> type)
    {
        return (newComponent (type, null));
    }
}

// EOF
