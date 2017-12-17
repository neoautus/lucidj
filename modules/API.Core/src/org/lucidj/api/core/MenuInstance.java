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

package org.lucidj.api.core;

import java.util.Map;
import java.util.TreeSet;

public interface MenuInstance
{
    void setMenuManager (MenuManager menu_manager);
    TreeSet<MenuEntry> getMenuEntries ();
    Map<String, Object> properties ();
    MenuEntry newMenuEntry (String title, Object icon, int weight, String navid);
    void addMenuEntry (MenuEntry menu_entry);
    void removeMenuEntry (MenuEntry menu_entry);
    void menuChanged (MenuProvider menu_provider);

    void setEventListener (EventListener listener);
    void fireEventEntrySelected (MenuEntry entry);

    interface EventListener
    {
        void entrySelectedEvent (MenuEntry entry);
    }
}

// EOF
