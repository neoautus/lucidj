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

package org.lucidj.smartbox;

//import bsh.CallStack;
//import bsh.Interpreter;

// TODO: REFACTOR ALL OF THIS INTO A CLASS THAT CAN BE USED ON BEANSHELL OR SCALA OR WHATEVER
public class pragma
{
//    private static String PRAGMAS = "Pragmas";
//    private static ThreadLocal<SmartBox> current_sb = new InheritableThreadLocal<> ();
//
//    public static void setSmartBox (SmartBox sb)
//    {
//        current_sb.set (sb);
//    }
//
//    public static String usage()
//    {
//        return ("Usage: pragma (\"directive\" [, \"value\" ] )\n");
//    }
//
//    public static void pragma (String directive)
//    {
//        String current_directives = (String)current_sb.get ().getProperty (PRAGMAS);
//
//        if (current_directives == null)
//        {
//            current_directives = directive;
//        }
//        else
//        {
//            String cdr = "," + current_directives.replace (" ", "") + ",";
//
//            if (!cdr.contains ("," + directive + ","))
//            {
//                current_directives += ", " + directive;
//            }
//        }
//
//        // TODO: CLEAR PRAGMAS ON LOAD, AVOID KEEP UNWANTED PRAGMAS ALIVE
//        current_sb.get ().setProperty ("Pragmas", current_directives);
//    }
//
//    public static void invoke (Interpreter env, CallStack callstack, String directive)
//    {
//        pragma (directive);
//    }
//
//    public static String invoke (Interpreter env, CallStack callstack)
//    {
//        return ((String)current_sb.get ().getProperty (PRAGMAS));
//    }
}

// EOF
