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

package org.lucidj.shiro;

import com.vaadin.server.VaadinSession;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.Factory;
import org.apache.shiro.config.IniSecurityManagerFactory;
import org.lucidj.api.SecurityEngine;
import org.lucidj.api.SecuritySubject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

@Component (immediate = true, publicFactory = false)
@Instantiate
@Provides
public class Shiro implements SecurityEngine
{
    private final static Logger log = LoggerFactory.getLogger (Shiro.class);

    private SecurityManager ini_security_manager;

    public Shiro ()
    {
        String shiro_ini = "file:" + System.getProperty ("system.conf") + "/shiro.ini";
        log.info ("shiro_ini = " + shiro_ini);
        Factory<SecurityManager> factory = new IniSecurityManagerFactory (shiro_ini);
        ini_security_manager = factory.getInstance ();
        SecurityUtils.setSecurityManager (ini_security_manager);
    }

    @Override // SecurityEngine
    public SecuritySubject getStoredSubject (boolean create_as_system)
    {
        SecuritySubject current_subject =
            VaadinSession.getCurrent().getAttribute (SecuritySubject.class);

        if (current_subject == null || create_as_system)
        {
            Subject shiro_subject;

            // The subject is always rebuilt when configured as system
            if (create_as_system)
            {
                shiro_subject = new Subject.Builder (ini_security_manager)
                    .authenticated (true)
                    .principals (new SimplePrincipalCollection ("system", ""))
                    .buildSubject ();

                log.info ("Create system subject: {}", shiro_subject);
                log.info ("{}: authenticated={}", shiro_subject, shiro_subject.isAuthenticated ());
            }
            else
            {
                shiro_subject = SecurityUtils.getSecurityManager().createSubject (null);
            }

            // TODO: CONFIGURABLE
            shiro_subject.getSession ().setTimeout (24L * 60 * 60 * 1000); // 24h

            current_subject = new ShiroSubject (shiro_subject);

            // TODO: MAKE SECURITY IDENPENDENT FROM VAADIN/SESSIONS FROM WEB
            try
            {
                // Store current user into VaadinSession
                VaadinSession.getCurrent().getLockInstance().lock();
                VaadinSession.getCurrent().setAttribute(SecuritySubject.class, current_subject);
            }
            finally
            {
                VaadinSession.getCurrent().getLockInstance().unlock();
            }
        }

        // Reset doomsday counter....
        current_subject.touchSession ();
        return (current_subject);
    }

    @Override // SecurityEngine
    public SecuritySubject getSubject ()
    {
        SecuritySubject security_subject = getStoredSubject (false);
        log.info ("Shiro: getSubject() = {}", security_subject);
        return (security_subject);
    }

    @Override // SecurityEngine
    public SecuritySubject createSystemSubject ()
    {
        return (getStoredSubject (true));
    }
}

// EOF
