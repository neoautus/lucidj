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

package org.lucidj.menumanager;

import org.lucidj.api.MenuInstance;
import org.lucidj.api.MenuManager;
import org.lucidj.api.MenuProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import org.apache.felix.ipojo.annotations.Bind;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Unbind;

@Component
@Instantiate
@Provides (specifications = MenuManager.class)
public class DefaultMenuManager implements MenuManager
{
    private final static transient Logger log = LoggerFactory.getLogger (MenuManager.class);

    private List<MenuProvider> menu_provider_list = new ArrayList<> ();

    @Bind (aggregate=true, optional=true, specification = MenuProvider.class)
    private void bindMenuProvider (MenuProvider menu_provider)
    {
        log.info ("bindMenuProvider: Adding {}", menu_provider);
        menu_provider_list.add (menu_provider);
    }

    @Unbind
    private void unbindMenuProvider (MenuProvider menu_provider)
    {
        log.info ("unbindMenuProvider: Removing {}", menu_provider);
    }

    @Override
    public MenuInstance newMenuInstance ()
    {
        return (new DefaultMenuInstance ());
    }
}

// EOF
