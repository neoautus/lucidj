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

import java.util.ArrayList;
import java.util.List;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.ConfigurationException;
import org.apache.felix.ipojo.Factory;
import org.apache.felix.ipojo.InstanceManager;
import org.apache.felix.ipojo.MissingHandlerException;
import org.apache.felix.ipojo.UnacceptableConfiguration;
import org.apache.felix.ipojo.annotations.Bind;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Unbind;

@Component (immediate = true, public_factory = false)
@Provides
@Instantiate
public class TaskManagerImpl implements TaskManager
{
    private static final transient Logger log = LoggerFactory.getLogger (Kernel.class);

    private InheritableThreadLocal<TaskContext> current_task = new InheritableThreadLocal<> ();
    private List<TaskInfo> task_info_list = new ArrayList<> ();
    private Factory tc_factory;

    private TaskManagerImpl ()
    {
        // Singleton
    }

    public TaskContext currentTaskContext ()
    {
        return (current_task.get ());
    }

    public void bindTaskContext (TaskContext tctx)
    {
        log.info ("Thread {} binded to {}", Thread.currentThread (), tctx);
        current_task.set (tctx);
    }

    @Bind (optional=true, filter="(component.providedServiceSpecifications=org.lucidj.api.TaskContext)")
    private void bind_tc_factory (Factory factory)
    {
        tc_factory = factory;
    }

    @Unbind
    private void unbind_tc_factory (Factory factory)
    {
        tc_factory = null;
    }

    public TaskContext createTaskContext ()
    {
        log.info ("<<KERNEL>> createTaskContext: {} tc_factory={}", this, tc_factory);

        if (tc_factory != null)
        {
            try
            {
                // Create a new instance...
                ComponentInstance new_comp = tc_factory.createComponentInstance (null);

                if (new_comp.getState () == ComponentInstance.VALID)
                {
                    // ...and extracts Pojo object from it
                    TaskContext new_tc = (TaskContext)((InstanceManager)new_comp).getPojoObject ();

                    log.info ("<<KERNEL>> newTaskContext(): new_tc = {}", new_tc);

                    task_info_list.add (new TaskInfo (new_comp, new_tc));

                    // Bind new TaskContext to current thread
                    bindTaskContext (new_tc);
                    return (new_tc);
                }
            }
            catch (UnacceptableConfiguration | MissingHandlerException | ConfigurationException e)
            {
                log.error ("Exception creating new TaskContext", e);
            }
        }

        return (null);
    }

    public TaskContext[] getTaskContexts ()
    {
        List<TaskContext> tc_list = new ArrayList<> ();

        for (TaskInfo ti : task_info_list)
        {
            tc_list.add (ti.getTaskContext ());
        }

        return (tc_list.toArray (new TaskContext [tc_list.size ()]));
    }

    class TaskInfo
    {
        ComponentInstance component_instance;
        TaskContext task_context;

        TaskInfo (ComponentInstance component_instance, TaskContext task_context)
        {
            this.component_instance = component_instance;
            this.task_context = task_context;
        }

        ComponentInstance getComponentInstance ()
        {
            return (component_instance);
        }

        TaskContext getTaskContext ()
        {
            return (task_context);
        }
    }
}

// EOF
