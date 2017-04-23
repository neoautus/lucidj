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

package org.lucidj.beanshell;

import bsh.Interpreter;
import bsh.NameSpace;
import org.lucidj.api.CodeEngineBase;
import org.lucidj.api.CodeContext;
import org.lucidj.api.CodeEngineManager;
import org.lucidj.api.CodeEngineProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;

@Component (immediate = true, publicFactory = false)
@Instantiate
@Provides
public class BeanShellEngineProvider implements CodeEngineProvider
{
    private final static Logger log = LoggerFactory.getLogger (BeanShellEngineProvider.class);

    // This interpreter is where all running threads are anchored while running on
    // the component, in order to share namespace etc.
    private Interpreter bsh;
    private NameSpace ns;

    @Requires
    private CodeEngineManager engineFactory;

    private boolean init_engine ()
    {
        // Create an interpreter instance with a null inputstream,
        // the capture out/err stream, non-interactive
        bsh = new Interpreter (null, System.out, System.err, false);
//        bsh.setClassLoader (cld);

        // TODO: CHECK: ONE LNS FOR EVERY BUNDLE?

        // The local namespace is child of parent interpreters namespace
        ns = bsh.getNameSpace ();

        try
        {
            // Our own commands
            //lns.importCommands (this.getClass ().getPackage ().getName ());

            // Utility imports
            ns.importPackage ("com.vaadin.ui");
            ns.importClass ("com.vaadin.server.ClassResource");
            ns.importClass ("com.vaadin.server.ExternalResource");
            ns.importClass ("com.vaadin.server.FontAwesome");
            ns.importClass ("com.vaadin.server.GenericFontIcon");
            ns.importClass ("com.vaadin.server.StreamResource");
            ns.importClass ("com.vaadin.server.ThemeResource");
            ns.importClass ("org.osgi.framework");
            ns.importClass ("com.vaadin.shared.ui.label.ContentMode");
            ns.importStatic (java.lang.Math.class);
            ns.importClass ("org.lucidj.plotly.Plotly");
            return (true);
        }
        catch (Exception e)
        {
            log.error ("Exception creating ContextData", e);
            return (false);
        }
    }

    @Override
    public CodeEngineBase newCodeEngine (String shortName, CodeContext context)
    {
        return (new BeanShellEngine (this, bsh));
    }

    @Validate
    private void validate ()
    {
        if (init_engine ())
        {
            engineFactory.registerEngineName ("beanshell", this);
            log.info ("BeanShellEngineProvider started.");
        }
    }

    @Invalidate
    private void invalidate ()
    {
        log.info ("BeanShellEngineProvider terminated.");
    }

}

// EOF
