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

package org.lucidj.quark;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.wiring.BundleWiring;

public class QuarkClassLoader extends ClassLoader
{
    private final transient Logger log = LoggerFactory.getLogger (QuarkClassLoader.class);

    private BundleContext ctx;
    private Bundle bnd;
    private ClassLoader cld;

    private List<Bundle> source_bundles = new ArrayList<> ();
    private Map<String, Class> class_cache = new HashMap<> ();

    public QuarkClassLoader (BundleContext bundle_context)
    {
        ctx = bundle_context;
        bnd = bundle_context.getBundle ();
        cld = bnd.adapt(BundleWiring.class).getClassLoader();
    }

    public Class loadClass(String name)
            throws ClassNotFoundException
    {
        log.debug ("===> loadClass {} from {}", name, this);

        if (class_cache.containsKey (name))
        {
            Class cls = class_cache.get(name);
            log.debug ("<==CACHE loadClass {} = {}", name, cls);
            return (cls);
        }

        // First try the bundle classloader
        try
        {
            // We do not cache local classloader
            Class cls = cld.loadClass (name);
            log.debug ("<==LCLD loadClass {} = {} from {}", name, cls, cld);
            return (cls);
        }
        catch (ClassNotFoundException ignore) {};

        // Try the bundles we already used
        for (Bundle source: source_bundles)
        {
            try
            {
                Class<?> cls = source.loadClass (name);
                class_cache.put (name, cls);
                log.debug ("<==CBND loadClass {} = {}", name, cls);
                return (cls);
            }
            catch (ClassNotFoundException ignore) {};
        }

        // Go looking around
        for (Bundle attempt: ctx.getBundles())
        {
            // We don't care trying again on source_bundles
            try
            {
                Class<?> cls = attempt.loadClass (name);
                source_bundles.add (attempt);
                class_cache.put (name, cls);
                log.debug ("<==GBND loadClass {} = {}", name, cls);
                return (cls);
            }
            catch (ClassNotFoundException ignore) {};
        }

        log.debug ("<!!! loadClass: Not found {}", name);

        // Nothing found
        return (null);
    }
}

// EOF
