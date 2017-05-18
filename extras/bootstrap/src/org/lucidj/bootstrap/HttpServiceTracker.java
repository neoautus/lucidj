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

package org.lucidj.bootstrap;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.HttpContext;
import org.osgi.util.tracker.ServiceTracker;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class HttpServiceTracker extends ServiceTracker<HttpService, HttpService>
{
    private TinyLog log = new TinyLog ();

    private final String APP_CONTEXT = "/bootstrap";
    private BootstrapDeployer deployer;

    public HttpServiceTracker (BundleContext context, BootstrapDeployer deployer)
    {
        super (context, HttpService.class.getName (), null);
        this.deployer = deployer;
    }

    @Override
    public HttpService addingService (ServiceReference<HttpService> reference)
    {
        HttpService http_service = context.getService (reference);

        try
        {
            HttpServlet servlet = new HttpServlet ()
            {
                @Override
                protected void doGet (HttpServletRequest req, HttpServletResponse resp)
                    throws ServletException, IOException
                {
                    resp.setContentType ("text/plain");
                    resp.getWriter().write ("bootstrap_finished=" + deployer.bootstrapFinished () + "\n");
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

        log.info ("Application servlet stopped: {}", APP_CONTEXT);

        super.removedService (reference, service);
    }
}

// EOF
