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

package org.lucidj.beanshell;

import bsh.Interpreter;
import bsh.NameSpace;
import bsh.UtilEvalError;
import bsh.Variable;
import org.lucidj.api.CodeContext;
import org.lucidj.api.CodeEngineBase;
import org.lucidj.api.CodeEngineProvider;
import org.lucidj.api.ManagedObjectInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Reader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

public class BeanShellEngine implements CodeEngineBase
{
    private final static Logger log = LoggerFactory.getLogger (BeanShellEngineProvider.class);

    private CodeEngineProvider provider;
    private CodeContext context;
    private Interpreter parent_interpreter;
    private LocalNameSpace local_namespace;

    // TODO: SOLVE METHOD REWRITE
    public BeanShellEngine (CodeEngineProvider provider, Interpreter parent_interpreter)
    {
        this.provider = provider;
        this.parent_interpreter = parent_interpreter;
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
        local_namespace = new LocalNameSpace (parent_interpreter.getNameSpace ());
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

    public class LocalNameSpace extends NameSpace
    {
        private Map<String, Variable> var_map = new HashMap<> ();

        public LocalNameSpace (NameSpace parent)
        {
            super (parent, "beanshell");
        }

        @Override
        protected Variable getVariableImpl (String name, boolean recurse)
            throws UtilEvalError
        {
            log.info ("getVariableImpl(name={}, recurse={}) IN", name, recurse);

            // We have precedence here for both speed and scope
            Variable v = super.getVariableImpl (name, recurse);

            log.info ("getVariableImpl(name={}, recurse={}) LOOKUP = {}", name, recurse, v);

            // This
            if (v == null)
            {
                log.info ("getVariableImpl(name={}, recurse={}) DYNAMIC", name, recurse);

                if (var_map.containsKey (name))
                {
                    // From cache
                    v = var_map.get (name);
                }
                else // Try to retrieve from services
                {
                    Object obj = context.getServiceObject (name);

                    if (obj != null)
                    {
                        v = createVariable(name, obj, null/*modifiers*/);
                        var_map.put (name, v);
                    }
                }
            }

            log.info ("getVariableImpl(name={}, recurse={}) OUT = {}", name, recurse, v);
            return (v);
        }
    }
}

// EOF
