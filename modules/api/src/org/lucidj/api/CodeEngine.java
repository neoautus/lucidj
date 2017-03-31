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

public interface CodeEngine extends ManagedObject
{
    Object getOutput ();
    boolean haveOutput ();
    Exception getException ();
    Object eval ();

    void exec (String statements);
    boolean isRunning ();
    void requestBreak ();

    void setStdoutListener (PrintListener listener);
    void setStderrListener (PrintListener listener);
    void stateListener (StateListener listener);
    void dynamicVariableListener (DynamicVariableListener listener)
        throws NoSuchFieldError;
    Object dynamicVariableLookup (String name);

    void setStatements (String statements);
//    void setStdin (InputStream stdin);        --> inside execution instance?
//    void setStdout (PrintStream stdout);
//    void setStderr (PrintStream stderr);

    interface StateListener
    {
        void state (Thread.State s);
    }

    interface PrintListener
    {
        void print (String output);
    }

    interface DynamicVariableListener
    {
        Object getDynamicVariable (String varname) throws NoSuchFieldError;
    }
}

// EOF
