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

import org.lucidj.api.Embedding;
import org.lucidj.api.EmbeddingContext;
import org.lucidj.api.EmbeddingHandler;
import org.lucidj.api.EmbeddingManager;
import org.lucidj.api.ManagedObject;
import org.lucidj.api.ManagedObjectInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.osgi.framework.Bundle;

public class DefaultEmbeddingContext implements EmbeddingContext, ManagedObject, EmbeddingManager.EmbeddingListener
{
    private final static transient Logger log = LoggerFactory.getLogger (DefaultEmbeddingContext.class);

    private List<EmbeddingImpl> embedded_files = new ArrayList<> ();
    private ExecutorService background = Executors.newSingleThreadExecutor ();

    private EmbeddingManager embeddingManager;
    private String artifact_source;
    private Bundle bundle;

    public DefaultEmbeddingContext (EmbeddingManager embeddingManager, String artifact_source, Bundle bundle)
    {
        this.bundle = bundle;
        this.embeddingManager = embeddingManager;
        this.artifact_source = artifact_source;

        // Find all bundle entries and add them to context file list
        for (Enumeration<URL> entries = bundle.findEntries ("/", null, true); entries.hasMoreElements (); )
        {
            URL file = entries.nextElement ();
            embedded_files.add (new EmbeddingImpl (file.toString (), file));
        }

        // Now we can get asynchronous notifications
        embeddingManager.addListener (this);
    }

    private boolean contains_object_name (LinkedList<EmbeddingImpl> list, String name)
    {
        for (EmbeddingImpl embedding: list)
        {
            if (embedding.getName ().equals (name))
            {
                return (true);
            }
        }
        return (false);
    }

    private boolean apply_handlers (String name, Object object, LinkedList<EmbeddingImpl> embedding_queue)
    {
        boolean missing_dependencies = false;
        EmbeddingHandler[] handler_list = embeddingManager.getHandlers (name, object);

        for (EmbeddingHandler handler: handler_list)
        {
            String new_embedding_name = handler.getPrefix () + ":" + name;

            // Check first if the handler was already applied to the object
            if (!contains_object_name (embedding_queue, new_embedding_name))
            {
                log.info ("Applying '{}' handler to {}", handler.getPrefix (), object);
                Object new_object = handler.applyHandler (name, object);

                if (new_object != null)
                {
                    embedding_queue.add (new EmbeddingImpl (new_embedding_name, new_object));
                }
                else
                {
                    missing_dependencies = true;
                }
            }
        }
        return (!missing_dependencies);
    }

    private boolean update_file_embeddings (EmbeddingImpl file_embedding)
    {
        LinkedList<EmbeddingImpl> embedding_queue = file_embedding.getChildren ();
        int current_embedding_pos = 0;
        boolean all_updated = true;

        // Apply the first pass on the file
        all_updated &= apply_handlers (file_embedding.getName (), file_embedding.getObject (), embedding_queue);

        // Cycle the created embeddings until no more embeddings are created on the queue
        while (current_embedding_pos < embedding_queue.size ())
        {
            // Fetch the current embedding and apply all handlers
            EmbeddingImpl current_embedding = embedding_queue.get (current_embedding_pos++);
            all_updated &= apply_handlers (current_embedding.getName (), current_embedding.getObject (), embedding_queue);
        }
        return (all_updated);
    }

    private boolean update_all_file_embeddings ()
    {
        boolean all_updated;
        int delay = 1000;
        int retries = 10;

        do
        {
            all_updated = true;

            // Scan all available files/embeddings for new embeddings
            for (EmbeddingImpl embedded_file: embedded_files)
            {
                if (!update_file_embeddings (embedded_file))
                {
                    log.warn ("Missing dependencies found for: {}", embedded_file.getName ());
                    all_updated = false;
                }
            }

            if (all_updated)
            {
                break;
            }

            try
            {
                log.warn ("Missing dependencies found on {}, will retry update embeddings in {}ms", bundle, delay);
                Thread.sleep (delay);
            }
            catch (Exception ignore) {};
        }
        while (--retries > 0);

        if (!all_updated)
        {
            log.error ("Missing dependencies on {}, will NOT retry update", bundle);
        }
        return (all_updated);
    }

    @Override // EmbeddingContext
    public Future updateEmbeddings ()
    {
        // The Future might be used to wait
        return (background.submit ((Runnable)() ->
        {
            update_all_file_embeddings ();
        }));
    }

    @Override // EmbeddingContext
    public Future addFile (URL file)
    {
        return (background.submit ((Runnable)() ->
        {
            // Create the new embedding and update its handlers
            EmbeddingImpl file_embedding = new EmbeddingImpl (file.toString (), file);
            embedded_files.add (file_embedding);
            update_file_embeddings (file_embedding);
        }));
    }

    @Override // EmbeddingContext
    public Future removeFile (URL file)
    {
        // TODO: CLOSE THE AFFECTED EMBEDDINGS. USE MANAGED OBJECTS??
        return (null);
    }

    @Override // EmbeddingContext
    public List<Embedding> getEmbeddedFiles ()
    {
        return (Collections.unmodifiableList (embedded_files));
    }

    @Override
    public File getWritableFile (Embedding embedded_file)
    {
        if (embedded_file instanceof EmbeddingImpl)
        {
            EmbeddingImpl e = (EmbeddingImpl)embedded_file;

            if (e.getObject () instanceof URL)
            {
                try
                {
                    URL file_url = (URL)e.getObject ();
                    File artifact_source_file = new File (new URI (artifact_source));

                    if (artifact_source_file.isDirectory ())
                    {
                        // Returns the base dir plus the embedding
                        return (new File (artifact_source_file, file_url.getPath ()));
                    }
                }
                catch (Exception ignore) {};
            }
        }
        return (null);
    }

    @Override // EmbeddingContext
    public List<Embedding> getEmbeddings (Embedding embedded_file)
    {
        if (embedded_file instanceof EmbeddingImpl)
        {
            return (Collections.unmodifiableList (((EmbeddingImpl)embedded_file).getChildren ()));
        }
        return (Collections.emptyList ());
    }

    @Override // EmbeddingManager.EmbeddingListener
    public void addingHandler (EmbeddingHandler handler)
    {
        // Apply and refresh all embeddings, since we may have dependencies between embeddings
        log.info ("*** Updating embeddings from {}", bundle);
        updateEmbeddings ();
    }

    @Override // EmbeddingManager.EmbeddingListener
    public void removingHandler (EmbeddingHandler handler)
    {
        // Nop
    }

    @Override // ManagedObject
    public void validate (ManagedObjectInstance instance)
    {
        // Nop
    }

    @Override // ManagedObject
    public void invalidate (ManagedObjectInstance instance)
    {
        // TODO: GRACEFUL OR NOT...?
        background.shutdownNow ();
    }
}

// EOF
