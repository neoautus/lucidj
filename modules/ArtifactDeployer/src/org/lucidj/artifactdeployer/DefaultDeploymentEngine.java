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
import org.lucidj.api.DeploymentEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.osgi.framework.Bundle;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

@Component (immediate = true, publicFactory = false)
@Instantiate
@Provides
public class DefaultDeploymentEngine implements DeploymentEngine
{
    private final static transient Logger log = LoggerFactory.getLogger (DefaultDeploymentEngine.class);
    private final static int ENGINE_LEVEL = 1;

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
        // The default handling only applies to .jar
        if (!location.toLowerCase ().endsWith (".jar"))
        {
            // This way we avoid confusion with .leap and others
            return (0);
        }

        Manifest mf = bundle_manager.getManifest (location);

        if (mf == null)
        {
            // No manifest no glory
            return (0);
        }

        // We need at very least Bundle-SymbolicName on the manifest...
        Attributes attrs = mf.getMainAttributes ();

        // ...then we return the lowest compatibility, as fallback, to deploy any generic OSGi bundle
        return ((attrs != null && attrs.getValue ("Bundle-SymbolicName") != null)? ENGINE_LEVEL: 0);
    }

    @Override
    public int getState (Bundle bnd)
    {
        // We handle only simple bundles
        // TODO: HANDLE open() AND close()
        return (bnd.getState ());
    }

    @Override
    public Bundle install (String location, Properties properties)
    {
        return (bundle_manager.installBundle (location, properties));
    }

    @Override
    public boolean open (Bundle bnd)
    {
        // Always open
        return (true);
    }

    @Override
    public boolean close (Bundle bnd)
    {
        // Always close
        return (true);
    }

    @Override
    public boolean update (Bundle bnd)
    {
        return (bundle_manager.updateBundle (bnd));
    }

    @Override
    public boolean refresh (Bundle bnd)
    {
        return (bundle_manager.refreshBundle (bnd));
    }

    @Override
    public boolean uninstall (Bundle bnd)
    {
        return (bundle_manager.uninstallBundle (bnd));
    }
}

// EOF
