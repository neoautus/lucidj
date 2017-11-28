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

package org.lucidj.mappingclassmanager;

import org.lucidj.api.core.ClassManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Context;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

@Component (immediate = true, publicFactory = false)
@Instantiate
@Provides (strategy="SERVICE")
public class MappingClassManager implements ClassManager
{
    private final static Logger log = LoggerFactory.getLogger (MappingClassManager.class);

    private List<Bundle> source_bundles = new ArrayList<> ();
    private Map<String, Class> class_cache = new HashMap<> ();
    private Map<String, Bundle> package_to_bundle = new HashMap<> ();
    private ClassLoader class_loader = new MappingClassLoader ();

    @Context
    private BundleContext context;

    private Class locate_and_load_class (String name)
    {
        log.info ("===> loadClass {} from {}", name, this);

        // Shortcut
        if (class_cache.containsKey (name))
        {
            Class cls = class_cache.get(name);
            log.info ("<==CACHE loadClass {} = {}", name, cls);
            return (cls);
        }

        // First try our own bundle classloader
        try
        {
            // We do not cache local classloader
            Class cls = getClass ().getClassLoader ().loadClass (name);
            log.info ("<==LCLD loadClass {} = {} from {}", name, cls);
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
                log.info ("<==CBND loadClass {} = {}", name, cls);
                return (cls);
            }
            catch (ClassNotFoundException ignore) {};
        }

        // Go looking around
        for (Bundle attempt: context.getBundles())
        {
            // We don't care trying again on source_bundles
            try
            {
                Class<?> cls = attempt.loadClass (name);
                source_bundles.add (attempt);
                class_cache.put (name, cls);
                log.info ("<==GBND loadClass {} = {}", name, cls);
                return (cls);
            }
            catch (ClassNotFoundException ignore) {};
        }

        log.info ("<!!! loadClass: Not found {}", name);

        // Nothing found
        return (null);
    }

    @Override
    public Class loadClass (String name)
    {
        return (locate_and_load_class (name));
    }

    @Override
    public Class loadClassUsingClass (Class clazz, String name)
    {
        try
        {
            // We're keeping track of all used package/bundle pairs
            Bundle bnd = FrameworkUtil.getBundle (clazz);
            Class cls = bnd.loadClass (name);
            package_to_bundle.put (cls.getPackage ().getName (), bnd);
            return (cls);
        }
        catch (NullPointerException | ClassNotFoundException e)
        {
            return (null);
        }
    }

    @Override
    public Class loadClassUsingObject (Object obj, String name)
    {
        return (loadClassUsingClass (obj.getClass (), name));
    }

    @Override
    public Map<String, Bundle> getPackageMap ()
    {
        return (package_to_bundle);
    }

    @Override
    public ClassLoader getClassLoader ()
    {
        return (class_loader);
    }

    public class MappingClassLoader extends ClassLoader
    {
//        public QuarkClassLoader (BundleContext bundle_context)
//        {
//            ctx = bundle_context;
//            bnd = bundle_context.getBundle ();
//            cld = bnd.adapt(BundleWiring.class).getClassLoader();
//        }

        public Class loadClass(String name)
            throws ClassNotFoundException
        {
            Class cls = locate_and_load_class (name);

            if (cls != null)
            {
                return (cls);
            }
            else
            {
                throw (new ClassNotFoundException ("Class " + name + " not found on any bundle"));
            }
        }
    }
}

// EOF
