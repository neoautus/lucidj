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
import org.lucidj.api.DeploymentInstance;
import org.lucidj.api.EmbeddingContext;
import org.lucidj.api.EmbeddingManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Properties;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

@Component (immediate = true, publicFactory = false)
@Instantiate
@Provides
public class PackageDeploymentEngine implements DeploymentEngine
{
    private final static Logger log = LoggerFactory.getLogger (PackageDeploymentEngine.class);
    private final static int ENGINE_LEVEL = 50;

    public final static String ATTR_PACKAGE = "X-Package";
    public final static String ATTR_PACKAGE_VERSION = "1.0";

    @Requires
    private BundleManager bundleManager;

    @Requires
    private EmbeddingManager embeddingManager;

    private String packages_dir;

    public PackageDeploymentEngine ()
    {
        // TODO: THIS SHOULD BE RECONFIGURABLE
        packages_dir = System.getProperty ("system.home") + "/cache/" + this.getClass ().getPackage ().getName ();

        File check_packages_dir = new File (packages_dir);

        if (!check_packages_dir.exists ())
        {
            if (check_packages_dir.mkdir ())
            {
                log.info ("Creating cache {}", packages_dir);
            }
            else
            {
                log.error ("Error creating cache {}", packages_dir);
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
        // LEAP requires only the proper package extension
        File location_file = new File (location);
        return (location_file.getName ().toLowerCase ().endsWith (".leap")? ENGINE_LEVEL: 0);
    }

    @Override
    public DeploymentInstance install (String location, Properties properties)
        throws Exception
    {
        EmbeddingContext embedding_context = embeddingManager.newEmbeddingContext ();
        DeploymentInstance instance = new PackageInstance (embedding_context, bundleManager, packages_dir);
        instance.install (location, properties);
        return (instance);
    }
}

// EOF
