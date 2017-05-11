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

package org.lucidj.accounts;

import org.lucidj.api.ManagedObjectFactory;
import org.lucidj.api.ManagedObjectInstance;

import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewProvider;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

@Component
@Instantiate
@Provides
public class Accounts implements ViewProvider
{
    private final static String NAVID = "accounts";

    @Requires
    private ManagedObjectFactory object_factory;

    @Override // ViewProvider
    public String getViewName (String s)
    {
        if (NAVID.equals (s))
        {
            return (NAVID);
        }
        return null;
    }

    @Override // ViewProvider
    public View getView (String s)
    {
        if (NAVID.equals (s))
        {
            ManagedObjectInstance view_instance = object_factory.wrapObject (new AccountsView ());
            return (view_instance.adapt (AccountsView.class));
        }
        return null;
    }
}

// EOF
