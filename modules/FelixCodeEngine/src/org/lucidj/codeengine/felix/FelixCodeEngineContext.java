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

import org.lucidj.api.CodeBindings;
import org.lucidj.api.CodeContext;
import org.lucidj.api.CodeEngineManager;
import org.lucidj.api.ManagedObjectInstance;

import javax.lang.model.type.TypeKind;
import javax.script.ScriptContext;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.osgi.framework.Bundle;

public class FelixCodeEngineContext implements CodeContext, CodeContext.Callbacks
{
    private Map<String, Object> properties = new HashMap<> ();

    private PrintStream stdout, stderr;
    private Object output = TypeKind.NONE;

    private Bundle bundle;
    private FelixCodeEngineManager engineManager;
    private CodeBindings bindings;

    private ScriptContext linked_context;

    private List<Callbacks> listeners = new ArrayList<> ();

    public FelixCodeEngineContext (Bundle bundle, CodeEngineManager engineManager)
    {
        this.bundle = bundle;
        this.engineManager = (FelixCodeEngineManager)engineManager;
//        this.bindings = ???;
        stdout = System.out; // Not that sane defaults...
        stderr = System.err;
    }

    @Override
    public String getContextId ()
    {
        return ("<" + this.toString () + ">");
    }

    @Override // CodeContext
    public void setStdout (PrintStream stdout)
    {
        if (linked_context != null)
        {
            linked_context.setWriter (new PrintWriter (stdout));
        }
        this.stdout = stdout;
    }

    @Override // CodeContext
    public PrintStream getStdout ()
    {
        return (stdout);
    }

    @Override // CodeContext
    public void setStderr (PrintStream stderr)
    {
        if (linked_context != null)
        {
            linked_context.setErrorWriter (new PrintWriter (stderr));
        }
        this.stderr = stderr;
    }

    @Override // CodeContext
    public PrintStream getStderr ()
    {
        return (stderr);
    }

    @Override
    public void setBindings (CodeBindings bindings)
    {
        if (linked_context != null)
        {
            linked_context.setBindings (bindings, ScriptContext.ENGINE_SCOPE); // Which is the correct scope???
        }
        this.bindings = bindings;
    }

    @Override
    public CodeBindings getBindings ()
    {
        return (bindings);
    }

    @Override // CodeContext
    public Bundle getBundle ()
    {
        return (bundle);
    }

    @Override
    public void linkContext (ScriptContext jsr223_context)
    {
        linked_context = jsr223_context;

        // Copy some volatile parameters
        linked_context.setWriter (new PrintWriter (stdout));
        linked_context.setErrorWriter (new PrintWriter (stderr));
    }

    @Override
    public ScriptContext getLinkedContext ()
    {
        return (linked_context);
    }

    @Override // CodeContext
    public <T> T getObject (Class<T> type)
    {
        return (type.cast (properties.get (type.getName ())));
    }

    @Override // CodeContext
    public <T> void putObject (Class<T> type, T obj)
    {
        properties.put (type.getName (), obj);
    }

    @Override // CodeContext
    public Object getOutput ()
    {
        return (output);
    }

    @Override // CodeContext
    public boolean haveOutput ()
    {
        return (!TypeKind.NONE.equals (output));
    }

    @Override // ManagedObject
    public void validate (ManagedObjectInstance instance)
    {
        // Nop
    }

    @Override // ManagedObject
    public void invalidate (ManagedObjectInstance instance)
    {
        // Nop
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Callbacks
    //-----------------------------------------------------------------------------------------------------------------

    // TODO: THIS IS A GREAT PLACE TO ADD LISTENERS!!

    @Override // CodeContext
    public void addCallbacksListener (Callbacks listener)
    {
        if (!listeners.contains (listener))
        {
            listeners.add (listener);
        }
    }

    @Override // CodeContext
    public void removeCallbacksListener (Callbacks listener)
    {
        listeners.remove (listener);
    }

    @Override // CodeContext.Callbacks
    public void stdoutPrint (String str)
    {
        for (CodeContext.Callbacks listener: listeners)
        {
            listener.stdoutPrint (str);
        }
    }

    @Override // CodeContext.Callbacks
    public void stderrPrint (String str)
    {
        for (Callbacks listener: listeners)
        {
            listener.stderrPrint (str);
        }
    }

    @Override // CodeContext.Callbacks
    public void outputObject (Object obj)
    {
        output = obj;
    }

    @Override // CodeContext.Callbacks
    public void started ()
    {
        for (Callbacks listener: listeners)
        {
            listener.started ();
        }
    }

    @Override // CodeContext.Callbacks
    public void terminated ()
    {
        for (Callbacks listener: listeners)
        {
            listener.terminated ();
        }
    }
}

// EOF
