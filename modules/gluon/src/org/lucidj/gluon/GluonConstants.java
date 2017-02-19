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

public interface GluonConstants
{
    String SERIALIZATION_ENGINE = "X-Serialization-Engine";
    String OBJECT_CLASS = "X-Object";
    String EMBEDDING_FLAG = "embedded";

    String DEFAULT_BOUNDARY = "-------THINK_BIG_BE_BOLD-------";
    String CONTENT_BOUNDARY = ".Content-Boundary";

    String MACRO_CHAR = "\r";
    String EOF_MARKER = "//";
}

// EOF
