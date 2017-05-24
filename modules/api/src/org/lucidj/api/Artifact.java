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

package org.lucidj.api;

import org.osgi.framework.Bundle;

public interface Artifact
{
    // Normal lifecycle, mirrors OSGi
    int	STATE_UNINSTALLED = Bundle.UNINSTALLED;
    int	STATE_INSTALLED   = Bundle.INSTALLED;
    int	STATE_RESOLVED    = Bundle.RESOLVED;
    int	STATE_STARTING    = Bundle.STARTING;
    int	STATE_STOPPING    = Bundle.STOPPING;    // Automatically fires CLOSING if state is RUNNING
    int	STATE_ACTIVE      = Bundle.ACTIVE;

    // Extended lifecycle inside ACTIVE state
    int STATE_EX_OPENING  = -1;    // Opening the provided services
    int STATE_EX_RUNNING  = -2;    // All services up and running
    int STATE_EX_CLOSING  = -3;    // Closing provided services

    String PROP_SOURCE             = ".Artifact-Source";
    String PROP_DEPLOYMENT_ENGINE  = ".Artifact-Deployment-Engine";
    String PROP_LOCATION           = ".Artifact-Location";
    String PROP_LAST_MODIFIED      = ".Artifact-Last-Modified";
    String PROP_BUNDLE_STATE       = ".Artifact-Bundle-State";
    String PROP_BUNDLE_STATE_HUMAN = ".Artifact-Bundle-State-Human";
    String PROP_BUNDLE_START       = ".Artifact-Bundle-Start";
}

// EOF
