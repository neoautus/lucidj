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

package org.lucidj.karafbinding;

import org.apache.karaf.shell.api.console.Session;
import org.apache.karaf.shell.api.console.SessionFactory;
import org.apache.karaf.util.jaas.JaasHelper;
import org.lucidj.api.CodeContext;
import org.lucidj.api.ServiceBinding;
import org.lucidj.api.ServiceBindingsManager;
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

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;

@Component (immediate = true, publicFactory = false)
@Instantiate
@Provides
public class KarafBinding implements ServiceBinding
{
    private final static transient Logger log = LoggerFactory.getLogger (KarafBinding.class);

    private String admin_realm = "karaf";
    private Subject admin_subject;

    @Requires
    ServiceBindingsManager bindings;

    @Requires
    private SessionFactory karaf_session_factory;

    @Validate
    private void validate ()
    {
        bindings.register (this);
    }

    @Override
    public String getBindingName ()
    {
        return ("Karaf");
    }

    private boolean authenticate (final String username, final String password)
    {
        try
        {
            admin_subject = new Subject ();

            LoginContext loginContext = new LoginContext (admin_realm, admin_subject, new CallbackHandler ()
            {
                public void handle(Callback[] callbacks)
                    throws IOException, UnsupportedCallbackException
                {
                    for (Callback callback : callbacks)
                    {
                        if (callback instanceof NameCallback)
                        {
                            ((NameCallback)callback).setName (username);
                        }
                        else if (callback instanceof PasswordCallback)
                        {
                            ((PasswordCallback)callback).setPassword (password.toCharArray());
                        }
                        else
                        {
                            throw new UnsupportedCallbackException (callback);
                        }
                    }
                }
            });

            loginContext.login ();

            //session.setAttribute(SUBJECT_ATTRIBUTE_KEY, subject);
            return (true);
        }
        catch (Exception e)
        {
            log.error ("User authentication failed with " + e.getMessage(), e);
        }
        return (false);
    }

    private Subject get_subject ()
    {
        if (admin_subject == null)
        {
            authenticate ("karaf", "karaf");
        }

        return (admin_subject);
    }

    public Object run (final String command)
    {
//        SessionFactory session_factory = get_session_factory ();
        Subject subject = get_subject ();

//        if (session_factory == null || subject == null)
//        {
//            return (null);
//        }

        final Session admin_session = karaf_session_factory.create (System.in, System.out, System.err);
        Object result = null;

        try
        {
            result = JaasHelper.doAs (subject, new PrivilegedExceptionAction<Object> ()
            {
                public Object run() throws Exception
                {
                    return (admin_session.execute(command));
                }
            });
        }
        catch (PrivilegedActionException e)
        {
            result = e;
        }

        return (result);
    }

    @Override
    public Object getService (CodeContext context)
    {
        return (new KarafInterface ());
    }

    public class KarafInterface
    {
        private Session session;

        public Object run (final String command)
        {
            // We create a new admin session for every command.
            // Should be revised, the impact on resources is unknown.
            final Session admin_session = karaf_session_factory.create (System.in, System.out, System.err);
            Object result = null;

            try
            {
                Subject subject = get_subject ();

                result = JaasHelper.doAs (subject, new PrivilegedExceptionAction<Object> ()
                {
                    public Object run() throws Exception
                    {
                        return (admin_session.execute(command));
                    }
                });
            }
            catch (PrivilegedActionException e)
            {
                result = e;
            }

            return (result);
        }
    }
}

// EOF
