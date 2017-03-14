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

package org.rationalq.smartbox;

import bsh.CallStack;
import bsh.Interpreter;
import org.lucidj.api.ComponentInterface;
import org.lucidj.api.ObjectManager;
import org.lucidj.runtime.CompositeTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO: PIPE SHOULD BECOME AN EXTENSION SERVICE
public class Pipe
{
//    public final static String PIPE_PROPERTY_NAME = Pipe.class.getCanonicalName ();
//
//    private final transient static Logger log = LoggerFactory.getLogger (Pipe.class);
//    private static ThreadLocal<Object> current_parent = new InheritableThreadLocal<> ();
//
//    public static void setComponentContext (Object parent)
//    {
//        current_parent.set (parent);
//    }
//
//    public static String usage ()
//    {
//        return ("Usage: Pipe ( [index] )\n");
//    }
//
//    public static ObjectManager pipe ()
//    {
//        log.info ("pipe() start");
//
//        CompositeTask ctask = Kernel.currentTaskContext ().currentTask (CompositeTask.class);
//
//        log.info ("pipe() ctask={}", ctask);
//
//        if (ctask == null)
//        {
//            return (null);
//        }
//
//        Object parent = current_parent.get ();
//        int parent_index = ctask.indexOf (parent);
//
//        log.info ("pipe() parent={} parent_index={}", parent, parent_index);
//
//        if (parent_index < 1)
//        {
//            // No automatic pipe available for 0 and not found
//            return (null);
//        }
//
//        do
//        {
//            parent_index--;
//
//            if (ctask.get (parent_index) instanceof ComponentInterface)
//            {
//                ComponentInterface comp = (ComponentInterface)ctask.get (parent_index);
//
//                log.info ("pipe() comp={}", comp);
//
//                if (comp.getProperty (PIPE_PROPERTY_NAME) != null)
//                {
//                    log.info ("pipe() FOUND {}", comp.getProperty (PIPE_PROPERTY_NAME));
//                    return ((ObjectManager)comp.getProperty (PIPE_PROPERTY_NAME));
//                }
//            }
//
//        }
//        while (parent_index > 0);
//
//        log.info ("pipe() NOT FOUND");
//
//        // No pipe found
//        return (null);
//    }
//
//    public static ObjectManager invoke (Interpreter env, CallStack callstack)
//    {
//        log.info ("{} => Pipe()", current_parent.get ());
//        return (pipe ());
//    }
//
//    public static Object pipe (int index)
//    {
//        ObjectManager om = pipe ();
//
//        if (index < 0 || index >= om.available ())
//        {
//            return (null);
//        }
//        return (om.getObject (index));
//    }
//
//    public static Object invoke (Interpreter env, CallStack callstack, int index)
//    {
//        log.info ("{} => Pipe({})", current_parent.get (), index);
//        return (pipe (index));
//    }
}

// EOF
