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

package org.lucidj.vaadinui;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;

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
class VaadinMapper extends HttpServlet implements HttpContext
{
    private final static Logger log = LoggerFactory.getLogger (VaadinMapper.class);
    private final static String VAADIN_RESOURCE_ALIAS = "/VAADIN";
    private final static String VAADIN_RESOURCE_NAME = "/VAADIN";

    // CSS content automagically generated from published styles.css
    public final static String VAADIN_DYNAMIC_STYLES_CSS = "/VAADIN/~/dynamic.css";

    @Requires (optional = true, filter = "(extension=css)")
    private URL[] published_css_files;

    @Requires
    private HttpService http_service;

    @Context
    private BundleContext context;

    private Bundle last_bundle;

    @Override // HttpContext
    public String getMimeType (String name)
    {
        // Quick & dirty(TM)
        return (URLConnection.guessContentTypeFromName (name));
    }

    public URL look_for_resource (Bundle bundle, String resource_name)
    {
        return ((bundle != null)? bundle.getResource (resource_name): null);
    }

    @Override // HttpContext
    public URL getResource (String name)
    {
        // TODO: SANITIZE name
        if (name.contains (".."))
        {
            return (null);
        }

        URL found_resource = look_for_resource (last_bundle, name);

        if (found_resource == null)
        {
            Bundle[] bundle_list = context.getBundles ();

            for (Bundle bundle: bundle_list)
            {
                found_resource = look_for_resource (bundle, name);

                if (found_resource != null)
                {
                    // TODO: ADD SOME CACHE
                    last_bundle = bundle;
                    break;
                }
            }
        }

        log.debug ("getResource ({}) => {}", name, found_resource);
        return (found_resource);
    }

    @Override // HttpContext
    public boolean handleSecurity (HttpServletRequest request, HttpServletResponse response)
        throws IOException
    {
        // All public resources are always accessible
        return (true);
    }

    private void handle_request (HttpServletRequest request, HttpServletResponse response)
    {
        try
        {
            String path_info = request.getPathInfo ();
            String servlet_path = request.getServletPath ();

            if (path_info == null && servlet_path.equals (VAADIN_DYNAMIC_STYLES_CSS))
            {
                byte[] buffer = new byte[1024];
                int buf_read;

                // Let's output a composite CSS
                response.setContentType ("text/css");
                OutputStream out = response.getOutputStream ();

                String timestamp = "/* Generated on " + new Date () + " */\n\n";
                out.write (timestamp.getBytes ());

                // Copy every published CSS
                for (URL url: published_css_files)
                {
                    // Simple header
                    String source = "/* [[" + url.toString () + "]] */\n";
                    out.write (source.getBytes ());

                    // Copy contents
                    try (InputStream is = url.openStream ())
                    {
                        while ((buf_read = is.read (buffer)) != -1)
                        {
                            out.write (buffer, 0, buf_read);
                        }
                    }
                }
            }
            else
            {
                response.sendError (HttpServletResponse.SC_NOT_FOUND);
            }

        }
        catch (Exception e)
        {
            log.error ("Exception on dynamic CSS generator", e);
        }
    }

    @Override // HttpServlet
    protected void doGet (HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException
    {
        handle_request (request, response);
    }

    @Override // HttpServlet
    protected void doPost (HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException
    {
        handle_request (request, response);
    }

    @Validate
    private void validate ()
    {
        try
        {
            log.info ("Starting Vaadin Mapper on {}", VAADIN_RESOURCE_ALIAS);
            http_service.registerResources (VAADIN_RESOURCE_ALIAS, VAADIN_RESOURCE_NAME, this);
            http_service.registerServlet (VAADIN_DYNAMIC_STYLES_CSS, this, null, this);
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
