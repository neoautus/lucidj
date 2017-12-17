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

package org.lucidj.vaadinweaving;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.hooks.weaving.WeavingHook;

public class VaadinWeavingActivator implements BundleActivator
{
    private final static Logger log = LoggerFactory.getLogger (VaadinWeavingActivator.class);

    private final List<ServiceRegistration<?>> reg_list = new ArrayList<> ();

    @Override
    public void start (BundleContext context) throws Exception
    {
        addHook (context, new VaadinWeaving ());
    }

    @Override
    public void stop (BundleContext context)
        throws Exception
    {
        for (ServiceRegistration<?> reg : reg_list)
        {
            reg.unregister ();
        }
        reg_list.clear ();
    }

    private void addHook (BundleContext context, WeavingHook hook)
    {
        log.info ("Registering WeavingHook {}", hook);
        reg_list.add (context.registerService (WeavingHook.class.getName (), hook, null));
    }

}

// EOF
