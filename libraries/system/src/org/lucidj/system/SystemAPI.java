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

package org.lucidj.system;

import org.apache.shiro.subject.Subject;
import org.lucidj.task.TaskContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.kuori.shiro.Shiro;

import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.ConfigurationException;
import org.apache.felix.ipojo.Factory;
import org.apache.felix.ipojo.InstanceManager;
import org.apache.felix.ipojo.MissingHandlerException;
import org.apache.felix.ipojo.Pojo;
import org.apache.felix.ipojo.UnacceptableConfiguration;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

@Component (publicFactory=false, immediate = true)
@Instantiate
@Provides
public class SystemAPI
{
    private final static transient Logger log = LoggerFactory.getLogger (SystemAPI.class);
    private static SystemAPI self;

    @Requires private transient Shiro shiro;
    @Requires private transient TaskContext tc_factory;

    private Map<String, TaskContext> ctx_table = new HashMap<> ();
    private Map<String, Object> published_objects = new HashMap<> ();

    public SystemAPI ()
    {
        self = this;
    }

    public static SystemAPI getCurrent ()
    {
        return (self);
    }

    // TODO: PUT IT INTO ctx_table SO WE CAN KEEP TRACK OF ALL TaskContexts
    public TaskContext newTaskContext ()
    {
        Factory factory = ((Pojo)tc_factory).getComponentInstance ().getFactory();

        try
        {
            Properties props = new Properties();

            // Create a new instance...
            ComponentInstance new_comp = factory.createComponentInstance (props);
            //component_map.put(navid, new_comp);

            String[] status = "-1/DISPOSED,0/STOPPED,1/INVALID,2/VALID".split("\\,");
            log.info ("@@@@@ is_valid => " + status [new_comp.getState() + 1]);

            if (new_comp.getState() == ComponentInstance.VALID)
            {
                // ...and returns Pojo object from it
                TaskContext new_tc = (TaskContext) ((InstanceManager)new_comp).getPojoObject ();

                log.info("newTaskContext(): NEW = {}", new_tc);
                return (new_tc);
            }
        }
        catch (UnacceptableConfiguration | MissingHandlerException | ConfigurationException e)
        {
            log.error ("Exception creating new TaskContext", e);
        }

        return (null);
    }

    public void publishObject (String identifier, Object obj)
    {
        published_objects.put (identifier, obj);
    }

    public void unpublishObject (String identifier)
    {
        published_objects.remove (identifier);
    }

    public Object getPublishedObject (String identifier)
    {
        return (published_objects.get (identifier));
    }

    public Subject getSubject ()
    {
        if (shiro == null)
        {
            return (null);
        }

        Subject subject = null;

        try
        {
            subject = shiro.getSubject ();
        }
        catch (Exception ignore) {};

        return (subject);
    }

    public String getCurrentUser ()
    {
        Subject subject = getSubject ();

        if (subject != null)
        {
            Object principal = subject.getPrincipal ();

            if (principal instanceof String)
            {
                return ((String)principal);
            }
        }

        return (null);
    }

    public FileSystem getDefaultUserFS ()
    {
        return (shiro.getDefaultUserFS ());
    }

    public Path getDefaultUserDir ()
    {
        return (shiro.getDefaultUserDir ());
    }

    private TaskContext get_context (String username, Object ipojo_component, String component_context)
    {
        String ctx_name = username + "/" + ipojo_component.getClass ().getSimpleName ();

        if (component_context != null)
        {
            ctx_name += "/" + component_context;
        }

        if (ctx_table.containsKey (ctx_name))
        {
            return (ctx_table.get (ctx_name));
        }

        // Full ctx_name is used as context id
        TaskContext new_ctx = newTaskContext (); // SystemContext (ctx_name, ipojo_component);
        ctx_table.put (ctx_name, new_ctx);

        return (new_ctx);
    }

    public TaskContext getSystemContext (Object ipojo_component, String component_context)
    {
        if (getCurrentUser () == null ||
            getCurrentUser ().equals (SystemConfig.getAdminUsername ()))
        {
            return (get_context (SystemConfig.getAdminUsername (), this/*ipojo_component*/, component_context));
        }

        // No system context for logged users
        return (null);
    }

    public TaskContext getContext (Object ipojo_component, String component_context)
    {
        if (getCurrentUser () != null)
        {
            return (get_context (getCurrentUser (), this/*ipojo_component*/, component_context));
        }

        return (null);
    }

    // TODO: CONVERT TO DATASOURCE, EXPORT COPY OF DATA
    public TaskContext[] getAllContexts ()
    {
        TaskContext[] ctxl = new TaskContext [ctx_table.size ()];
        int index = 0;

        for (TaskContext sc: ctx_table.values ())
        {
            ctxl [index++] = sc;
        }

        return (ctxl);
    }
}

// EOF
