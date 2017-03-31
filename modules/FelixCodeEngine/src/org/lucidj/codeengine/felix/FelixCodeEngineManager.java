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

import org.lucidj.api.CodeEngine;
import org.lucidj.api.CodeEngineContext;
import org.lucidj.api.CodeEngineManager;
import org.lucidj.api.CodeEngineProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.util.tracker.BundleTracker;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Context;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Provides;
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

    private void clear_components_by_bundle (Bundle provider_bundle)
    {
        log.info ("clear_components_by_bundle (provider_bundle={})", provider_bundle);
    }

    @Override
    public CodeEngineContext newContext (Bundle parentBundle)
    {
        // TODO: STORE CREATED CONTEXT FOR FURTHER QUERIES
        return (new FelixCodeEngineContext (parentBundle, this));
    }

    @Override
    public void registerEngineName (String shortName, CodeEngineProvider provider)
    {
        name_to_provider.put (shortName, provider);
    }

    public CodeEngine _getEngineByName (String shortName, CodeEngineContext context)
    {
        if (name_to_provider.containsKey (shortName))
        {
            CodeEngineProvider provider = name_to_provider.get (shortName);
            return (provider.newCodeEngine (shortName, context));
        }
        return (null);
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
