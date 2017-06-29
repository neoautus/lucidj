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

package org.lucidj.pkgdeployer;

import org.lucidj.api.Artifact;
import org.lucidj.api.Embedding;
import org.lucidj.api.EmbeddingContext;
import org.lucidj.api.EmbeddingHandler;
import org.lucidj.api.EmbeddingManager;
import org.lucidj.api.Package;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutionException;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;

public class PackageImpl implements EmbeddingManager.EmbeddingListener, Package, ServiceListener, Runnable
{
    private final static transient Logger log = LoggerFactory.getLogger (PackageImpl.class);

    private Bundle bnd;
    private BundleContext context;
    private ServiceReference local_reference;
    private Thread local_thread;
    private volatile int extended_state;

    private EmbeddingManager embeddingManager;
    private EmbeddingContext embedding_context;

    public PackageImpl (Bundle bnd, EmbeddingManager embeddingManager)
    {
        this.bnd = bnd;

        this.embeddingManager = embeddingManager;
        this.embeddingManager.addListener (this);

        embedding_context = embeddingManager.newEmbeddingContext (bnd);

        context = bnd.getBundleContext ();
        context.addServiceListener (this);
    }

    public void setState (int state)
    {
        extended_state = state;
    }

    private String get_state_str (Bundle bundle)
    {
        switch (bundle.getState ())
        {
            case Bundle.INSTALLED:   return ("INSTALLED");
            case Bundle.RESOLVED:    return ("RESOLVED");
            case Bundle.STARTING:    return ("STARTING");
            case Bundle.STOPPING:    return ("STOPPING");
            case Bundle.ACTIVE:      return ("ACTIVE");
            case Bundle.UNINSTALLED: return ("UNINSTALLED");
        }
        return ("Unknown");
    }

    @Override // Package
    public Bundle getBundle ()
    {
        return (bnd);
    }

    @Override // Package
    public int getState ()
    {
        int bundle_state = bnd.getState ();

        // Extended states are non-zero and derive from valid ACTIVE states
        if (bundle_state == Bundle.ACTIVE && extended_state != 0)
        {
            return (extended_state);
        }
        return (bundle_state);
    }

    @Override // Package
    public EmbeddingContext getEmbeddingContext ()
    {
        return (embedding_context);
    }

    private void do_opening_transition ()
    {
        // Assert the state...
        extended_state = Artifact.STATE_EX_OPENING;

        // Prepare and load embeddings context
        try
        {
            embedding_context.updateEmbeddings ().get ();
        }
        catch (InterruptedException | ExecutionException e)
        {
            log.error ("Exception opening bundle: {}", bnd, e);
        }

        // RETRIEVE ALL ACTIVE EMBEDDINGS/FILES AND PRINT THEM
        for (Embedding file: embedding_context.getEmbeddedFiles ())
        {
            log.info ("Embedding: [{}] -> {}", file.getName (), file.getObject ());

            for (Embedding embedding: embedding_context.getEmbeddings (file))
            {
                log.info ("Embedding: [{}] {} -> {}", file.getName (), embedding.getName (), embedding.getObject ());
            }
        }

        // After Opening, we are Running
        extended_state = Artifact.STATE_EX_OPEN;
    }

    private void do_closing_transition ()
    {
        extended_state = Artifact.STATE_EX_CLOSING;

        // Incredibly complex things....

        // Back to pure OSGi state
        extended_state = 0;
    }

    @Override
    public void run ()
    {
        try
        {
            log.info ("Starting package {} (state = {})", bnd, get_state_str (bnd));

            do_opening_transition ();

            if (extended_state != 0)
            {
                log.info ("Package {} started (state = {})", bnd, get_state_str (bnd));
            }
            else
            {
                log.info ("Package {} not started (state = {})", bnd, get_state_str (bnd));
            }
        }
        catch (Throwable transition_exception)
        {
            log.error ("Unhandled exception transitioning states", transition_exception);
        }
    }

    @Override
    public void serviceChanged (ServiceEvent serviceEvent)
    {
        ServiceReference reference = serviceEvent.getServiceReference ();

        if (local_reference == null)
        {
            if (serviceEvent.getType () == ServiceEvent.REGISTERED
                && reference.getBundle () == bnd)
            {
                if (context.getService (reference) == this)
                {
                    local_reference = reference;

                    local_thread = new Thread (this);
                    local_thread.setName ("P/" + bnd.getSymbolicName () + "/" + bnd.getVersion ());
                    local_thread.start ();
                }
                context.ungetService (reference);
            }
        }
        else if (reference == local_reference)
        {
            if (serviceEvent.getType () == ServiceEvent.UNREGISTERING)
            {
                log.info ("=====> THIS SERVICE IS UNREGISTERING: reference={}", local_reference);
            }
        }
    }

    @Override
    public void addingHandler (EmbeddingHandler handler)
    {

    }

    @Override
    public void removingHandler (EmbeddingHandler handler)
    {
        // Do nothing for now
    }
}

// EOF
