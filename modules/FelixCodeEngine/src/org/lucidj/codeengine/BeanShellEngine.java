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

package org.lucidj.codeengine;

import bsh.Interpreter;
import org.lucidj.api.CodeContext;
import org.lucidj.api.CodeEngineBase;
import org.lucidj.api.CodeEngineProvider;
import org.lucidj.api.ManagedObjectInstance;

import java.io.Reader;
import java.io.StringReader;

public class BeanShellEngine implements CodeEngineBase
{
    private CodeEngineProvider provider;
    private CodeContext context;
    private ContextData context_data;
    private Interpreter parent_interpreter;
    private LocalNameSpace local_namespace;

    // TODO: SOLVE METHOD REWRITE
    public BeanShellEngine (CodeEngineProvider provider, ContextData context_data)
    {
        this.provider = provider;
        this.context_data = context_data;

        // TODO: THIS HIERARCHICAL DATA BELONGS TO CodeBindings
        this.parent_interpreter = context_data.getRootInterpreter ();
        this.local_namespace = context_data.getLocalNameSpace ();
    }

    @Override
    public CodeEngineProvider getProvider ()
    {
        return (provider);
    }

    @Override
    public void setContext (CodeContext context)
    {
        this.context = context;
    }

    @Override
    public CodeContext getContext ()
    {
        return (context);
    }

    @Override
    public Object eval (Reader reader, CodeContext context)
    {
        // Set this BeanShell reference
        //local_namespace.setBeanShell (parent);

        try
        {
            return (parent_interpreter.eval (reader, context.getStdout (), context.getStderr (),
                                             local_namespace, context.getContextId ()));
        }
        catch (Throwable t)
        {
            // It's valid to return the throwable object
            return (t);
        }
    }

    @Override
    public Object eval (String code, CodeContext context)
    {
        return (eval (new StringReader (code + ";"), context));
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
