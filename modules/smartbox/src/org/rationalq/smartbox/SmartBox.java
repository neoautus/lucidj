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

import org.lucidj.api.ComponentInterface;
import org.lucidj.api.ComponentState;
import org.lucidj.api.ObjectManager;
import org.lucidj.api.ObjectManagerProperty;
import org.lucidj.api.Quark;
import org.lucidj.api.TaskContext;
import org.lucidj.objectmanager.DefaultObjectManager;
import org.lucidj.runtime.Kernel;
import org.lucidj.console.Console;
import org.rationalq.vaadin.Vaadin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

@Component (immediate = true)
@Instantiate
@Provides
public class SmartBox implements Quark, ComponentInterface, ObjectManagerProperty, ComponentState
{
    private final static transient Logger log = LoggerFactory.getLogger (SmartBox.class);

    private final SmartBox self = this;

    private int component_state = ACTIVE;
    private ComponentState.ChangeListener state_listener;

    private BeanShellProvider bsh;
    private BeanShell sh;

    private HashMap<String, Object> properties = new HashMap<>();
    private TaskContext tctx;

    private ObjectManager om;

    private String code = "";

    // TODO: GET RID OF THESE DEPENDENCIES!!!
    private Console console;
    private Vaadin vaadin;
    private Karaf karaf;

    public SmartBox ()
    {
        init ();
    }

    // TODO: DECOUPLE THESE OBJECTS INTO PLUGGABLE STRUCTURES
    private Console get_console (boolean show)
    {
        if (console == null)
        {
            console = new Console ();
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
            vaadin = new Vaadin ();
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
        tctx = Kernel.currentTaskContext ();

        log.info ("tctx = {}", Kernel.currentTaskContext ());

        if (tctx == null)
        {
            log.info ("Will not init environment, no TaskContext available.");
            return;
        }

        // TODO: FACTORY....
        om = new DefaultObjectManager ();

        bsh = tctx.getObject (BeanShellProvider.class);

        if (bsh == null)
        {
            bsh = new BeanShellProvider (tctx.getClassLoader ());
            bsh.init (null);
            tctx.putObject (bsh);
        }

        log.info("bsh = {}", bsh);

        sh = bsh.getInstance ();

        log.info ("sh = {}", sh);

        // TODO: CREATE Console RENDERER
        sh.outListener (new BeanShell.PrintStreamListener ()
        {
            @Override
            public void print (String output)
            {
                get_console (true).output ("OUT", output);
            }
        });

        sh.errListener (new BeanShell.PrintStreamListener ()
        {
            @Override
            public void print (String output)
            {
                get_console (true).output ("ERR", output);
            }
        });

        sh.stateListener (new BeanShell.StateListener ()
        {
            @Override
            public void state (Thread.State s)
            {
                if (s == Thread.State.RUNNABLE)
                {
                    // Set proper ObjectManager and SmartBox _inside_ the new running thread
                    show.setObjectManager (om);
                    Pipe.setComponentContext (self);
                    pragma.setSmartBox (self);
                    setState (RUNNING);
                }
                else if (s == Thread.State.TERMINATED || s == Thread.State.BLOCKED)
                {
                    Object obj;

                    setState (TERMINATED);

                    if ((obj = sh.getResult ()) != null)
                    {
                        om.showObject (obj);
                    }

                    if ((obj = sh.getException ()) != null)
                    {
                        if (s == Thread.State.BLOCKED)
                        {
                            setState (INTERRUPTED);
                        }
                        else
                        {
                            setState (ABORTED);
                        }
                        om.showObject (obj);
                    }

                    // Release screen update if not already done
                    om.release ();

                    // TODO: ADD LATER SOME AUTO UPDATE
                    //update_pragmas ();
                }
            }
        });

        sh.dynamicVariableListener (new BeanShell.DynamicVariableListener ()
        {
            @Override
            public Object getDynamicVariable (String varname)
                throws NoSuchFieldException
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
                else if ("Pipe".equals (varname))
                {
                    return (Pipe.pipe ());
                }
                else if (Kernel.currentTaskContext ().getPublishedObject (varname) != null)
                {
                    return (Kernel.currentTaskContext ().getPublishedObject (varname));
                }
                throw new NoSuchFieldException (varname);
            }
        });
    }

    private void eventhandler_run ()
    {
        log.info ("TaskContext before = {}", Kernel.currentTaskContext ());
        Kernel.bindTaskContext (tctx);
        log.info ("TaskContext after = {}", Kernel.currentTaskContext ());

        log.info (">>> RUN {}", code);
        get_console (false).clear ();
        get_vaadin (false).removeAllComponents ();
        om.restrain ();
        om.clearObjects ();
        sh.exec (code + ";");
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
                    sh.requestBreak ();
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
        if (Pipe.PIPE_PROPERTY_NAME.equals (name))
        {
            return (om);
        }
        return (properties.get (name));
    }

    @Override
    public String getIconTitle ()
    {
        // TODO: MOVE THIS AWAY TO A PROPER VISUAL COMPONENT
        return "BeanShell";
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

    @Override // Quark
    public void deserializeObject(Map<String, Object> properties)
    {
        this.properties.putAll(properties);
        code = (String)properties.get("/");

        Object obj;
        int i = 0;

        log.info ("*** deserializeObject: properties={}", properties);

        while ((obj = properties.get("output" + i)) != null)
        {
            log.info ("*** output{} = {}", i, obj);
            om.showObject (obj);
            i++;
        }
    }

    @Override // Quark
    public Map<String, Object> serializeObject ()
    {
        log.info("serializeObject()");
        properties.put("/", code);

        //properties.put ("output", om.getObjects ());
        for (int i = 0; i < om.available (); i++)
        {
            properties.put ("output" + i, om.getObject (i));
        }

        return(properties);
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
            sh.requestBreak ();
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
}

// EOF
