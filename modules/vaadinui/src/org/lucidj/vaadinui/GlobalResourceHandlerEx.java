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

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.vaadin.server.ClassResource;
import com.vaadin.server.ClientConnector;
import com.vaadin.server.ConnectorResource;
import com.vaadin.server.DownloadStream;
import com.vaadin.server.GlobalResourceHandler;
import com.vaadin.server.Resource;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinResponse;
import com.vaadin.server.VaadinSession;
import com.vaadin.shared.ApplicationConstants;

import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

public class GlobalResourceHandlerEx extends GlobalResourceHandler
{
    private Map<String, BundleResource> path_to_resource = new HashMap<> ();
    private Map<String, ClientConnector> path_to_connector = new HashMap<> ();

    @Override
    public boolean handleRequest (VaadinSession session, VaadinRequest request, VaadinResponse response)
        throws IOException
    {
        // We simply filter the resources we want to serve directly
        if (path_to_resource.containsKey (request.getPathInfo ()))
        {
            DownloadStream stream = path_to_resource.get (request.getPathInfo ()).getStream ();

            if (stream != null)
            {
                stream.writeResponse (request, response);
                return (true);
            }

            // Fall back if not found here
        }

        return (super.handleRequest (session, request, response));
    }

    @Override
    public void register (Resource resource, ClientConnector ownerConnector)
    {
        // Business as usual
        super.register (resource, ownerConnector);

        // In order to show resources properly, we hook register() to filter
        // all ClassResources that can be transformed into BundleResources.
        // The trick is actually quite simple, we take the associatedClass and
        // use it to find out what bundle it belongs. With the source bundle,
        // getting the resource becomes a trivial bundle.getResource().

        // Do we need to override this resource?
        if (resource instanceof ClassResource)
        {
            // We use only explicit associatedClass, because the current UI available
            // on ClassResource may come from an unrelated bundle
            Class associated_class = (Class)get_object_field (resource, "associatedClass");
            String resource_name = (String)get_object_field (resource, "resourceName");

            // Is the magic possible??
            if (associated_class != null && resource_name != null)
            {
                // After super.register() we can retrieve the uri assigned internally
                String uri = super.getUri (ownerConnector, (ConnectorResource)resource);
                String path = uri.substring (ApplicationConstants.APP_PROTOCOL_PREFIX.length () - 1);

                // Now we can track the resource from uri down to its source bundle
                Bundle source_bundle = FrameworkUtil.getBundle (associated_class);
                path_to_resource.put (path, new BundleResource (source_bundle, resource_name));

                // Keep the linked connector allowing cleanup
                path_to_connector.put (path, ownerConnector);
            }
        }
    }

    @Override
    public void unregisterConnector (ClientConnector connector)
    {
        Iterator<Map.Entry<String, ClientConnector>> it = path_to_connector.entrySet().iterator();

        while (it.hasNext())
        {
            Map.Entry<String, ClientConnector> entry = it.next();

            if (entry.getValue ().equals (connector))
            {
                path_to_resource.remove (entry.getKey ());
                it.remove ();
            }
        }
    }

    //---------------------------
    // UGLY STUFF. DON'T LOOK :)
    //---------------------------

    // http://stackoverflow.com/questions/12485351/java-reflection-field-value-in-extends-class
    private Field find_underlying (Class<?> root_class, String fieldName)
    {
        Class<?> current = root_class;

        do
        {
            try
            {
                return (current.getDeclaredField (fieldName));
            }
            catch (Exception ignore) {};
        }
        while ((current = current.getSuperclass ()) != null);
        return (null);
    }

    private Object get_object_field (Object object, String field_name)
    {
        try
        {
            Field field = find_underlying (object.getClass (), field_name);
            field.setAccessible (true);
            return (field.get (object));
        }
        catch (Exception ignore) {};
        return (null);
    }
}

// EOF
