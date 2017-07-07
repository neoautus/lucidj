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

import org.lucidj.api.CodeContext;
import org.lucidj.api.CodeEngine;
import org.lucidj.api.ComponentInterface;
import org.lucidj.api.ComponentState;
import org.lucidj.api.ManagedObject;
import org.lucidj.api.ManagedObjectFactory;
import org.lucidj.api.ManagedObjectInstance;
import org.lucidj.api.ObjectManager;
import org.lucidj.api.ObjectManagerProperty;
import org.lucidj.api.Stdio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.ui.AbstractComponent;

import java.util.HashMap;

public class SmartBox implements ManagedObject, ComponentInterface, ObjectManagerProperty, ComponentState
{
    private final static transient Logger log = LoggerFactory.getLogger (SmartBox.class);

    private final SmartBox self = this;

    private int component_state = ACTIVE;
    private ComponentState.ChangeListener state_listener;

    private HashMap<String, Object> properties = new HashMap<>();

    private ObjectManager om;

    private String code = "";

    // TODO: GET RID OF THESE DEPENDENCIES!!!
    private Stdio console;

    private ManagedObjectFactory objectFactory;
    private CodeEngine code_engine;
    private CodeContext code_context;

    public SmartBox ()
    {
//        init ();
    }

    public SmartBox (ManagedObjectFactory objectFactory)
    {
        this.objectFactory = objectFactory;
        log.info ("objectFactory = {}", objectFactory);

        // Create our own ObjectManager
        ManagedObjectInstance om_instance = objectFactory.newInstance (ObjectManager.class, null);
        om = om_instance.adapt (ObjectManager.class);
    }

    // TODO: DECOUPLE THESE OBJECTS INTO PLUGGABLE STRUCTURES
    private Stdio get_console (boolean show)
    {
        if (console == null)
        {
            ManagedObjectInstance console_instance = objectFactory.newInstance (Stdio.class, null);
            console = console_instance.adapt (Stdio.class);
        }

        if (show && om.getObject (Stdio.class.getCanonicalName ()) == null)
        {
            om.showObject (console);
            om.setObjectTag (console, Stdio.class.getCanonicalName ());
        }

        return (console);
    }

    private void set_code_engine (CodeEngine code_engine)
    {
        log.info ("---> set_code_engine: {}", code_engine);
        this.code_engine = code_engine;
        code_context = code_engine.getContext ();

        log.info ("set_code_engine(): code_engine={} code_context={}", code_engine, code_context);

        // TODO: REMOVE THIS CALLBACK IF CodeEngine IS CHANGED
        code_context.addCallbacksListener (new CodeContext.Callbacks ()
        {
            @Override
            public void stdoutPrint (String str)
            {
                get_console (true).stdout (str);
            }

            @Override
            public void stderrPrint (String str)
            {
                get_console (true).stderr (str);
            }

            @Override
            public void fetchService (String svcName, Object svcObject)
            {
                log.info ("fetchService(svcName={}, svcObject={})", svcName, svcObject);

                // When the returned object is a visual component,
                // it is added automatically on the object output
                if (svcObject instanceof AbstractComponent)
                {
                    if (om.getObject (svcName) == null)
                    {
                        log.info ("fetchService: will show");
                        om.showObject (svcObject);
                        om.setObjectTag (svcObject, svcName);
                    }
                }
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
//                show.setObjectManager (om);
//                    Pipe.setComponentContext (self);          ---
//                pragma.setSmartBox (self);
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
                        log.error ("THROWABLE", obj);
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
        });
    }

    private void eventhandler_run ()
    {
        log.info (">>> RUN {}", code);
        get_console (false).clear ();
//        get_vaadin (false).removeAllComponents ();
        om.restrain ();
        om.clearObjects ();
        code_engine.exec (code, null);
    }

    @Override // ComponentInterface
    public Object fireEvent (Object source, Object event)
    {
        if (code_engine == null)
        {
            return (null);
        }

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

        if (CodeEngine.CODE_ENGINE.equals (name))
        {
            set_code_engine ((CodeEngine)value);
        }
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

    // TODO: CHECK OR REORDER THIS MECHANISM
    @Override
    public boolean signal (int signal)
    {
        if (code_engine == null)
        {
            return (false);
        }

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
