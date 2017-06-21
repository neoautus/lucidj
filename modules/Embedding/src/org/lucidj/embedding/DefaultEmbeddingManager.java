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

package org.lucidj.embedding;

import org.lucidj.api.EmbeddingHandler;
import org.lucidj.api.EmbeddingManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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
import org.apache.felix.ipojo.annotations.Validate;

@Component (immediate = true, publicFactory = false)
@Instantiate
@Provides
public class DefaultEmbeddingManager implements EmbeddingManager
{
    private final transient static Logger log = LoggerFactory.getLogger (DefaultEmbeddingManager.class);

    private BundleTracker bundle_cleaner;
    private List<EmbeddingHandler> embedding_handlers = new ArrayList<> ();

    @Context
    private BundleContext ctx;

    // TODO: WE NEED SOME CONSTRUCT TO WAIT FOR A SPECIFIC SERVICE/HANDLER

    @Override
    public void registerProvider (EmbeddingHandler embeddingHandler)
    {
        log.info ("Adding embedded handler: {}", embeddingHandler.getPrefix ());
        embedding_handlers.add (embeddingHandler);
    }

    private EmbeddingHandler find_handler (String name, Object obj)
    {
        for (EmbeddingHandler handler: embedding_handlers)
        {
            if (handler.haveHandler (name, obj))
            {
                return (handler);
            }
        }
        return (null);
    }

    @Override
    public String getHandler (String name, Object obj)
    {
        EmbeddingHandler handler = find_handler (name, obj);
        return (handler != null? handler.getPrefix (): null);
    }

    @Override
    public Object applyHandler (String name, Object obj)
    {
        EmbeddingHandler handler = find_handler (name, obj);
        return ((handler == null)? null: handler.applyHandler (name, obj));
    }

    private void clear_components_by_bundle (Bundle bnd)
    {
        Iterator<EmbeddingHandler> it = embedding_handlers.iterator ();

        while (it.hasNext ())
        {
            EmbeddingHandler handler = it.next ();
            Bundle handler_bundle = FrameworkUtil.getBundle (handler.getClass ());

            if (handler_bundle.equals (bnd))
            {
                log.info ("Removing embedded handler: {}", handler.getPrefix ());
                it.remove ();
            }
        }
    }

    @Validate
    private void validate ()
    {
        bundle_cleaner = new BundleCleanup (ctx);
        bundle_cleaner.open ();
    }

    @Invalidate
    private void invalidate ()
    {
        bundle_cleaner.close ();
        bundle_cleaner = null;
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
