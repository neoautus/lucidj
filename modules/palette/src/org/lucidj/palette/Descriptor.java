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

package org.lucidj.palette;

import org.lucidj.api.core.ComponentDescriptor;

public class Descriptor implements ComponentDescriptor
{
    private String icon_title;
    private String icon_url;
    private String component_class;

    @Override
    public void setIconTitle (String icon_title)
    {
        this.icon_title = icon_title;
    }

    @Override
    public String getIconTitle ()
    {
        return (icon_title);
    }

    @Override
    public void setIconUrl (String url)
    {
        icon_url = url;
    }

    @Override
    public String getIconUrl ()
    {
        return (icon_url);
    }

    @Override
    public void setComponentClass (String component_class)
    {
        this.component_class = component_class;
    }

    @Override
    public String getComponentClass ()
    {
        return (component_class);
    }
}

// EOF
