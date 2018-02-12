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

package org.lucidj.artifactdeployer;

import org.lucidj.api.core.Artifact;
import org.lucidj.api.core.ArtifactDeployer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.ServiceProperty;

@Component (immediate = true, publicFactory = false)
@Instantiate
@Provides (specifications = HttpServlet.class)
public class DeploySvc extends HttpServlet
{
    private final static Logger log = LoggerFactory.getLogger (DeploySvc.class);

    @Requires
    private ArtifactDeployer artifactDeployer;

    @ServiceProperty (name="@service.path")
    String service = "/deploy";

    @Override
    public void doGet (HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException
    {
        resp.setContentType ("text/plain");
        PrintWriter out = resp.getWriter ();
        String artifact_file = req.getQueryString ();

        if (artifact_file == null)
        {
            // Just the service returns true indicating it exists
            out.write ("true\n");
            return;
        }

        out.write ("### Hello from " + this.getClass ().getName () + " ###\n");
        out.write ("Artifact: " + artifact_file + "\n");

        log.info ("Install requested: {}", artifact_file);
        Artifact artifact = artifactDeployer.getArtifact (artifact_file);

        if (artifact == null)
        {
            try
            {
                artifact = artifactDeployer.installArtifact (artifact_file);
                out.write ("Artifact installed: " + artifact.toString ());
            }
            catch (Exception e)
            {
                out.write ("Exception deploying file: " + e.getMessage ());
            }
        }
        else
        {
            out.write ("Update artifact: " + artifact.toString ());
            artifact.update ();
        }
    }
}

// EOF