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

package org.lucidj.codeengine;

import bsh.Interpreter;
import org.apache.felix.gogo.runtime.threadio.ThreadIOImpl;
import org.lucidj.api.CodeEngine;
import org.lucidj.api.CodeEngineContext;
import org.lucidj.api.ManagedObjectInstance;
import org.lucidj.codeengine.felix.PipeListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.lang.model.type.TypeKind;
import java.io.PrintStream;
import java.io.StringReader;

public class BeanShellEngine implements CodeEngine
{
    private final transient Logger log = LoggerFactory.getLogger (BeanShellEngine.class);

    private ContextData context_data;
    private String context_id;
    private Interpreter parent_interpreter;
    private LocalNameSpace local_namespace;

    private CodeEngine.PrintListener out_listener;
    private CodeEngine.PrintListener err_listener;
    private StateListener state_listener;
    private DynamicVariableListener dynamicvariable_listener;
    private ExecThread exec_thread;
    private boolean interrupt_requested;

    private volatile Object result = TypeKind.NONE;
    private volatile Exception exception;

    // TODO: SOLVE METHOD REWRITE
    public BeanShellEngine (CodeEngineContext context, ContextData context_data)
    {
        this.context_data = context_data;
        this.parent_interpreter = context_data.getRootInterpreter ();
        this.local_namespace = context_data.getLocalNameSpace ();
//        this.context_id = context_id;
    }

    @Override
    public Object getOutput ()
    {
        return (result);
    }

    @Override
    public boolean haveOutput ()
    {
        return (result != TypeKind.NONE);
    }

    public Exception getException ()
    {
        return (exception);
    }

    @Override
    public Object eval ()
    {
        return null;
    }

    public void exec (String statements)
    {
        // TODO: context => which smartbox. context MAY be running.

        // Setup capture listeners
        PipeListener stdout_pipe = new PipeListener ();
        stdout_pipe.setPrintListener (out_listener);
        PipeListener stderr_pipe = new PipeListener ();
        stderr_pipe.setPrintListener (err_listener);

        // Setup beanshell thread
        exec_thread = new ExecThread (statements,
            stdout_pipe.getPrintStream (), stderr_pipe.getPrintStream (), this);
//        exec_thread.setContextClassLoader (cld);

        // Start the gang
        interrupt_requested = false;
        stdout_pipe.start ();
        stderr_pipe.start ();
        exec_thread.start ();

        // TODO: INSTEAD WAITING, PUT THE THREAD ON THE "PROCESS POOL" TO MONITOR IT
    }

    public boolean isRunning ()
    {
        return (exec_thread != null && exec_thread.isAlive ());
    }

    public void requestBreak ()
    {
        log.info ("*** Interrupting thread {}", exec_thread);
        exec_thread.interrupt ();
        interrupt_requested = true;
    }

    @Override
    public void setStdoutListener (PrintListener listener)
    {
        out_listener = listener;
    }

    @Override
    public void setStderrListener (PrintListener listener)
    {
        err_listener = listener;
    }

    @Override
    public void stateListener (CodeEngine.StateListener listener)
    {
        state_listener = listener;
    }

    @Override
    public void dynamicVariableListener (CodeEngine.DynamicVariableListener listener)
    {
        dynamicvariable_listener = listener;
    }

    @Override
    public Object dynamicVariableLookup (String name)
        throws NoSuchFieldError
    {
        return (dynamicvariable_listener.getDynamicVariable (name));
    }

    @Override
    public void setStatements (String statements)
    {

    }

    @Override
    public void validate (ManagedObjectInstance instance)
    {
        // Nop
    }

    @Override
    public void invalidate (ManagedObjectInstance instance)
    {
        // Nop
    }

    class ExecThread extends Thread
    {
        private PrintStream capture_out, capture_err;
        private String statements;
        private BeanShellEngine parent;

        public ExecThread (String statements, PrintStream capture_out, PrintStream capture_err,
                           BeanShellEngine parent)
        {
            this.statements = statements;
            this.capture_out = capture_out;
            this.capture_err = capture_err;
            this.parent = parent;
        }

        @Override
        public void run ()
        {
            log.info("run() {} START bsh={} statements={}", this, parent_interpreter, statements);

            // Clear previous result and exception
            result = exception = null;

            // Set this BeanShell reference
            local_namespace.setBeanShell (parent);

            if (state_listener != null)
            {
                state_listener.state (State.RUNNABLE);
            }

            // Setup ThreadIO for this thread
            ThreadIOImpl tio = new ThreadIOImpl ();
            tio.setStreams (System.in, capture_out, capture_err);

            // Statement input
            StringReader input_statements = new StringReader (statements);

            // No final return yet
            result = null;

            try
            {
                result = parent_interpreter.eval (input_statements,
                    capture_out, capture_err, local_namespace, context_id);
            }
            catch (Exception e)
            {
                exception = e;
            }

            // End capture and close handles, so listener threads can be finished
            capture_out.flush ();
            capture_out.close ();
            capture_err.flush();
            capture_err.close ();

            if (state_listener != null)
            {
                if (interrupt_requested)
                {
                    // TODO: BETTER SIGNALING USING TaskState
                    state_listener.state (State.BLOCKED);
                }
                else
                {
                    state_listener.state (State.TERMINATED);
                }
            }

            log.info("run() {} FINISH bsh={} result={} exception={}", this, parent_interpreter, result, exception);
        }
    }
}

// EOF
