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

package org.lucidj.pkgdeployer;

import org.lucidj.api.DeploymentEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.jar.Attributes;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Context;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

@Component (immediate = true)
@Instantiate
@Provides (specifications = DeploymentEngine.class)
public class DefaultDeploymentEngine implements DeploymentEngine
{
    private final static transient Logger log = LoggerFactory.getLogger (DefaultDeploymentEngine.class);
    private final static int ENGINE_LEVEL = 1;

    @Context
    private BundleContext context;

    private Manifest get_manifest (String location)
    {
        FileInputStream file_stream = null;

        try
        {
            File jar_file = new File (new URI (location));
            file_stream = new FileInputStream (jar_file);
            JarInputStream jar_stream = new JarInputStream (file_stream);
            return (jar_stream.getManifest ());
        }
        catch (IOException e)
        {
            return (null);
        }
        catch (URISyntaxException e)
        {
            return (null);
        }
        finally
        {
            if (file_stream != null)
            {
                try
                {
                    file_stream.close ();
                }
                catch (Exception ignore) {};
            }
        }
    }

    @Override
    public String getEngineName ()
    {
        return (getClass ().getCanonicalName () + "(" + ENGINE_LEVEL + ")");
    }

    @Override
    public int compatibleArtifact (String location)
    {
        Manifest mf = get_manifest (location);

        if (mf == null)
        {
            // No manifest no glory
            return (0);
        }

        // We need at very least Bundle-SymbolicName on the manifest...
        Attributes attrs = mf.getMainAttributes ();

        // ...then we return the lowest compatibility, as fallback, to deploy any generic OSGi bundle
        return ((attrs != null && attrs.getValue ("Bundle-SymbolicName") != null)? 1: 0);
    }

    @Override
    public Bundle installBundle (String location)
    {
        try
        {
            return (context.installBundle (location));
        }
        catch (Exception e)
        {
            log.error ("Exception installing bundle: {}", location, e);
            return (null);
        }
    }

    @Override
    public boolean updateBundle (Bundle bnd)
    {
        try
        {
            log.info ("Updating package {}", bnd);
            bnd.stop (Bundle.STOP_TRANSIENT);
            bnd.update ();
            return (true);
        }
        catch (Exception e)
        {
            log.error ("Error updating {}", bnd, e);
            uninstallBundle (bnd);
            return (false);
        }
    }

    @Override
    public boolean uninstallBundle (Bundle bnd)
    {
        try
        {
            log.info ("Uninstalling bundle {}", bnd);
            bnd.uninstall ();
            return (true);
        }
        catch (Exception e)
        {
            log.error ("Exception uninstalling {}", bnd, e);
            return (false);
        }
    }
}

// EOF
