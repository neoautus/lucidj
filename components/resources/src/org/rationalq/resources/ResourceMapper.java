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

package org.rationalq.resources;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.felix.ipojo.annotations.*;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.osgi.util.tracker.BundleTracker;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URL;
import java.util.*;

@Component
@Instantiate
public class ResourceMapper
{
    private final transient static Logger log = LoggerFactory.getLogger (ResourceMapper.class);

    private Map<String, BundleHttpContext> bnd_map = new HashMap<>();
    private BundleTracker bnd_tracker;

    @Context BundleContext bundle_context;
    @Requires HttpService http_service;

    private void clear_resources (Bundle bnd)
    {
        String bundle_id = String.valueOf(bnd.getBundleId());

        BundleHttpContext bc = bnd_map.get (bundle_id);

        if (bc != null)
        {
            bnd_map.remove(bundle_id);
            bc.clear_mappings();
        }
    }

    private void scan_and_map_resources (Bundle bnd)
    {
        Dictionary header = bnd.getHeaders ();
        BundleHttpContext bc = new BundleHttpContext (bnd);

        // We always map /public directories to outside
        if (bnd.findEntries ("/public", null, false) != null)
        {
            bc.map_resource ("vaadin://" + bnd.getSymbolicName (), "/public");
        }

        String mappings = (String)header.get ("Resource-Mapping");

        if (mappings == null)
        {
            return;
        }

        log.info ("Resource-Mapping: [{}] {}", bnd.getSymbolicName (), mappings);

        clear_resources (bnd);

        String bundle_id = String.valueOf (bnd.getBundleId ());

        bnd_map.put (bundle_id, bc);

        String[] map_list = mappings.trim ().split ("\\s*\\,\\s*");

        for (int i = 0; i < map_list.length; i++)
        {
            log.debug ("[3] " + map_list [i]);

            if (map_list [i].isEmpty ())
            {
                log.debug ("[3] SKIP");
                continue;
            }

            String[] args = map_list [i].split ("\\s*;\\s*");

            if (args.length != 2)
            {
                log.error ("Mapping error: '{}'", map_list [i]);
                continue;
            }

            bc.map_resource (args [0], args [1]);
        }
    }

    @Validate
    public void validate ()
    {
        bnd_tracker = new DynamicComponentTracker ();
        bnd_tracker.open ();
    }

    @Invalidate
    public void invalidate ()
    {
        bnd_tracker.close();
        bnd_tracker = null;
    }

    class BundleHttpContext implements HttpContext
    {
        private Bundle res_bundle;
        private List<String> res_aliases = new LinkedList<> ();

        public BundleHttpContext (Bundle resource)
        {
            res_bundle = resource;
        }

        public String getMimeType (String name)
        {
            if (name.endsWith (".css"))
            {
                return ("text/css");
            }
            else if (name.endsWith (".html"))
            {
                return ("text/html");
            }
            else if (name.endsWith (".js"))
            {
                return ("application/x-javascript");
            }
            else if (name.endsWith (".gif"))
            {
                return ("image/gif");
            }
            else if (name.endsWith (".ico"))
            {
                return ("image/x-icon");
            }
            else if (name.endsWith (".png"))
            {
                return ("image/png");
            }
            else if (name.endsWith (".jpeg") || name.endsWith (".jpg"))
            {
                return ("image/jpeg");
            }
            else if (name.endsWith (".swf"))
            {
                return ("application/x-shockwave-flash");
            }

            return (null);
        }

        public URL getResource (String name)
        {
            log.debug ("getResource: " + name);

            // Somente sao retornados objetos simples
            if (/*getMimeType (name) == null || */ name.contains (".."))
            {
                // Tudo mais nao existe
                return (null);
            }

            // Returns the resource from its bundle
            return (res_bundle.getResource (name));
        }

        public boolean handleSecurity (HttpServletRequest request, HttpServletResponse response)
                throws IOException
        {
            // All static resources are always accessible
            return (true);
        }

        public void map_resource (String alias, String name)
        {
            if (alias.startsWith("vaadin://"))
            {
                alias = "/VAADIN/" + alias.substring("vaadin://".length());
            }

            if (!name.startsWith ("/"))
            {
                name = "/" + name;
            }

            if (alias.contains("~"))
            {
                alias = alias.replace("~", name.substring (1));
            }

            log.debug ("DEPLOYING '" + alias + "' => '" + name + "'");

            try
            {
                http_service.registerResources (alias, name, this);
                res_aliases.add (alias);
                log.info ("Deployed mapping: [{}]:{} alias '{}' ", res_bundle.getSymbolicName (), name, alias);
            }
            catch (Exception e)
            {
                log.error ("Error deploying mapping", e);
            }
        }

        public void clear_mappings ()
        {
            long bundle_id_to_clear = res_bundle.getBundleId();

            log.debug ("Clear_mappings (" + res_aliases.size() + "): bnd=" + res_bundle +
                      " : " + bundle_id_to_clear);

            for (String alias: res_aliases)
            {
                log.info ("Removing mapping alias: {}", alias);

                http_service.unregister(alias);
            }
        }
    }

    class DynamicComponentTracker extends BundleTracker
    {
        DynamicComponentTracker ()
        {
            super (bundle_context, Bundle.ACTIVE, null);
        }

        @Override
        public Object addingBundle (Bundle bundle, BundleEvent event)
        {
            scan_and_map_resources (bundle);
            return (bundle);
        }

        @Override
        public void removedBundle (Bundle bundle, BundleEvent event, Object t)
        {
            clear_resources(bundle);
        }
    }
}

// EOF
