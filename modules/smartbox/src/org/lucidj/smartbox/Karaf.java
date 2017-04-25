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

import org.apache.karaf.shell.api.console.Session;
import org.apache.karaf.shell.api.console.SessionFactory;
import org.apache.karaf.util.jaas.JaasHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginContext;
import java.io.IOException;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

// TODO: KARAF SHOULD BECOME AN EXTENSION SERVICE
public class Karaf
{
//    private final static transient Logger log = LoggerFactory.getLogger (Karaf.class);
//
//    private String admin_realm = "karaf";
//    private Subject admin_subject;
//    private SessionFactory karaf_session_factory;
//
//    public Karaf ()
//    {
//        // Nothing needed for now
//    }
//
//    private SessionFactory get_session_factory ()
//    {
//        if (karaf_session_factory == null)
//        {
//            TaskContext tctx = Kernel.currentTaskContext ();
//
//            if (tctx != null)
//            {
//                BundleContext ctx = tctx.getBundleContext ();
//                ServiceReference<SessionFactory> serviceReference = ctx.getServiceReference (SessionFactory.class);
//
//                if (serviceReference != null)
//                {
//                    karaf_session_factory = ctx.getService (serviceReference);
//                }
//            }
//            // Subject ./itests/src/test/java/org/apache/karaf/itests/KarafTestSupport.java
//            // itests/src/test/java/org/apache/karaf/itests/FeatureTest.java
//        }
//
//        return (karaf_session_factory);
//    }
//
//    private boolean authenticate (final String username, final String password)
//    {
//        try
//        {
//            admin_subject = new Subject ();
//
//            LoginContext loginContext = new LoginContext (admin_realm, admin_subject, new CallbackHandler ()
//            {
//                public void handle(Callback[] callbacks)
//                    throws IOException, UnsupportedCallbackException
//                {
//                    for (Callback callback : callbacks)
//                    {
//                        if (callback instanceof NameCallback)
//                        {
//                            ((NameCallback)callback).setName (username);
//                        }
//                        else if (callback instanceof PasswordCallback)
//                        {
//                            ((PasswordCallback)callback).setPassword (password.toCharArray());
//                        }
//                        else
//                        {
//                            throw new UnsupportedCallbackException (callback);
//                        }
//                    }
//                }
//            });
//
//            loginContext.login ();
//
//            //session.setAttribute(SUBJECT_ATTRIBUTE_KEY, subject);
//            return (true);
//        }
//        catch (Exception e)
//        {
//            log.error ("User authentication failed with " + e.getMessage(), e);
//        }
//        return (false);
//    }
//
//    private Subject get_subject ()
//    {
//        if (admin_subject == null)
//        {
//            authenticate ("karaf", "karaf");
//        }
//
//        return (admin_subject);
//    }
//
//    public Object run (final String command)
//    {
//        SessionFactory session_factory = get_session_factory ();
//        Subject subject = get_subject ();
//
//        if (session_factory == null || subject == null)
//        {
//            return (null);
//        }
//
//        final Session admin_session = karaf_session_factory.create (System.in, System.out, System.err);
//        Object result = null;
//
//        try
//        {
//            result = JaasHelper.doAs (subject, new PrivilegedExceptionAction<Object> ()
//            {
//                public Object run() throws Exception
//                {
//                    return (admin_session.execute(command));
//                }
//            });
//        }
//        catch (PrivilegedActionException e)
//        {
//            result = e;
//        }
//
//        return (result);
//    }
}

// EOF
