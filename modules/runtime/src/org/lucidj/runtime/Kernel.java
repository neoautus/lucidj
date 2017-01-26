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

package org.lucidj.runtime;

import org.lucidj.api.TaskContext;
import org.lucidj.api.TaskManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

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
}

// EOF
