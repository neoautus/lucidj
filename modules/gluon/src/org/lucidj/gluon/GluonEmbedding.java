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

package org.lucidj.gluon;

import com.google.common.io.Files;
import org.lucidj.api.core.EmbeddingHandler;
import org.lucidj.api.core.EmbeddingManager;
import org.lucidj.api.core.Serializer;
import org.lucidj.api.core.SerializerEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Context;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;

@Component (immediate = true, publicFactory = false)
@Instantiate
@Provides
public class GluonEmbedding implements EmbeddingHandler
{
    private final static Logger log = LoggerFactory.getLogger (GluonEmbedding.class);

    private final String HANDLER_PREFIX = "gluon";

    @Requires
    private EmbeddingManager embeddingManager;

    @Requires
    private SerializerEngine serializer;

    @Context
    private BundleContext context;

    @Override
    public String getPrefix ()
    {
        return (HANDLER_PREFIX);
    }

    @Override
    public boolean haveHandler (String name, Object obj)
    {
        return (name != null
                && obj != null
                && name.startsWith ("bundle:")
                && obj instanceof URL
                && Files.getFileExtension (name).equals (HANDLER_PREFIX));
    }

    @Override
    public Object applyHandler (String name, Object obj)
    {
        try
        {
            InputStream prop_is = ((URL)obj).openStream ();
            InputStreamReader prop_isr = new InputStreamReader (prop_is, StandardCharsets.UTF_8);

            Map<String, Object> properties = serializer.getProperties (new BufferedReader (prop_isr));
            boolean some_missing = false;

            if (properties.containsKey (SerializerEngine.EMBEDDED_TYPES))
            {
                String[] embedded_types = (String[])properties.get (SerializerEngine.EMBEDDED_TYPES);

                // Check whether all embedded types are available, so deserialization can succeed
                for (String type: embedded_types)
                {
                    String filter = "(@type=" + type + ")";

                    try
                    {
                        if (context.getServiceReferences (Serializer.class.getName (), filter) != null)
                        {
                            continue;
                        }
                        else
                        {
                            log.warn ("Missing dependency: {}/{}", Serializer.class.getName (), filter);
                        }
                    }
                    catch (InvalidSyntaxException ignore) {};

                    // Either error or missing service
                    some_missing = true;
                }
            }

            if (!some_missing)
            {
                InputStream obj_is = ((URL)obj).openStream ();
                InputStreamReader obj_isr = new InputStreamReader (obj_is, StandardCharsets.UTF_8);
                return (serializer.deserializeObject (new BufferedReader (obj_isr)));
            }
        }
        catch (IOException ignore) {};
        return (null);
    }

    @Validate
    private void validate ()
    {
        embeddingManager.registerHandler (this);
    }
}

// EOF
