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

package org.lucidj.vaadin;

import org.lucidj.api.CodeContext;
import org.lucidj.api.ServiceBinding;
import org.lucidj.api.ServiceBindingsManager;
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
public class VaadinBinding implements ServiceBinding
{
    private final static transient Logger log = LoggerFactory.getLogger (VaadinBinding.class);

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
        return ("Vaadin");
    }

    @Override
    public Object getService (CodeContext context)
    {
        log.debug ("====>> getService (context={})", context);

        Vaadin vaadin = null;

        synchronized (context)
        {
            if ((vaadin = context.getObject (Vaadin.class)) == null)
            {
                vaadin = new Vaadin ();
                context.putObject (Vaadin.class, vaadin);
            }
        }
        log.debug ("====>> getService (context={}) => {}", context, vaadin);
        return (vaadin);
    }
}

// EOF
