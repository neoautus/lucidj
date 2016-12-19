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

package org.lucidj.vaadinui;

import org.lucidj.api.BundleDeployer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Context;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;

@Component (immediate = true)
@Instantiate
class VaadinMapper implements HttpContext
{
    private final static transient Logger log = LoggerFactory.getLogger (VaadinMapper.class);
    private final static String VAADIN_RESOURCE_ALIAS = "/VAADIN";
    private final static String VAADIN_RESOURCE_NAME = "/VAADIN";

    @Requires
    private HttpService http_service;

    @Requires
    private BundleDeployer bundle_deployer;

    @Context
    private BundleContext context;

    private Bundle last_bundle;

    public String getMimeType (String name)
    {
        // Quick & dirty(TM)
        return (URLConnection.guessContentTypeFromName (name));
    }

    public URL look_for_resource (Bundle bundle, String resource_name)
    {
        return ((bundle != null)? bundle.getResource (resource_name): null);
    }

    public URL getResource (String name)
    {
        // TODO: SANITIZE name
        if (name.contains (".."))
        {
            return (null);
        }

        // Remove the resource name to obtain the resource spec component/path
        String resource_spec = name.substring (VAADIN_RESOURCE_NAME.length () + 1);

        URL found_resource = look_for_resource (last_bundle, name);

        if (found_resource == null)
        {
            Bundle[] bundle_list = context.getBundles ();

            for (Bundle bundle : bundle_list)
            {
                found_resource = look_for_resource (bundle, name);

                if (found_resource != null)
                {
                    last_bundle = bundle;
                    break;
                }
            }
        }

        log.info ("getResource ({}) => {}", name, found_resource);
        return (found_resource);
    }

    public boolean handleSecurity (HttpServletRequest request, HttpServletResponse response)
        throws IOException
    {
        // All public resources are always accessible
        return (true);
    }

    @Validate
    private void validate ()
    {
        try
        {
            log.info ("Starting Vaadin Mapper on {}", VAADIN_RESOURCE_ALIAS);
            http_service.registerResources (VAADIN_RESOURCE_ALIAS, VAADIN_RESOURCE_NAME, this);
        }
        catch (Exception e)
        {
            log.info ("Exception activating Vaadin Mapper", e);
        }
    }

    @Invalidate
    private void invalidate ()
    {
        log.info ("Stopping Vaadin Mapper on {}", VAADIN_RESOURCE_ALIAS);
        http_service.unregister (VAADIN_RESOURCE_ALIAS);
    }
}

// EOF
