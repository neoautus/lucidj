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

package org.lucidj.api.core;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Map;

public interface ServiceObject
{
    int VALIDATE = 1;
    int INVALIDATE = 2;
    int SIGKILL = 9;
    int SIGPIPE = 13;
    int USER_DEFINED = 1000;

    interface Listener
    {
        void event (int type, Object serviceObject);
    }

    interface Provider
    {
        Object newObject (String objectClassName, Map<String, Object> properties);
    }

    @Retention (RetentionPolicy.RUNTIME)
    @Target ({ElementType.FIELD})
    @interface Context
    {
        // No properties
    }

    @Retention (RetentionPolicy.RUNTIME)
    @Target ({ElementType.METHOD})
    @interface Validate
    {
        // No properties
    }

    @Retention (RetentionPolicy.RUNTIME)
    @Target ({ElementType.METHOD})
    @interface Invalidate
    {
        // No properties
    }
}

// EOF
