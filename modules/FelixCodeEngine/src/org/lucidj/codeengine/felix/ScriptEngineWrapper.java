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

import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import java.io.Reader;

public class ScriptEngineWrapper implements CodeEngineBase
{
    private CodeEngineProvider provider;
    private CodeContext code_context;
    private ScriptEngine script_engine;      // The wrapped JSR223 engine
    private ScriptContext script_context;    // The wrapped JSR223 context

    public ScriptEngineWrapper (CodeEngineProvider provider, ScriptEngine script_engine)
    {
        this.provider = provider;
        this.script_engine = script_engine;
    }

    @Override
    public Object eval (Reader code, CodeContext context)
    {
        try
        {
            // TODO: SET CLASSLOADER FROM CodeContext?
            return (script_engine.eval (code, script_context));
        }
        catch (Throwable t)
        {
            return (t);
        }
    }

    @Override
    public Object eval (String code, CodeContext context)
    {
        try
        {
            // TODO: SET CLASSLOADER FROM CodeContext?
            return (script_engine.eval (code, script_context));
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
        code_context = context;
        script_context = context.wrapContext (script_engine.getContext ());
    }

    @Override
    public CodeContext getContext ()
    {
        return (code_context);
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
