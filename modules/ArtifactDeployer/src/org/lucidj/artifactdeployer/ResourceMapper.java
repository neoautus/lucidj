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

package org.lucidj.artifactdeployer;

import org.lucidj.api.BundleManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

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

@Component (immediate = true, publicFactory = false)
@Instantiate
class ResourceMapper implements HttpContext
{
    private final static Logger log = LoggerFactory.getLogger (ResourceMapper.class);
    private final static String PUBLIC_RESOURCE_ALIAS = "/VAADIN/~";
    private final static String PUBLIC_RESOURCE_NAME = "/VAADIN/~";

    @Requires
    private HttpService http_service;

    @Requires
    private BundleManager bundleManager;

    @Context
    public BundleContext context;

    public String getMimeType (String name)
    {
        // Quick & dirty(TM)
        return (URLConnection.guessContentTypeFromName (name));
    }

    public URL getResource (String name)
    {
        // TODO: SANITIZE name
        if (name.contains (".."))
        {
            return (null);
        }

        // Remove the resource name to obtain the resource spec component/path
        String resource_spec = name.substring (PUBLIC_RESOURCE_NAME.length () + 1);

        if (resource_spec.contains ("/"))
        {
            int resource_path_pos = resource_spec.indexOf ('/');
            String resource_component = resource_spec.substring (0, resource_path_pos);
            String resource_path = resource_spec.substring (resource_path_pos + 1);

            // Component is a bundle symbolic name
            Bundle resource_bundle = bundleManager.getBundleByDescription (resource_component, null);

            log.debug ("resource_component={}, resource_path=>{}<, resource_bundle={}",
                resource_component, resource_path, resource_bundle);

            // Find the bundle specified on the URL
            if (resource_bundle != null)
            {
                URL resource = resource_bundle.getResource ("/Public/" + resource_path);

                if (resource != null)
                {
                    return (resource);
                }

                return (resource_bundle.getResource ("/public/" + resource_path));
            }
            else // This may be a reference to system/public (if it exists) like /VAADIN/~/system/file.ext
            {
                Path system_public = Paths.get (System.getProperty ("system.home"), "system", "public");

                if (Files.exists (system_public))
                {
                    // Try to resolve the resource as a file under $LUCIDJ_HOME/system/public
                    try
                    {
                        return (system_public.resolve (resource_path).toUri ().toURL ());
                    }
                    catch (MalformedURLException e)
                    {
                        log.warn ("Exception resolving: {}", name, e);
                    }
                }
            }
        }
        // Resource not found
        return (null);
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
            log.info ("Starting Resource Mapper on {}", PUBLIC_RESOURCE_ALIAS);
            http_service.registerResources (PUBLIC_RESOURCE_ALIAS, PUBLIC_RESOURCE_NAME, this);
        }
        catch (Exception e)
        {
            log.info ("Exception activating Resource Mapper", e);
        }
    }

    @Invalidate
    private void invalidate ()
    {
        log.info ("Stopping Resource Mapper on {}", PUBLIC_RESOURCE_ALIAS);
        http_service.unregister (PUBLIC_RESOURCE_ALIAS);
    }
}

// EOF
