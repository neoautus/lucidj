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

import java.io.File;
import java.util.Map;
import java.util.Properties;
import java.util.jar.Manifest;

import org.osgi.framework.Bundle;
import org.osgi.framework.Version;

public interface BundleManager
{
    String PROP_LOCATION      = "location";
    String PROP_LAST_MODIFIED = "last-modified";
    String PROP_BUNDLE_STATE  = "bundle-status";

    Manifest   getManifest            (String location);
    Manifest   getManifest            (File jar);
    Bundle     getBundleByDescription (String symbolic_name, Version version);
    Bundle     getBundleByLocation    (String location);
    Bundle     getBundleByProperty    (String property, String value);
    Map<Bundle, Properties> getBundles ();
    Properties getBundleProperties    (Bundle bnd);
    Bundle     installBundle          (String location, Properties properties);
    boolean    updateBundle           (Bundle bnd);
    boolean    refreshBundle          (Bundle bnd);
    boolean    uninstallBundle        (Bundle bnd);
}

// EOF
