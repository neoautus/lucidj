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

import javax.script.ScriptContext;
import java.io.PrintStream;

import org.osgi.framework.Bundle;

public interface CodeContext extends ManagedObject
{
    String getContextId ();

    void setStdout (PrintStream stdout);
    PrintStream getStdout ();
    void setStderr (PrintStream stderr);
    PrintStream getStderr ();

    void setBindings (CodeBindings bindings);
    CodeBindings getBindings ();
    Bundle getBundle ();
    void linkContext (ScriptContext jsr223_context);
    ScriptContext getLinkedContext ();

    <T> T getObject (Class<T> type);
    <T> void putObject (Class<T> type, T obj);

    void addCallbacksListener (Callbacks listener);
    void removeCallbacksListener (Callbacks listener);
    Object getOutput ();
    boolean haveOutput ();

//
//    interface Callbacks
//    {
//        void outputObject (Object obj);
//        Object getVariable (String varname) throws NoSuchFieldError;
//    }

//    boolean isRunning ();
//    void requestBreak ();

    interface Callbacks
    {
        void stdoutPrint (String str);
        void stderrPrint (String str);
        void outputObject (Object obj);
        void started ();
        void terminated ();
    }
}

// EOF
