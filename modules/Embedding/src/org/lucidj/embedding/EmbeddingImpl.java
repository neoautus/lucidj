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

import org.lucidj.api.core.Embedding;

import java.util.LinkedList;

public class EmbeddingImpl implements Embedding
{
    private String name;
    private Object object;
    private LinkedList<EmbeddingImpl> children = null;

    public EmbeddingImpl (String name, Object object)
    {
        this.name = name;
        this.object = object;
    }

    @Override
    public String getName ()
    {
        return (name);
    }

    @Override
    public Object getObject ()
    {
        return (object);
    }

    public LinkedList<EmbeddingImpl> getChildren ()
    {
        if (children == null)
        {
            children = new LinkedList<> ();
        }
        return (children);
    }
}

// EOF
