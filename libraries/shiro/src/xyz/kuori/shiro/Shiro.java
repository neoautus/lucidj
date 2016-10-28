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

package xyz.kuori.shiro;

import com.vaadin.server.VaadinSession;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.Factory;
import org.apache.shiro.config.IniSecurityManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

@Component (immediate = true)
@Instantiate
@Provides
public class Shiro
{
    private final static transient Logger log = LoggerFactory.getLogger (Shiro.class);

    // http://shiro.apache.org/10-minute-tutorial.html
    // http://shiro.apache.org/configuration.html
    public Shiro ()
    {
        String shiro_ini = "file:" + System.getProperty("rq.conf") + "/shiro.ini";
        log.info ("shiro_ini = " + shiro_ini);
        Factory<SecurityManager> factory = new IniSecurityManagerFactory(shiro_ini);
        SecurityUtils.setSecurityManager(factory.getInstance());
    }

    public String getLocalHome ()
    {
        return (System.getProperty("rq.home"));
    }

    public Subject getSubject ()
    {
        Subject current_user = VaadinSession.getCurrent().getAttribute(Subject.class);

        if (current_user == null)
        {
            current_user = SecurityUtils.getSecurityManager().createSubject(null);
            current_user.getSession().setTimeout(24L * 60 * 60 * 1000); // 24h

            try
            {
                // Store current user into VaadinSession
                VaadinSession.getCurrent().getLockInstance().lock();
                VaadinSession.getCurrent().setAttribute(Subject.class, current_user);
            }
            finally
            {
                VaadinSession.getCurrent().getLockInstance().unlock();
            }
        }

        // Reset doomsday counter....
        current_user.getSession().touch();

        return (current_user);
    }


    // Return an FileSystem so we can take advantage of JSR203
    public FileSystem getDefaultUserFS ()
    {
        return (FileSystems.getDefault ());
    }

    public Path getDefaultUserDir ()
    {
        Subject subject = getSubject ();

        if (subject == null)
        {
            return (null);
        }

        if (!(subject.getPrincipal() instanceof String))
        {
            return (null);
        }

        String username = (String)subject.getPrincipal ();

        return (getDefaultUserFS ().getPath (getLocalHome (), "userdata", username));
    }
}

// EOF
