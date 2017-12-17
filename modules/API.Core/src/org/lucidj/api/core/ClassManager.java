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

package org.lucidj.api.core;

import java.util.Map;

import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.wiring.BundleWiring;

public interface ClassManager
{
    Class loadClass (String name);
    Class loadClassUsingClass (Class clazz, String name);
    Class loadClassUsingObject (Object obj, String name);
    Map<String, Bundle> getPackageMap ();
    ClassLoader getClassLoader ();

    static String objectHash (Object obj)
    {
        return (obj.getClass().getName() + "#" + Integer.toHexString (obj.hashCode()));
    }

    static boolean isZoombie (Class clazz)
    {
        Bundle declared_owner_bundle = FrameworkUtil.getBundle (clazz);

        if (declared_owner_bundle == null)
        {
            // No bundles, no zoombies
            return (false);
        }

        ClassLoader bundle_classloader = declared_owner_bundle.adapt (BundleWiring.class).getClassLoader ();

        // We compare the classloader from the class with the classloader from the bundle.
        // Of course we assume that the classloader from the class was not changed by the program.
        // If they differ, it means the class belongs to an invalid classloader.
        return (bundle_classloader != clazz.getClassLoader ());
    }

    static boolean isZoombie (Object object)
    {
        return (isZoombie (object.getClass ()));
    }
}

// EOF
