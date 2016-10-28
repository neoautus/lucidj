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

import bsh.Interpreter;
import org.apache.felix.gogo.runtime.threadio.ThreadIOImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.io.StringReader;

public class BeanShell
{
    private final transient Logger log = LoggerFactory.getLogger (BeanShell.class);

    private String context_id;
    private Interpreter parent_interpreter;
    private LocalNameSpace local_namespace;
    private PrintStreamListener out_listener;
    private PrintStreamListener err_listener;
    private StateListener state_listener;
    private DynamicVariableListener dynamicvariable_listener;
    private ExecThread exec_thread;
    private boolean interrupt_requested;
    private ClassLoader cld;

    private volatile Object result;
    private volatile Exception exception;

    // TODO: SOLVE METHOD REWRITE
    public BeanShell (String context_id, Interpreter parent_interpreter, LocalNameSpace local_namespace, ClassLoader cld)
    {
        this.context_id = context_id;
        this.parent_interpreter = parent_interpreter;
        this.local_namespace = local_namespace;
        this.cld = cld;
    }

    public Object getResult ()
    {
        return (result);
    }

    public Exception getException ()
    {
        return (exception);
    }

    public Interpreter getInterpreter ()
    {
        return (parent_interpreter);
    }

    class PipeListener extends Thread
    {
        private PipedInputStream sink = null;
        private PrintStreamListener listener = null;
        private byte[] buffer = new byte [512];

        public PipeListener (PipedOutputStream source, PrintStreamListener listener)
        {
            try
            {
                sink = new PipedInputStream (source);
                this.listener = listener;
            }
            catch (Exception ignore) {};

            log.info ("PipeListener {}: source={}, listener={}, sink={}", this, source, listener, sink);
        }

        @Override
        public void run ()
        {
            log.info ("PipeListener {}: START", this);
            try
            {
                int len;

                while ((len = sink.read (buffer, 0, buffer.length)) != -1)
                {
                    String output = new String (buffer, 0, len);

                    log.info ("PipeListener {}: OUTPUT [{}]", sink, output.replace ("\n", "\\n"));

                    if (listener != null)
                    {
                        listener.print (output);
                    }
                }
            }
            catch(IOException e)
            {
                throw new RuntimeException(e);
            }
            finally
            {
                // close....
            }
            log.info ("PipeListener {}: FINISH", this);
        }
    }

    class ExecThread extends Thread
    {
        private PrintStream capture_out, capture_err;
        private String statements;
        private BeanShell parent;

        public ExecThread (String statements, PrintStream capture_out, PrintStream capture_err,
                           BeanShell parent)
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
                result = parent_interpreter.eval (input_statements, capture_out, capture_err,
                                                  parent_interpreter.getNameSpace (), context_id);
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

    class NonBufferedPipedOutputStream extends PipedOutputStream
    {
        @Override
        public void write(byte[] b, int off, int len)
            throws IOException
        {
            super.write (b, off, len);
            flush ();
        }

        @Override
        public void write(int b)
            throws IOException
        {
            super.write (b);
            flush ();
        }
    }

    public void exec (String statements)
    {
        // TODO: context => which smartbox. context MAY be running.

        // Capture out and err
        PipedOutputStream pipe_stdout = new NonBufferedPipedOutputStream ();
        PrintStream capture_stdout = new PrintStream (pipe_stdout);
        PipedOutputStream pipe_stderr = new NonBufferedPipedOutputStream ();
        PrintStream capture_stderr = new PrintStream (pipe_stderr);

        // Setup capture listeners
        PipeListener stdout_listener = new PipeListener (pipe_stdout, out_listener);
        PipeListener stderr_listener = new PipeListener (pipe_stderr, err_listener);

        // Setup beanshell thread
        exec_thread = new ExecThread (statements, capture_stdout, capture_stderr, this);
        exec_thread.setContextClassLoader (cld);

        // Start the gang
        interrupt_requested = false;
        stdout_listener.start ();
        stderr_listener.start ();
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

    void outListener (PrintStreamListener listener)
    {
        out_listener = listener;
    }

    void errListener (PrintStreamListener listener)
    {
        err_listener = listener;
    }

    void stateListener (StateListener listener)
    {
        state_listener = listener;
    }

    void dynamicVariableListener (DynamicVariableListener listener)
    {
        dynamicvariable_listener = listener;
    }

    Object dynamicVariableLookup (String name)
        throws NoSuchFieldException
    {
        return (dynamicvariable_listener.getDynamicVariable (name));
    }

    interface StateListener
    {
        void state (Thread.State s);
    }

    interface PrintStreamListener
    {
        void print (String output);
    }

    interface DynamicVariableListener
    {
        Object getDynamicVariable (String varname) throws NoSuchFieldException;
    }
}

// EOF
