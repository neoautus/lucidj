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

package org.lucidj.api;

import java.io.Reader;
import java.io.Writer;

public interface SerializerEngine
{
    String SERIALIZATION_ENGINE = "X-Serialization-Engine";

    boolean register (Class clazz, Serializer serializer);
    boolean serializeObject (Writer wrt, Object obj);
    Object deserializeObject (Reader reader);
}

// EOF