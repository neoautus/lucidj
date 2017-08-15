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

package org.lucidj.codeengine.felix;

import org.lucidj.api.ClassManager;
import org.lucidj.api.CodeContext;
import org.lucidj.api.CodeEngine;
import org.lucidj.api.CodeEngineBase;
import org.lucidj.api.CodeEngineManager;
import org.lucidj.api.CodeEngineProvider;
import org.lucidj.api.ServiceBindingsManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptEngineFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.FrameworkUtil;
import org.osgi.util.tracker.BundleTracker;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Context;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;

@Component (immediate = true, publicFactory = false)
@Instantiate
@Provides
public class FelixCodeEngineManager implements CodeEngineManager
{
    private final static transient Logger log = LoggerFactory.getLogger (FelixCodeEngineManager.class);

    private BundleTracker bundle_cleaner;

    private final Map<String, CodeEngineProvider> name_to_provider = new HashMap<> ();

    @Context
    private BundleContext ctx;

    @Requires
    ServiceBindingsManager bindingsManager;

    @Requires
    ClassManager classManager;

    @Validate
    private void validate ()
    {
        log.info ("FelixCodeEngineManager started.");
        bundle_cleaner = new BundleCleanup (ctx);
        bundle_cleaner.open ();
    }

    @Invalidate
    private void invalidate ()
    {
        bundle_cleaner.close ();
        bundle_cleaner = null;
        log.info ("FelixCodeEngineManager terminated.");
    }

    @Override
    public CodeContext newContext (Bundle parentBundle)
    {
        // TODO: STORE CREATED CONTEXT FOR FURTHER QUERIES
        return (new FelixCodeEngineContext (parentBundle, bindingsManager));
    }

    @Override
    public void registerEngineName (String shortName, CodeEngineProvider provider)
    {
        name_to_provider.put (shortName, provider);
    }

    @Override
    public void registerEngine (ScriptEngineFactory factory)
    {
        String[] name_list = factory.getNames ().toArray (new String [0]);

        log.info ("Registering script engine '{} {}' providing {}",
            factory.getEngineName (), factory.getEngineVersion (), name_list);

        // Just one factory for all names
        CodeEngineProvider factory_wrapper = new ScriptEngineFactoryWrapper (factory);

        for (String name: name_list)
        {
            registerEngineName (name, factory_wrapper);
        }
    }

    @Override
    public Set<String> getEngines ()
    {
        return (name_to_provider.keySet ());
    }

    @Override
    public CodeEngine getEngineByName (String shortName)
    {
        if (name_to_provider.containsKey (shortName))
        {
            // Create engine instance
            CodeEngineProvider provider = name_to_provider.get (shortName);
            CodeContext context = newContext (FrameworkUtil.getBundle (provider.getClass ()));
            CodeEngineBase new_engine = provider.newCodeEngine (shortName, context);
            CodeEngine full_engine;

            if (new_engine instanceof CodeEngine)
            {
                // Engine is fully featured
                full_engine = (CodeEngine)new_engine;
            }
            else
            {
                // Upgrade base engine to full engine
                full_engine = new CodeEngineThreading (new_engine);
            }

            // Set default bundle/service-aware classloader
            context.setClassLoader (classManager.getClassLoader ());

            // Default context and optional JSR223 context wrapping
            full_engine.setContext (context);
            return (full_engine);
        }
        return (null);
    }

    private void clear_components_by_bundle (Bundle provider_bundle)
    {
        log.info ("clear_components_by_bundle (provider_bundle={})", provider_bundle);
    }

    class BundleCleanup extends BundleTracker
    {
        BundleCleanup (BundleContext context)
        {
            super (context, Bundle.ACTIVE, null);
        }

        @Override
        public void removedBundle (Bundle bundle, BundleEvent event, Object object)
        {
            clear_components_by_bundle (bundle);
        }
    }
}

// EOF
