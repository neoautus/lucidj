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
import org.osgi.framework.Version;

public interface ArtifactDeployer
{
    Bundle getArtifactByDescription (String symbolic_name, Version version);
    Bundle installArtifact (String location) throws Exception;
    boolean openArtifact (Bundle bnd);
    boolean closeArtifact (Bundle bnd);
    boolean updateArtifact (Bundle bnd);
    boolean refreshArtifact (Bundle bnd);
    boolean uninstallArtifact (Bundle bnd);
    Bundle getArtifactByLocation (String location);
}

// EOF
