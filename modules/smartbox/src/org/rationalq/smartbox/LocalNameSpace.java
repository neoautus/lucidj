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

import bsh.NameSpace;
import bsh.UtilEvalError;
import bsh.Variable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

public class LocalNameSpace extends NameSpace
{
    private final transient Logger log = LoggerFactory.getLogger (LocalNameSpace.class);

    private static ThreadLocal<BeanShell> parent_beanshell = new ThreadLocal<> ();
    private static ThreadLocal<HashMap<String, Variable>> dynamic_vars = new ThreadLocal<> ();

    public LocalNameSpace (NameSpace parent, String name)
    {
        super (parent, name);
    }

    public void setBeanShell (BeanShell parent)
    {
        parent_beanshell.set (parent);
    }

    @Override
    protected Variable getVariableImpl(String name, boolean recurse)
        throws UtilEvalError
    {
        log.debug ("getVariableImpl(name={}, recurse={}) IN", name, recurse);

        // We have precedence here for both speed and scope
        Variable v = super.getVariableImpl (name, recurse);

        log.trace ("getVariableImpl(name={}, recurse={}) LOOKUP = {}", name, recurse, v);

        // This
        if (v == null)
        {
            log.trace ("getVariableImpl(name={}, recurse={}) DYNAMIC", name, recurse);
            HashMap<String, Variable> var_map = dynamic_vars.get ();

            if (var_map == null)
            {
                dynamic_vars.set (new HashMap<String, Variable> ());
            }
            else if (var_map.containsKey (name))
            {
                v = var_map.get (name);
            }
            else
            {
                BeanShell bsl = parent_beanshell.get ();

                log.trace ("parent_beanshell = {}", parent_beanshell.get ());

                try
                {
                    Object obj = bsl.dynamicVariableLookup (name);
                    v = createVariable(name, obj, null/*modifiers*/);
                    var_map.put (name, v);
                }
                catch (Exception ignore) {}; // v stays null
            }
        }

        log.debug ("getVariableImpl(name={}, recurse={}) OUT = {}", name, recurse, v);
        return (v);
    }
}

// EOF
