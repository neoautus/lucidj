/*
 * Copyright 2018 NEOautus Ltd. (http://neoautus.com)
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

package org.lucidj.bootstrap;

import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.HttpContext;
import org.osgi.util.tracker.ServiceTracker;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.InetAddress;
import java.util.HashSet;
import java.util.Set;

public class HttpServiceTracker extends ServiceTracker<HttpService, HttpService>
{
    private TinyLog log = new TinyLog ();

    private final String APP_CONTEXT = "/~localsvc";
    private BootstrapDeployer deployer;
    private BundleContext context;

    private Set<String> local_addrs;

    public HttpServiceTracker (BundleContext context, BootstrapDeployer deployer)
    {
        super (context, HttpService.class.getName (), null);
        this.deployer = deployer;
        this.context = context;
    }

    private boolean is_local (HttpServletRequest req)
        throws ServletException
    {
        if (local_addrs == null)
        {
            local_addrs = new HashSet<> ();

            try
            {
                // localhost ipv4/ipv6
                local_addrs.add ("127.0.0.1");
                local_addrs.add ("0:0:0:0:0:0:0:1");

                // Hostname may have another address(es)
                String hostname = InetAddress.getLocalHost().getHostName();
                for (InetAddress inetAddress: InetAddress.getAllByName (hostname))
                {
                    local_addrs.add (inetAddress.getHostAddress());
                }
            }
            catch (IOException e)
            {
                log.warn ("Exception retrieving local addresses", e);
                throw (new ServletException ("Unable to lookup local addresses"));
            }
        }
        return (local_addrs.contains (req.getRemoteAddr ()));
    }

    @Override
    @SuppressWarnings ("unchecked")
    public HttpService addingService (ServiceReference<HttpService> reference)
    {
        HttpService http_service = context.getService (reference);

        try
        {
            HttpServlet servlet = new HttpServlet ()
            {
                @Override
                protected void service (HttpServletRequest req, HttpServletResponse resp)
                    throws ServletException, IOException
                {
                    String path = req.getPathInfo ();

                    // We allow only local requests for ~localsvc
                    if (!is_local (req))
                    {
                        resp.sendError (HttpServletResponse.SC_FORBIDDEN);
                        return;
                    }

                    // Compatibility
                    if (path == null || path.equals ("/"))
                    {
                        resp.setContentType ("text/plain");
                        resp.getWriter().write ("bootstrap_finished=" + deployer.bootstrapFinished () + "\n");
                        return;
                    }

                    ServiceReference[] refs = null;

                    // Look for the invoked service (/deploy, /status, etc)
                    try
                    {
                        String filter = "(@service.path=" + path + ")";
                        refs = context.getServiceReferences (HttpServlet.class.getName (), filter);
                    }
                    catch (InvalidSyntaxException ignore) {};

                    if (refs == null || refs.length != 1)
                    {
                        resp.sendError (HttpServletResponse.SC_NOT_FOUND);
                        return;
                    }

                    // Invoke the service
                    try
                    {
                        HttpServlet mservlet = (HttpServlet)context.getService (refs [0]);
                        mservlet.service (req, resp);
                    }
                    finally
                    {
                        context.ungetService (refs [0]);
                    }
                }
            };

            HttpContext ctx = http_service.createDefaultHttpContext ();
            http_service.registerServlet (APP_CONTEXT, servlet, null, ctx);

            log.info ("Bootstrap servlet started: {}", APP_CONTEXT);
        }
        catch (Exception e)
        {
            log.error ("Error adding HttpService {}", APP_CONTEXT, e);
        }

        return (http_service);
    }

    @Override
    public void removedService(ServiceReference<HttpService> reference, HttpService service)
    {
        service.unregister (APP_CONTEXT);

        log.info ("Bootstrap servlet stopped: {}", APP_CONTEXT);

        super.removedService (reference, service);
    }
}

// EOF
