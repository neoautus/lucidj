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

package org.lucidj.codeengine.felix;

import org.apache.felix.gogo.runtime.threadio.ThreadIOImpl;
import org.lucidj.api.CodeContext;
import org.lucidj.api.CodeEngine;
import org.lucidj.api.CodeEngineProvider;
import org.lucidj.api.ManagedObjectInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.lang.model.type.TypeKind;
import java.io.PrintStream;
import java.io.Reader;

// TODO: CodeRunner MAY BE RUNNING!!!!
public class CodeEngineMtWrapper implements CodeEngine, CodeEngine.Mt
{
    private final transient Logger log = LoggerFactory.getLogger (CodeEngineMtWrapper.class);

    private final CodeEngine code_engine;
    private ExecThread exec_thread;
    private Object output = TypeKind.NONE;
    private boolean interrupt_requested;

    public CodeEngineMtWrapper (CodeEngine code_engine)
    {
        this.code_engine = code_engine;
    }

    private CodeContext perform_exec (Object code, CodeContext context)
    {
        // TODO: context => which smartbox. context MAY be running.

        // Ensure a valid context to use
        if (context == null)
        {
            context = code_engine.getContext ();
        }

        final CodeContext.Callbacks context_callbacks = (CodeContext.Callbacks)context;

        // Setup capture listeners
        PipeListener stdout_pipe = new PipeListener ();
        stdout_pipe.setPrintListener (new PipeListener.PrintListener ()
        {
            @Override
            public void print (String output)
            {
                context_callbacks.stdoutPrint (output);
            }
        });
        context.setStdout (stdout_pipe.getPrintStream ());

        PipeListener stderr_pipe = new PipeListener ();
        stderr_pipe.setPrintListener (new PipeListener.PrintListener ()
        {
            @Override
            public void print (String output)
            {
                context_callbacks.stderrPrint (output);
            }
        });
        context.setStderr (stderr_pipe.getPrintStream ());

        // Setup beanshell thread
        exec_thread = new ExecThread (code, context);
//        exec_thread.setContextClassLoader (cld);

        // Start the gang
        interrupt_requested = false;
        stdout_pipe.start ();
        stderr_pipe.start ();
        exec_thread.start ();

        // TODO: INSTEAD WAITING, PUT THE THREAD ON THE "PROCESS POOL" TO MONITOR IT
        return (context);
    }

    //-----------------------------------------------------------------------------------------------------------------
    // CodeEngine Multithread
    //-----------------------------------------------------------------------------------------------------------------

    @Override // CodeEngine.Mt
    public CodeContext exec (String code, CodeContext context)
    {
        return (perform_exec (code, context));
    }

    @Override // CodeEngine.Mt
    public CodeContext exec (Reader code, CodeContext context)
    {
        return (perform_exec (code, context));
    }

    @Override
    public Thread getThread ()
    {
        return (exec_thread);
    }

    //-----------------------------------------------------------------------------------------------------------------
    // CodeEngine wrapping
    //-----------------------------------------------------------------------------------------------------------------

    @Override // CodeEngine
    public Object eval (String script, CodeContext context)
    {
        return (code_engine.eval (script, context));
    }

    @Override // CodeEngine
    public Object eval (Reader reader, CodeContext context)
    {
        return (code_engine.eval (reader, context));
    }

    @Override // CodeEngine
    public CodeContext getContext ()
    {
        return (code_engine.getContext ());
    }

    @Override // CodeEngine
    public void setContext (CodeContext context)
    {
        code_engine.setContext (context);
    }

    @Override // CodeEngine
    public CodeEngineProvider getProvider ()
    {
        return (code_engine.getProvider ());
    }

    @Override // ManagedObject
    public void validate (ManagedObjectInstance instance)
    {
        // Nop
    }

    @Override // ManagedObject
    public void invalidate (ManagedObjectInstance instance)
    {
        // Nop
    }

    class ExecThread extends Thread
    {
        private Object code;
        private CodeContext context;
        private CodeContext.Callbacks context_callbacks;

        public ExecThread (Object code, CodeContext context)
        {
            this.code = code;
            this.context = context;
            context_callbacks = (CodeContext.Callbacks)context;
        }

        @Override
        public void run ()
        {
            log.info("run() {} START code_engine={} statements={}", this, code_engine, code);

            context_callbacks.started ();

            // TODO: THESE WILL BE CLOSED... WE NEED TO REFIL THESE PrintStreams
            PrintStream capture_out = context.getStdout ();
            PrintStream capture_err = context.getStderr ();

            // Setup ThreadIO for this thread
            ThreadIOImpl tio = new ThreadIOImpl ();
            tio.setStreams (System.in, capture_out, capture_err);

            // No final return yet
            Object result = null;

            try
            {
                if (code instanceof Reader)
                {
                    result = code_engine.eval ((Reader)code, context);
                }
                else
                {
                    result = code_engine.eval ((String)code, context);
                }
            }
            catch (Throwable e)
            {
                result = e;
            }

            // End capture and close handles, so listener threads can be finished
            capture_out.flush ();
            capture_out.close ();
            capture_err.flush();
            capture_err.close ();

            // Output either a result object or a throwable
            context_callbacks.outputObject (result);

            // Thread about to terminate
            context_callbacks.terminated ();

            log.info("run() {} FINISH code_engine={} result={}", this, code_engine, result);
        }
    }
}

// EOF
