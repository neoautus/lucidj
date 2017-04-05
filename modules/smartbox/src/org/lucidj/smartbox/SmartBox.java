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

package org.lucidj.smartbox;

import org.lucidj.api.BundleRegistry;
import org.lucidj.api.CodeContext;
import org.lucidj.api.CodeEngine;
import org.lucidj.api.ComponentInterface;
import org.lucidj.api.ComponentState;
import org.lucidj.api.ManagedObject;
import org.lucidj.api.ManagedObjectFactory;
import org.lucidj.api.ManagedObjectInstance;
import org.lucidj.api.ObjectManager;
import org.lucidj.api.ObjectManagerProperty;
import org.lucidj.objectmanager.DefaultObjectManager;
import org.lucidj.console.Console;
import org.lucidj.vaadin.Vaadin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

public class SmartBox implements ManagedObject, ComponentInterface, ObjectManagerProperty, ComponentState
{
    private final static transient Logger log = LoggerFactory.getLogger (SmartBox.class);

    private final SmartBox self = this;
    private String descriptor_id;

    private int component_state = ACTIVE;
    private ComponentState.ChangeListener state_listener;

    private HashMap<String, Object> properties = new HashMap<>();

    private ObjectManager om;

    private String code = "";

    // TODO: GET RID OF THESE DEPENDENCIES!!!
    private Console console;
    private Vaadin vaadin;
    private Karaf karaf;

    private BundleRegistry bundleRegistry;
    private ManagedObjectFactory objectFactory;
    private CodeEngine code_engine;
    private CodeContext code_context;

    public SmartBox ()
    {
//        init ();
    }

    public SmartBox (CodeEngine code_engine, BundleRegistry bundleRegistry, ManagedObjectFactory objectFactory)
    {
        this.bundleRegistry = bundleRegistry;
        this.objectFactory = objectFactory;

        this.code_engine = code_engine;
        code_context = code_engine.getContext ();

        log.info ("#### bundleRegistry = {}", bundleRegistry);
        init ();
    }

    // TODO: DECOUPLE THESE OBJECTS INTO PLUGGABLE STRUCTURES
    private Console get_console (boolean show)
    {
        if (console == null)
        {
            ManagedObjectInstance console_instance = objectFactory.newInstance (Console.class, null);
            console = console_instance.adapt (Console.class);
        }

        if (show && om.getObject (Console.class.getCanonicalName ()) == null)
        {
            om.showObject (console);
            om.setObjectTag (console, Console.class.getCanonicalName ());
        }

        return (console);
    }

    private Vaadin get_vaadin (boolean show)
    {
        if (vaadin == null)
        {
            ManagedObjectInstance console_instance = objectFactory.newInstance (Vaadin.class, null);
            vaadin = console_instance.adapt (Vaadin.class);
        }

        if (show && om.getObject (Vaadin.class.getCanonicalName ()) == null)
        {
            om.showObject (vaadin);
            om.setObjectTag (vaadin, Vaadin.class.getCanonicalName ());
        }

        return (vaadin);
    }


    private Karaf get_karaf ()
    {
        if (karaf == null)
        {
            karaf = new Karaf ();
        }

        // Creates a new session for every run
        return (karaf);
    }

    private void init ()
    {
        log.info ("bundleRegistry = {}", bundleRegistry);

        // TODO: FACTORY....
        om = new DefaultObjectManager ();

        log.info ("code_engine = {}", code_engine);

        code_context.addCallbacksListener (new CodeContext.Callbacks ()
        {
            @Override
            public void stdoutPrint (String str)
            {
                get_console (true).output ("OUT", str);
            }

            @Override
            public void stderrPrint (String str)
            {
                get_console (true).output ("ERR", str);
            }

            @Override
            public void outputObject (Object obj)
            {
                om.showObject (obj);
            }

            @Override
            public void started ()
            {
                // Set proper ObjectManager and SmartBox _inside_ the new running thread
                show.setObjectManager (om);
//                    Pipe.setComponentContext (self);          ---
                pragma.setSmartBox (self);
                setState (RUNNING);
            }

            @Override
            public void terminated ()
            {
                setState (TERMINATED);

                if (code_context.haveOutput ())
                {
                    Object obj = code_context.getOutput ();

                    om.showObject (obj);

                    if (obj instanceof Throwable)
                    {
//                        if (s == Thread.State.BLOCKED)
//                        {
//                            setState (INTERRUPTED);
//                        }
//                        else
//                        {
                            setState (ABORTED);
//                        }
                    }
                }

                // Release screen update if not already done
                om.release ();

                // TODO: ADD LATER SOME AUTO UPDATE
                //update_pragmas ();
            }

            // TODO: ALL THIS SHOULD GO TO CodeBindings
/*
            @Override
            public Object getVariable (String varname)
                throws NoSuchFieldError
            {
                log.debug ("getDynamicVariable {}", varname);

                if ("Console".equals(varname))
                {
                    // Console is alias for current System.out
                    return (System.out);
                }
                else if ("Vaadin".equals (varname))
                {
                    // Return current Vaadin output object
                    return (get_vaadin (true));
                }
                else if ("Karaf".equals (varname))
                {
                    return (get_karaf ());
                }
                else if ("Self".equals (varname))
                {
                    return (self);
                }
//                else if ("Pipe".equals (varname))
//                {
//                    return (Pipe.pipe ());
//                }
//                else if (Kernel.currentTaskContext ().getPublishedObject (varname) != null)
//                {
//                    return (Kernel.currentTaskContext ().getPublishedObject (varname));
//                }
                throw new NoSuchFieldError (varname);
            }
*/
        });
    }

    private void eventhandler_run ()
    {
//        log.info ("TaskContext before = {}", Kernel.currentTaskContext ());
//        Kernel.bindTaskContext (tctx);
//        log.info ("TaskContext after = {}", Kernel.currentTaskContext ());

        log.info (">>> RUN {}", code);
        get_console (false).clear ();
        get_vaadin (false).removeAllComponents ();
        om.restrain ();
        om.clearObjects ();
        code_engine.exec (code, null);
    }

    @Override // ComponentInterface
    public String getDescriptorId ()
    {
        return (descriptor_id);
    }

    @Override // ComponentInterface
    public void setDescriptorId (String descriptor_id)
    {
        this.descriptor_id = descriptor_id;
    }

    @Override // ComponentInterface
    public Object fireEvent (Object source, Object event)
    {
        if (event instanceof String)
        {
            String action = (String)event;

            switch (action)
            {
                case "run":
                {
                    eventhandler_run ();
                    break;
                }
                case "stop":
                {
                    code_engine.getThread ().interrupt ();
                    break;
                }
            }
        }
        return (null);
    }

    @Override // ComponentInterface
    public void setProperty (String name, Object value)
    {
        properties.put (name, value);
    }

    @Override // ComponentInterface
    public Object getProperty (String name)
    {
//        if (Pipe.PIPE_PROPERTY_NAME.equals (name))        // ---
//        {
//            return (om);
//        }
        return (properties.get (name));
    }

    public HashMap<String, Object> getProperties ()
    {
        return (properties);
    }

    @Override // ComponentInterface
    public void setValue (Object value)
    {
        code = (String)value;
    }

    @Override // ComponentInterface
    public Object getValue ()
    {
        return (code);
    }

    @Override // ObjectManagerProperty
    public ObjectManager getObjectManager ()
    {
        return (om);
    }

    @Override
    public int getState ()
    {
        return (component_state);
    }

    @Override
    public boolean setState (int new_state)
    {
        // Always change
        component_state = new_state;

        if (state_listener != null)
        {
            state_listener.stateChanged (this);
        }

        return (true);
    }

    @Override
    public boolean signal (int signal)
    {
        if (signal == SIGTERM && component_state == RUNNING)
        {
            code_engine.getThread ().interrupt ();
            return (true);
        }
        else if (signal == SIGSTART && component_state != RUNNING)
        {
            eventhandler_run ();
        }

        return false;
    }

    @Override
    public void addStateListener (ChangeListener listener)
    {
        state_listener = listener;
    }

    @Override // ManagedObject
    public void validate (ManagedObjectInstance instance)
    {
        // Nothing for now
    }

    @Override // ManagedObject
    public void invalidate (ManagedObjectInstance instance)
    {
        // Nothing for now
    }
}

// EOF
