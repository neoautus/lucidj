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

import org.lucidj.api.CodeContext;
import org.lucidj.api.ManagedObjectInstance;
import org.lucidj.api.ServiceBinding;
import org.lucidj.api.ServiceBindingsManager;

import javax.lang.model.type.TypeKind;
import javax.script.Bindings;
import javax.script.ScriptContext;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.osgi.framework.Bundle;

public class FelixCodeEngineContext implements CodeContext, CodeContext.Callbacks, ScriptContext
{
    private final int HACKISH_SERVICE_SCOPE = 0;

    private Map<String, Object> properties = new HashMap<> ();

    private PrintStream ps_stdout, ps_stderr;
    private Object output = TypeKind.NONE;

    private Bundle bundle;
    private ClassLoader class_loader;
    private ServiceBindingsManager bindingsManager;

    private ScriptContext wrapped_context;

    private List<Callbacks> listeners = new ArrayList<> ();

    public FelixCodeEngineContext (Bundle bundle, ServiceBindingsManager bindingsManager)
    {
        this.bundle = bundle;
        this.bindingsManager = bindingsManager;
        setStdout (System.out); // Not that sane defaults...
        setStderr (System.err);
    }

    @Override // CodeContext
    public String getContextId ()
    {
        return ("<" + this.toString () + ">");
    }

    @Override
    public void setClassLoader (ClassLoader classLoader)
    {
        this.class_loader = classLoader;
    }

    @Override
    public ClassLoader getClassLoader ()
    {
        return (class_loader);
    }

    @Override // CodeContext
    public void setStdout (PrintStream stdout)
    {
        if (wrapped_context != null)
        {
            setWriter (new PrintWriter (stdout));
        }
        this.ps_stdout = stdout;
    }

    @Override // CodeContext
    public PrintStream getStdout ()
    {
        return (ps_stdout);
    }

    @Override // CodeContext
    public void setStderr (PrintStream stderr)
    {
        if (wrapped_context != null)
        {
            setErrorWriter (new PrintWriter (stderr));
        }
        ps_stderr = stderr;
    }

    @Override // CodeContext
    public PrintStream getStderr ()
    {
        return (ps_stderr);
    }

    @Override // CodeContext
    public Bundle getBundle ()
    {
        return (bundle);
    }

    @Override // CodeContext
    public Object getServiceObject (String name)
    {
        ServiceBinding service = bindingsManager.getService (name);

        if (service != null)
        {
            // Pass this context allowing the service to be customized for us
            Object service_object = service.getService (this);
            fetchService (name, service_object);
            return (service_object);
        }
        return (null);
    }

    @Override // CodeContext
    public ScriptContext wrapContext (ScriptContext jsr223_context)
    {
        wrapped_context = jsr223_context;
        setWriter (new PrintWriter (ps_stdout));
        setErrorWriter (new PrintWriter (ps_stderr));
        return (this);
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
    public void fetchService (String svcName, Object svcObject)
    {
        for (Callbacks listener: listeners)
        {
            listener.fetchService (svcName, svcObject);
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

    //-----------------------------------------------------------------------------------------------------------------
    // ScriptContext wrapping
    //-----------------------------------------------------------------------------------------------------------------

    @Override // ScriptContext
    public void setBindings (Bindings bindings, int scope)
    {
        wrapped_context.setBindings (bindings, scope);
    }

    @Override // ScriptContext
    public Bindings getBindings (int scope)
    {
        return (wrapped_context.getBindings (scope));
    }

    @Override // ScriptContext
    public int getAttributesScope (String name)
    {
        int scope = wrapped_context.getAttributesScope (name);

        if (scope == -1 && bindingsManager.serviceExists (name))
        {
            // See [1] getAttribute(name,scope)
            scope = HACKISH_SERVICE_SCOPE;
        }
        return (scope);
    }

    @Override // ScriptContext
    public Object getAttribute (String name, int scope)
    {
        if (scope == HACKISH_SERVICE_SCOPE)
        {
            // [1] Nashorn Global.__noSuchProperty__() tries to find the missing
            // attribute by retrieving the scope and issuing getAttribute(name,scope).
            // So we need to return strictly this service object here, or else we end
            // up into a stack overflow.
            return (getServiceObject (name));
        }
        else
        {
            // Try the context first...
            Object obj = wrapped_context.getAttribute (name, scope);

            // ...then fall back into a service object. This way we can
            // override locally a service (if ever needed).
            return ((obj != null)? obj: getServiceObject (name));
        }
    }

    @Override // ScriptContext
    public Object getAttribute (String name)
    {
        // Local objects might override bound services
        Object obj = wrapped_context.getAttribute (name);
        return ((obj != null)? obj: getServiceObject (name));
    }

    @Override // ScriptContext
    public void setAttribute (String name, Object value, int scope)
    {
        wrapped_context.setAttribute (name, value, scope);
    }

    @Override // ScriptContext
    public Object removeAttribute (String name, int scope)
    {
        return (wrapped_context.removeAttribute (name, scope));
    }

    @Override // ScriptContext
    public Writer getWriter ()
    {
        return (wrapped_context.getWriter ());
    }

    @Override // ScriptContext
    public Writer getErrorWriter ()
    {
        return (wrapped_context.getErrorWriter ());
    }

    @Override // ScriptContext
    public void setWriter (Writer writer)
    {
        wrapped_context.setWriter (new PrintWriter (writer));
    }

    @Override // ScriptContext
    public void setErrorWriter (Writer writer)
    {
        wrapped_context.setErrorWriter (new PrintWriter (writer));
    }

    @Override // ScriptContext
    public Reader getReader ()
    {
        return (wrapped_context.getReader ());
    }

    @Override // ScriptContext
    public void setReader (Reader reader)
    {
        wrapped_context.setReader (reader);
    }

    @Override // ScriptContext
    public List<Integer> getScopes ()
    {
        return (wrapped_context.getScopes ());
    }
}

// EOF
