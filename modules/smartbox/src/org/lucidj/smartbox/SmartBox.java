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

import org.lucidj.api.core.CodeContext;
import org.lucidj.api.core.CodeEngine;
import org.lucidj.api.core.ComponentInterface;
import org.lucidj.api.core.ComponentState;
import org.lucidj.api.core.DisplayManager;
import org.lucidj.api.core.ServiceContext;
import org.lucidj.api.core.ServiceObject;
import org.lucidj.api.core.Stdio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

public class SmartBox implements ComponentInterface, ComponentState
{
    private final static Logger log = LoggerFactory.getLogger (SmartBox.class);

    private int component_state = ACTIVE;
    private ComponentState.ChangeListener state_listener;

    private HashMap<String, Object> properties = new HashMap<>();

    private DisplayManager displayManager;

    private String code = "";

    private Stdio console;

    private ServiceContext serviceContext;
    private CodeEngine code_engine;
    private CodeContext code_context;

    public SmartBox (ServiceContext serviceContext)
    {
        this.serviceContext = serviceContext;

        // Create our own DisplayManager
        displayManager = serviceContext.newServiceObject (DisplayManager.class);
    }

    // TODO: DECOUPLE THESE OBJECTS INTO PLUGGABLE STRUCTURES
    private Stdio get_console (boolean show)
    {
        if (console == null)
        {
            console = serviceContext.newServiceObject (Stdio.class);
        }

        if (show && displayManager.getObject (Stdio.class.getCanonicalName ()) == null)
        {
            displayManager.showObject (console);
            displayManager.setObjectTag (console, Stdio.class.getCanonicalName ());
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
                if (displayManager.getObject (svcName) == null && displayManager.showAutoDisplay (svcObject))
                {
                    displayManager.setObjectTag (svcObject, svcName);
                }
            }

            @Override
            public void outputObject (Object obj)
            {
                displayManager.showObject (obj);
            }

            @Override
            public void started ()
            {
                // Set proper DisplayManager and SmartBox _inside_ the new running thread
//                show.setObjectManager (displayManager);
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

                    displayManager.showObject (obj);

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
                displayManager.release ();

                // TODO: ADD LATER SOME AUTO UPDATE
                //update_pragmas ();
            }
        });
    }

    private void eventhandler_run ()
    {
        log.info (">>> RUN {}", code);
        displayManager.restrain ();
        displayManager.clearObjects ();
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
//            return (displayManager);
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

    public DisplayManager _getDisplayManager ()
    {
        return (displayManager);
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

    @ServiceObject.Validate
    public void validate ()
    {
        // Nothing for now
    }

    @ServiceObject.Invalidate
    public void invalidate ()
    {
        // Nothing for now
    }
}

// EOF
