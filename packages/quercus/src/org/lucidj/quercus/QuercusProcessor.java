/*
 * Copyright 2018 NEOautus Ltd. (http://neoautus.com)
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

package org.lucidj.quercus;

import org.lucidj.api.stddef.Aggregate;
import org.lucidj.api.core.CodeEngine;
import org.lucidj.api.core.ComponentInterface;

public class QuercusProcessor implements Aggregate
{
    private ComponentInterface code_container;
    private CodeEngine code_engine;

    public QuercusProcessor (ComponentInterface code_container, CodeEngine code_engine)
    {
        this.code_container = code_container;
        this.code_engine = code_engine;
    }

    @Override
    public Object identity ()
    {
        return (code_container);
    }

    @Override
    public Object[] elements ()
    {
        return (new Object[] { this, code_container, code_engine });
    }
}

// EOF
