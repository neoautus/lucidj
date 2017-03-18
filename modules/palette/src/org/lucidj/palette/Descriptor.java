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

import org.lucidj.api.ComponentDescriptor;

import com.vaadin.server.Resource;

import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

public class Descriptor implements ComponentDescriptor
{
    private String descriptor_id = "<" + this.toString () + ">";
    private String icon_title;
    private Resource icon_resource;
    private Class component_class;
    private Bundle component_bundle;

    @Override
    public void setDescriptorId (String id)
    {
        descriptor_id = id;
    }

    @Override
    public String getDescriptorId ()
    {
        return (descriptor_id);
    }

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
    public void setIcon (Resource icon_resource)
    {
        this.icon_resource = icon_resource;
    }

    @Override
    public Resource getIcon ()
    {
        return (icon_resource);
    }

    @Override
    public void setComponentClass (Class component_class)
    {
        this.component_class = component_class;
        component_bundle = FrameworkUtil.getBundle (component_class);
    }

    @Override
    public Class getComponentClass ()
    {
        return (component_class);
    }

    @Override
    public Bundle getComponentBundle ()
    {
        return (component_bundle);
    }
}

// EOF
