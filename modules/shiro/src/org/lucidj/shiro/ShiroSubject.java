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
import org.lucidj.api.SecuritySubject;

public class ShiroSubject implements SecuritySubject // Serializable?
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

    @Override
    public void touchSession ()
    {
        shiro_subject.getSession ().touch ();
    }
}

// EOF
