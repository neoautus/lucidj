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

import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.subject.Subject;
import org.lucidj.api.core.SecuritySubject;

import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;

// TODO: ServiceObject
public class ShiroSubject implements SecuritySubject
{
    private Subject shiro_subject;

    public ShiroSubject (Subject shiro_subject)
    {
        this.shiro_subject = shiro_subject;
    }

    @Override
    public boolean isAuthenticated ()
    {
        return (shiro_subject.isAuthenticated ());
    }

    @Override
    public boolean login (String username, String password)
    {
        UsernamePasswordToken token = new UsernamePasswordToken (username, password);

        try
        {
            // TODO: http://stackoverflow.com/questions/14516851/shiro-complaining-there-is-no-session-with-id-xxx-with-defaultsecuritymanager
            shiro_subject.login (token);
        }
        catch (Exception ignore) {};
        return (shiro_subject.isAuthenticated ());
    }

    @Override
    public String getPrincipal ()
    {
        return ((String)shiro_subject.getPrincipal ());
    }

// TODO: PROPAGATE SESSION EXPIRATION
//    org.apache.shiro.session.UnknownSessionException: There is no session with id [58451c67-9c18-4985-ba7d-2cb4a14c38ab]
//        at org.apache.shiro.session.mgt.eis.AbstractSessionDAO.readSession(AbstractSessionDAO.java:170)
//        at org.apache.shiro.session.mgt.DefaultSessionManager.retrieveSessionFromDataSource(DefaultSessionManager.java:236)
//        at org.apache.shiro.session.mgt.DefaultSessionManager.retrieveSession(DefaultSessionManager.java:222)
//        at org.apache.shiro.session.mgt.AbstractValidatingSessionManager.doGetSession(AbstractValidatingSessionManager.java:118)
//        at org.apache.shiro.session.mgt.AbstractNativeSessionManager.lookupSession(AbstractNativeSessionManager.java:108)
//        at org.apache.shiro.session.mgt.AbstractNativeSessionManager.lookupRequiredSession(AbstractNativeSessionManager.java:112)
//        at org.apache.shiro.session.mgt.AbstractNativeSessionManager.touch(AbstractNativeSessionManager.java:191)
//        at org.apache.shiro.session.mgt.DelegatingSession.touch(DelegatingSession.java:120)
//        at org.apache.shiro.session.ProxiedSession.touch(ProxiedSession.java:100)
//        at org.apache.shiro.session.ProxiedSession.touch(ProxiedSession.java:100)
//        at org.apache.shiro.session.ProxiedSession.touch(ProxiedSession.java:100)
//        at org.lucidj.shiro.ShiroSubject.touchSession(ShiroSubject.java:66)
//        at org.lucidj.shiro.Shiro.__M_getStoredSubject(Shiro.java:98)
//        at org.lucidj.shiro.Shiro.getStoredSubject(Shiro.java)
//        at org.lucidj.shiro.Shiro.__M_getSubject(Shiro.java:105)
//        at org.lucidj.shiro.Shiro.getSubject(Shiro.java)

    @Override
    public void touchSession ()
    {
        shiro_subject.getSession ().touch ();
    }

    @Override // SecurityEngine
    public FileSystem getDefaultUserFS ()
    {
        // Return an FileSystem so we can take advantage of JSR203
        return (FileSystems.getDefault ());
    }

    @Override // SecurityEngine
    public Path getDefaultUserDir ()
    {
        String user = getPrincipal ();

        if (user == null)
        {
            return (null);
        }
        else if (user.equals ("system"))
        {
            // System have a special location $LUCIDJ_HOME/system
            return (getDefaultUserFS ().getPath (System.getProperty ("system.home"), "system"));
        }
        else
        {
            // The profiles are located at $LUCIDJ_HOME/profiles/$LUCIDJ_USER
            return (getDefaultUserFS ().getPath (System.getProperty ("system.home"), "profiles", user));
        }
    }
}

// EOF
