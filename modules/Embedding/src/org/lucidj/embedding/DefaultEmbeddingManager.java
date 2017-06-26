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

import org.lucidj.api.EmbeddingContext;
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
    private List<EmbeddingListener> listener_list = new ArrayList<> ();

    @Context
    private BundleContext ctx;

    // TODO: WE NEED SOME CONSTRUCT TO WAIT FOR A SPECIFIC SERVICE/HANDLER

    @Override
    public EmbeddingContext newEmbeddingContext (Bundle bnd)
    {
        return (new DefaultEmbeddingContext (this, bnd));
    }

    @Override
    public void registerHandler (EmbeddingHandler handler)
    {
        log.info ("Adding embedded handler: {}", handler.getPrefix ());
        embedding_handlers.add (handler);

        // Notify the listeners of the new handler
        for (EmbeddingListener listener: listener_list)
        {
            listener.addingHandler (handler);
        }
    }

    @Override
    public EmbeddingHandler[] getHandlers (String name, Object obj)
    {
        List<EmbeddingHandler> found_handlers = new ArrayList<> ();

        for (EmbeddingHandler handler: embedding_handlers)
        {
            if (handler.haveHandler (name, obj))
            {
                found_handlers.add (handler);
            }
        }
        return (found_handlers.toArray (new EmbeddingHandler [found_handlers.size ()]));
    }

    @Override
    public void addListener (EmbeddingListener listener)
    {
        listener_list.add (listener);
    }

    @Override
    public void removeListener (EmbeddingListener listener)
    {
        listener_list.remove (listener);
    }

    private void clear_components_by_bundle (Bundle bnd)
    {
        // First we remove all handlers belonging to the gone bundle, while
        // also creating a listing of them. This list will be used to notify
        // the remaining listeners.
        List<EmbeddingHandler> removed_handlers = new ArrayList<> ();
        Iterator<EmbeddingHandler> ith = embedding_handlers.iterator ();

        while (ith.hasNext ())
        {
            EmbeddingHandler handler = ith.next ();

            if (bnd.equals (FrameworkUtil.getBundle (handler.getClass ())))
            {
                log.info ("Removing embedded handler: {}", handler.getPrefix ());
                removed_handlers.add (handler);
                ith.remove ();
            }
        }

        // Now we remove all listeners beloging to the gone bundle, while
        // also notifying all the remaining listeners.
        Iterator<EmbeddingListener> itl = listener_list.iterator ();

        while (itl.hasNext ())
        {
            EmbeddingListener listener = itl.next ();

            if (bnd.equals (FrameworkUtil.getBundle (listener.getClass ())))
            {
                // This listener is being deactivated, just remove
                itl.remove ();
            }
            else
            {
                // This listener is valid, so notify it of any departing handler
                for (EmbeddingHandler handler: removed_handlers)
                {
                    listener.removingHandler (handler);
                }
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
