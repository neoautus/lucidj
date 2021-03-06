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

package org.lucidj.console;

import org.lucidj.api.core.CodeContext;
import org.lucidj.api.core.ServiceBinding;
import org.lucidj.api.core.ServiceBindingsManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;

@Component (immediate = true, publicFactory = false)
@Instantiate
@Provides
public class ConsoleBinding implements ServiceBinding
{
    private final static Logger log = LoggerFactory.getLogger (ConsoleBinding.class);

    @Requires
    ServiceBindingsManager bindings;

    @Validate
    private void validate ()
    {
        bindings.register (this);
    }

    @Override
    public String getBindingName ()
    {
        return ("Console");
    }

    @Override
    public Object getService (CodeContext context)
    {
        log.info ("====>> getService (context={})", context);
        return (System.out);
    }
}

// EOF
