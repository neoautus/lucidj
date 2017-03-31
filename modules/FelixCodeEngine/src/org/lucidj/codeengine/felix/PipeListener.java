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

import org.lucidj.api.CodeEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;

public class PipeListener extends Thread
{
    private final static transient Logger log = LoggerFactory.getLogger (PipeListener.class);

    private PipedOutputStream pipe_output;
    private PrintStream print_stream;
    private PipedInputStream sink = null;
    private volatile CodeEngine.PrintListener listener = null;
    private byte[] buffer = new byte [512];

    public PipeListener ()
    {
        pipe_output = new NonBufferedPipedOutputStream ();
        print_stream = new PrintStream (pipe_output);

        try
        {
            sink = new PipedInputStream (pipe_output);
        }
        catch (Exception ignore) {};

        log.info ("PipeListener {}: pipe_output={}, sink={}", this, pipe_output, sink);
    }

    public PrintStream getPrintStream ()
    {
        return (print_stream);
    }

    public void setPrintListener (CodeEngine.PrintListener listener)
    {
        this.listener = listener;
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
            // TODO: CLOSE ALL THAT STUFF
        }
        log.info ("PipeListener {}: FINISH", this);
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
}

// EOF
