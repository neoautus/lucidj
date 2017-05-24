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

package org.lucidj.pkgdeployer;

import org.lucidj.api.BundleManager;
import org.lucidj.api.DeploymentEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URI;
import java.util.Properties;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Context;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

@Component (immediate = true)
@Instantiate
@Provides (specifications = DeploymentEngine.class)
public class SourcePackageDeploymentEngine implements DeploymentEngine
{
    private final static transient Logger log = LoggerFactory.getLogger (SourcePackageDeploymentEngine.class);
    private final static int ENGINE_LEVEL = 100;

    @Context
    private BundleContext context;

    @Requires
    private BundleManager bundle_manager;

    @Override
    public String getEngineName ()
    {
        return (getClass ().getCanonicalName () + "(" + ENGINE_LEVEL + ")");
    }

    @Override
    public int compatibleArtifact (String location)
    {
        try
        {
            File bundle_dir = new File (new URI (location));

            if (bundle_dir.isDirectory ())
            {
                Manifest mf;
                Attributes attrs;

                // Check compatibility looking for X-Package attribute on manifest
                if ((mf = bundle_manager.getManifest (location)) != null &&
                    (attrs = mf.getMainAttributes ()) != null &&
                    attrs.getValue ("X-Package") != null)
                {
                    return (ENGINE_LEVEL);
                }
            }
        }
        catch (Exception ignore) {};

        // Not compatible
        return (0);
    }

    @Override
    public int getState (Bundle bnd)
    {
        // TODO: HANDLE open() AND close()
        return 0;
    }

    @Override
    public Bundle install (String location, Properties properties)
    {
        Manifest mf = bundle_manager.getManifest (location);

        if (mf == null)
        {
            return (null);
        }

        File[] embedded_bundles = null;
        
        try
        {
            File location_dir = new File (new URI (location));
            File bundles_dir = new File (location_dir, "/Bundles");
            embedded_bundles = bundles_dir.listFiles ();
        }
        catch (Exception ignore) {};
        boolean got_errors = false;

        if (embedded_bundles != null)
        {
            for (File bundle_file: embedded_bundles)
            {
                if (bundle_file.isFile ())
                {
                    String bundle_uri = bundle_file.toURI ().toString ();

                    if (bundle_manager.installBundle (bundle_uri, properties) == null)
                    {
                        got_errors = true;
                    }
                }
            }
        }

        if (!got_errors)
        {
            try
            {
                // Install the exploded bundle using 'reference:'
                return (bundle_manager.installBundle ("reference:" + location, properties));
            }
            catch (Exception e)
            {
                log.error ("Exception installing package: {}", location, e);
            }
        }

        log.error ("Errors found when deploying embedded bundles -- will not install package.");
        return (null);
    }

    @Override
    public boolean open (Bundle bnd)
    {
        // TODO: CHECK!!!
        return (false);
    }

    @Override
    public boolean close (Bundle bnd)
    {
        // TODO: CHECK!!!
        return (false);
    }

    @Override
    public boolean update (Bundle bnd)
    {
        // TODO: HANDLE Bundles/ AND Resources/
        return (bundle_manager.updateBundle (bnd));
    }

    @Override
    public boolean refresh (Bundle bnd)
    {
        return (false);
        // TODO: HANDLE Bundles/ AND Resources/
        //return (bundle_manager.refreshBundle (bnd));
    }

    @Override
    public boolean uninstall (Bundle bnd)
    {
        // TODO: HANDLE Bundles/ AND Resources/
        return (bundle_manager.uninstallBundle (bnd));
    }
}

// EOF
