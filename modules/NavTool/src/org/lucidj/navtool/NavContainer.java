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

package org.lucidj.navtool;

import org.lucidj.api.vui.NavTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.data.util.HierarchicalContainer;
import com.vaadin.server.Resource;

import java.net.URI;
import java.util.Date;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.concurrent.atomic.AtomicInteger;

import org.osgi.framework.ServiceRegistration;

public class NavContainer extends HierarchicalContainer
{
    private final static Logger log = LoggerFactory.getLogger (NavToolService.class);

    private ServiceRegistration service_registration;
    private Dictionary<String, Object> properties = new Hashtable<> ();
    private AtomicInteger handle_counter = new AtomicInteger (NavTool.HANDLE_AUTO_START_RANGE);

    public NavContainer ()
    {
        addContainerProperty (NavTool.PROPERTY_NAME, String.class, "-");
        addContainerProperty (NavTool.PROPERTY_ICON, Resource.class, null);
        addContainerProperty (NavTool.PROPERTY_SIZE, Long.class, -1);
        addContainerProperty (NavTool.PROPERTY_LASTMODIFIED, Date.class, null);
        addContainerProperty (NavTool.PROPERTY_URI, URI.class, null);
    }

    public int newHandle ()
    {
        return (handle_counter.incrementAndGet ());
    }

    public Dictionary<String, Object> getProperties ()
    {
        return (properties);
    }

    public void setServiceRegistration (ServiceRegistration service_registration)
    {
        this.service_registration = service_registration;
    }

    public void updateProperties (Dictionary<String, Object> properties)
    {
        log.info ("NavContainer: updateProperties: service_registration={} {}", service_registration, properties);
        service_registration.setProperties (properties);
    }
}

// EOF
