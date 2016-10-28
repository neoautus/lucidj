/*
 * Copyright 2016 NEOautus Ltd. (http://neoautus.com)
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

package org.rationalq.smartbox;

import bsh.CallStack;
import bsh.Interpreter;
import org.lucidj.system.SystemAPI;

public class publish
{
    public static String usage()
    {
        return ("Usage: pragma (\"directive\" [, \"value\" ] )\n");
    }

    public static void invoke (Interpreter env, CallStack callstack, String identifier, Object object)
    {
        if (object == null)
        {
            SystemAPI.getCurrent ().unpublishObject (identifier);
        }
        else
        {
            SystemAPI.getCurrent ().publishObject (identifier, object);
            pragma.pragma ("publish");
        }
    }
}

// EOF
