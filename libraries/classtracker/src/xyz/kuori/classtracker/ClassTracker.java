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

package xyz.kuori.classtracker;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.util.tracker.BundleTracker;

import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.*;

public class ClassTracker extends BundleTracker
{
    private Map<Class, Bundle> tracked_classes = null;
    private BundleContext ctx;
    private Class ref_interface;

    @SuppressWarnings("unchecked")
    public ClassTracker (BundleContext ctx, Class ref_interface)
    {
        super (ctx, Bundle.ACTIVE, null);

        this.ctx = ctx;
        this.ref_interface = ref_interface;
    }

    @Override
    public void open()
    {
        tracked_classes = new HashMap<>();
        super.open();
    }

    @Override
    public void close()
    {
        super.close();
        tracked_classes = null;
    }

    public boolean addingClass (Bundle bnd, Class ref_class)
    {
        // true: add, false: skip
        return (true);
    }

    @SuppressWarnings("unchecked")
    protected void scanClass (Bundle bnd, String class_name)
    {
        // No subclasses here!
        if (class_name.indexOf ('$') != -1)
        {
            return;
        }

        Class cls = null;

        try
        {
            // For now, we see only global exports
            cls = bnd.loadClass (class_name);
        }
        catch (NoClassDefFoundError e)
        {
            return;
        }
        catch (ClassNotFoundException e)
        {
            return;
        }
        catch (Exception e)
        {
            return;
        }

        int modifiers = cls.getModifiers ();

        // Is the class public?
        if (!Modifier.isPublic(modifiers))
        {
            return;
        }

        // Do not track interfaces
        if (Modifier.isInterface(modifiers))
        {
            return;
        }

        if (ref_interface.isAssignableFrom (cls))
        {
            if (addingClass(bnd, cls))
            {
                tracked_classes.put(cls, bnd);
            }
        }
    }

    public boolean filterBundle (Bundle bnd)
    {
        // If not overriden, will scan ALL bundles
        return (true);
    }

    protected void scanBundle (Bundle bnd)
    {
        // User defined bundle filtering, scan all if not overriden
        if (!filterBundle(bnd))
        {
            return;
        }

        // Returns the URLs for all classes contained into the bundle
        Enumeration url_enum = bnd.findEntries ("/", "*.class", true);

        if (url_enum == null)
        {
            // Nothing here for us
            return;
        }

        while (url_enum.hasMoreElements ())
        {
            URL bnd_entry = (URL)url_enum.nextElement();

            // Like /packaged/Classified.class
            String class_name = bnd_entry.getFile ();

            // Turns into packaged.Classified
            class_name = class_name.substring (1, class_name.lastIndexOf ('.'))
                                   .replace ('/', '.');

            scanClass(bnd, class_name);
        }
    }

    @Override
    public Object addingBundle (Bundle bundle, BundleEvent event)
    {
        scanBundle (bundle);
        return (bundle);
    }

    public void removedClass (Bundle bnd, Class ref_class)
    {
        // Placeholder
    }

    @Override
    public void removedBundle(Bundle bundle, BundleEvent event, Object object)
    {
        Iterator<Map.Entry<Class,Bundle>> it = tracked_classes.entrySet().iterator();

        while (it.hasNext())
        {
            Map.Entry<Class, Bundle> entry = it.next();

            if (entry.getValue() == bundle)
            {
                removedClass(bundle, entry.getKey());
                it.remove();
            }
        }
    }
}

// EOF
