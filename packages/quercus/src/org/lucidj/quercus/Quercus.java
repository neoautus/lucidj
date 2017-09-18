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

package org.lucidj.quercus;

import org.lucidj.api.CodeEngineManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Context;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;

@Component (immediate = true)
@Instantiate
public class Quercus
{
    private final static Logger log = LoggerFactory.getLogger (Quercus.class);

    @Requires
    private CodeEngineManager engineManager;

    @Context
    private BundleContext context;

    @Validate
    private void validate ()
    {
        // We need to use our own bundle classloader to look for Script Engines
        ScriptEngineManager sem = new ScriptEngineManager (this.getClass ().getClassLoader ());

        Bundle this_bundle = context.getBundle ();

        // Cycle all available factories
        for (ScriptEngineFactory factory: sem.getEngineFactories ())
        {
            // Register only the factories belonging to this bundle
            if (FrameworkUtil.getBundle (factory.getClass ()).equals (this_bundle))
            {
                String[] name_list = factory.getNames ().toArray (new String [0]);
                log.info ("Registering {} {} {}", factory.getEngineName (), factory.getEngineVersion (), name_list);
                engineManager.registerEngine (factory);
            }
        }
    }
}

// EOF
