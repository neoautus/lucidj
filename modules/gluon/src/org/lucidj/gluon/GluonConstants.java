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
    String DEFAULT_BOUNDARY = "-------THINK_BIG_BE_BOLD-------";
    String VERSION = "Q-Quark-Version";

    String SIMPLE_REPRESENTATION = "/simple";
    String COMPLEX_REPRESENTATION = "/complex";
    String OBJECT_CLASS = "Q-Object";
    String OBJECT_CLASS_EMBEDDED = OBJECT_CLASS + "/embedded";
    String OBJECT_CLASS_ID = OBJECT_CLASS + "/id";
    String CONTENT_TYPE = "Q-Content-Type";
    String CONTENT_ENCODING = CONTENT_TYPE + "/encoding";
    String CONTENT_BOUNDARY = CONTENT_TYPE + "/boundary";
    String CONTENTS = "/contents";
    String INLINE_REPRESENTATION = "/inline";
    String FULL_REPRESENTATION = "/full";
}

// EOF
