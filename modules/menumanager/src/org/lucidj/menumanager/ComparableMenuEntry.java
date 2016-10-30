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

import org.lucidj.api.MenuEntry;

public class ComparableMenuEntry implements MenuEntry, Comparable<ComparableMenuEntry>
{
    private String title;
    private int weight;
    private Object icon;
    private String navid;
    private String options;
    private String badge;

    public ComparableMenuEntry (String title, Object icon, int weight, String navid)
    {
        this.title = title;
        this.icon = icon;
        this.weight = weight;
        this.navid = navid;

        options = "";
        badge = "";
    }

    public String toString ()
    {
        // The weight is used to group titles
        return (String.format ("%05d:%s:", weight, title));
    }

    @Override // Comparable<ComparableMenuEntry>
    public int compareTo (ComparableMenuEntry o)
    {
        return (toString ().compareTo (o.toString ()));
    }

    @Override // MenuEntry
    public String getTitle ()
    {
        return (title);
    }

    @Override // MenuEntry
    public int getWeight ()
    {
        return (weight);
    }

    @Override // MenuEntry
    public Object getIcon ()
    {
        return (icon);
    }

    @Override // MenuEntry
    public String getNavId ()
    {
        return (navid);
    }

    @Override // MenuEntry
    public String getBadge ()
    {
        return (badge);
    }

    @Override // MenuEntry
    public String getOptions ()
    {
        return (options);
    }
}

// EOF
