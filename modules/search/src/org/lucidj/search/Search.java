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

package org.lucidj.search;

import org.lucidj.api.MenuInstance;
import org.lucidj.api.MenuProvider;

import com.vaadin.server.FontAwesome;

import java.util.Map;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

@Component
@Instantiate
@Provides (specifications = MenuProvider.class)
public class Search implements MenuProvider
{
    @Override
    public Map<String, Object> getProperties ()
    {
        return (null);
    }

    @Override
    public void buildMenu (MenuInstance menu, Map<String, Object> properties)
    {
        menu.addMenuEntry (menu.newMenuEntry ("Search", FontAwesome.SEARCH, 250, "search"));
    }
}

// EOF
