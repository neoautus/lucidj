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

package org.lucidj.vaadinweaving;

import javassist.CannotCompileException;
import javassist.ClassClassPath;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.LoaderClassPath;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.server.VaadinSession;

import org.osgi.framework.hooks.weaving.WeavingHook;
import org.osgi.framework.hooks.weaving.WovenClass;
import org.osgi.framework.wiring.BundleWiring;

public class VaadinWeaving implements WeavingHook
{
    private final static Logger log = LoggerFactory.getLogger (VaadinWeaving.class);

    private final static String VAADIN_TRY_FINALLY_AUTO_LOCK =
        "{" +
        "    java.util.concurrent.locks.Lock __v_lock = null;" +
        "    try " +
        "    {" +
        "        com.vaadin.server.VaadinSession __v_session = " +
        "            com.vaadin.server.VaadinSession.getCurrent (); " +
        "        if (__v_session != null)" +
        "        {" +
        "            __v_lock = __v_session.getLockInstance (); " +
        "            __v_lock.lock ();" +
        "        }" +
        "        $_ = $proceed ($$);" +
        "    }" +
        "    finally" +
        "    {" +
        "        if (__v_lock != null)" +
        "        {" +
        "            __v_lock.unlock (); " +
        "        }" +
        "    }" +
        "}";

    @Override
    public void weave (WovenClass wc)
    {
        String class_name = wc.getClassName ();

        // TODO: MAKE THIS CONFIGURABLE, INCLUDING METHOD SETS
        // TODO: ADD SANITY CHECKS, LIKE "thou shall not VerticalLayout.addComponent(null);"
        if (class_name.startsWith ("com.vaadin.ui.")
            || class_name.equals ("org.vaadin.jouni.restrain.Restrain"))
        {
            log.info ("Weaving class {}", class_name);

            // This bundle classloader sees Vaadin et al.
            ClassPool cp = new ClassPool (true);
            cp.insertClassPath (new ClassClassPath (VaadinSession.class));

            // Add the source classloader for the woven class
            BundleWiring bwg = wc.getBundleWiring ();
            cp.insertClassPath (new LoaderClassPath (bwg.getClassLoader ()));

            try
            {
                // Get all methods from instrumented class
                CtClass cc = cp.get (wc.getClassName ());
                CtMethod[] methods = cc.getDeclaredMethods();

                // A little heavy-handed to hack every method, however if
                // sh*t is awaiting somewhere, it'll become visible faster
                for (CtMethod m: methods)
                {
                    if (m.isEmpty ())
                    {
                        continue;
                    }

                    // We need to replace() because of try/finally structure
                    m.instrument (new ExprEditor ()
                    {
                        public void edit (MethodCall m)
                            throws CannotCompileException
                        {
                            m.replace (VAADIN_TRY_FINALLY_AUTO_LOCK);
                        }
                    });
                }

                // Write back the shiny new bytecode
                wc.setBytes (cc.toBytecode ());
                cc.detach ();
            }
            catch (Exception e)
            {
                log.error ("Exception weaving: {}", class_name, e);
            }
        }
    }
}

// EOF
