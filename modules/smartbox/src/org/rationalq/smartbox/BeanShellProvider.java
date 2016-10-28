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
import bsh.NameSpace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.Map;

public class BeanShellProvider
{
    private final transient Logger log = LoggerFactory.getLogger (BeanShellProvider.class);

    // This interpreter is where all running threads are anchored while running on
    // the component, in order to share namespace etc.
    private Interpreter bsh;

    private final ClassLoader cld;
    private LocalNameSpace lns;

    private String context_prefix = "BeanShell";
    private int instance_count = 1;

    public BeanShellProvider (ClassLoader cld)
    {
        // Use ONLY the QuarkContext classloader
        this.cld = cld;
    }

    private synchronized String generateContextId ()
    {
        return (context_prefix + "-" + instance_count++);
    }

    public boolean init (Map variables)
    {
        // Create an interpreter instance with a null inputstream,
        // the capture out/err stream, non-interactive
        bsh = new Interpreter (null, System.out, System.err, false);
        bsh.setClassLoader (cld);

        try
        {
            if (variables != null)
            {

                for (Object entry : variables.entrySet ())
                {
                    Map.Entry property = (Map.Entry)entry;
                    bsh.set ((String)property.getKey (), property.getValue ());
                }
            }

            NameSpace parent = bsh.getNameSpace();

            lns = new LocalNameSpace (parent, context_prefix);
            bsh.setNameSpace (lns);

            // Our own commands
            lns.importCommands (this.getClass ().getPackage ().getName ());

            // Utility imports
            lns.importPackage("com.vaadin.ui");
            lns.importClass("com.vaadin.server.ClassResource");
            lns.importClass("com.vaadin.server.ExternalResource");
            lns.importClass("com.vaadin.server.FontAwesome");
            lns.importClass("com.vaadin.server.GenericFontIcon");
            lns.importClass("com.vaadin.server.StreamResource");
            lns.importClass("com.vaadin.server.ThemeResource");
            lns.importClass("org.osgi.framework");
            lns.importClass("com.vaadin.shared.ui.label.ContentMode");
            lns.importStatic(java.lang.Math.class);
            lns.importClass ("org.lucidj.plotly.Plotly");
        }
        catch (Exception e)
        {
            log.error ("init: Exception {}", e);
            return (false);
        }

        return (true);
    }

    public BeanShell getInstance ()
    {
        return (new BeanShell (generateContextId (), bsh, lns, cld));
    }
}

// EOF
