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
import org.lucidj.api.EmbeddingHandler;
import org.lucidj.api.EmbeddingManager;
import org.lucidj.api.SerializerEngine;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;

@Component (immediate = true, publicFactory = false)
@Instantiate
@Provides
public class GluonEmbedding implements EmbeddingHandler
{
    private final String HANDLER_PREFIX = "gluon";

    @Requires
    private EmbeddingManager embeddingManager;

    @Requires
    private SerializerEngine serializer;

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
            InputStream is = ((URL)obj).openStream ();
            InputStreamReader isr = new InputStreamReader (is, StandardCharsets.UTF_8);
            return (serializer.deserializeObject (new BufferedReader (isr)));
        }
        catch (IOException e)
        {
            return (null);
        }
    }

    @Validate
    private void validate ()
    {
        embeddingManager.registerProvider (this);
    }
}

// EOF
