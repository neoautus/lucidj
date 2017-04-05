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
import org.lucidj.api.CodeEngineBase;
import org.lucidj.api.CodeEngineProvider;
import org.lucidj.api.ManagedObjectInstance;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import java.io.PrintWriter;
import java.io.Reader;

public class ScriptEngineWrapper implements CodeEngineBase
{
    private CodeEngineProvider provider;
    private CodeContext default_context;
    private ScriptEngine engine; // The wrapped JSR223 engine

    public ScriptEngineWrapper (CodeEngineProvider provider, ScriptEngine engine)
    {
        this.provider = provider;
        this.engine = engine;
    }

    private ScriptContext get_jsr223_context (CodeContext context)
    {
        if (context == null)
        {
            context = default_context;
        }

        ScriptContext jsr223_context = context.getLinkedContext ();

        if (jsr223_context == null)
        {
            // Extract and link the JSR223 context
            jsr223_context = engine.getContext ();
            context.linkContext (jsr223_context);
        }
        return (jsr223_context);
    }

    @Override
    public Object eval (Reader code, CodeContext context)
    {
        try
        {
            return (engine.eval (code, get_jsr223_context (context)));
        }
        catch (Throwable t)
        {
            return (t);
        }
    }

    @Override
    public Object eval (String code, CodeContext context)
    {
//        javax.script.ScriptEngineManager manager = new ScriptEngineManager();
//        javax.script.ScriptEngine engine = manager.getEngineByName("groovy");
//        StringWriter stdOut = new StringWriter();
//        engine.getContext().setWriter(new PrintWriter(stdOut));
//        engine.eval("def myFunction() { print("Hello World!"); }");
//        Invocable invoker = (Invocable) engine;
//        invoker.invokeFunction("myFunction", new Object[0]);
//        return stdOut.getBuffer().toString();
        try
        {
            return (engine.eval (code, get_jsr223_context (context)));
        }
        catch (Throwable t)
        {
            return (t);
        }
    }

    @Override
    public CodeEngineProvider getProvider ()
    {
        return (provider);
    }

    @Override
    public void setContext (CodeContext context)
    {
        default_context = context;
        default_context.linkContext (engine.getContext ());
    }

    @Override
    public CodeContext getContext ()
    {
        return (default_context);
    }

    @Override
    public void validate (ManagedObjectInstance instance)
    {
        // Nop
    }

    @Override
    public void invalidate (ManagedObjectInstance instance)
    {
        // Nop
    }
}

// EOF
