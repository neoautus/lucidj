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

import org.lucidj.api.CodeEngine;
import org.lucidj.api.CodeContext;
import org.lucidj.api.CodeEngineManager;
import org.lucidj.api.CodeEngineProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;

@Component (immediate = true, publicFactory = false)
@Instantiate
@Provides
public class BeanShellEngineProvider implements CodeEngineProvider
{
    private final static transient Logger log = LoggerFactory.getLogger (BeanShellEngineProvider.class);

    @Requires
    private CodeEngineManager engineFactory;


    private ContextData init_context (CodeContext context)
    {
        ContextData context_data = context.getObject (ContextData.class);

        if (context_data == null)
        {
            context_data = new ContextData (context);
            context.putObject (ContextData.class, context_data);
        }
        return (context_data);
    }

    @Override
    public CodeEngine newCodeEngine (String shortName, CodeContext context)
    {
        return (new BeanShellEngine (this, init_context (context)));
    }

    @Validate
    private void validate ()
    {
        engineFactory.registerEngineName ("beanshell", this);
        log.info ("BeanShellEngineProvider started.");
    }

    @Invalidate
    private void invalidate ()
    {
        log.info ("BeanShellEngineProvider terminated.");
    }
}

// EOF
