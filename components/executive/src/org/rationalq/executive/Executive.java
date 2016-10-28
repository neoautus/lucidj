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

package org.rationalq.executive;

import org.lucidj.system.SystemAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.osgi.framework.BundleContext;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Context;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;

// TODO: DO WE ACTUALLY NEED THIS?

@Component
@Instantiate
public class Executive
{
    private final transient Logger log = LoggerFactory.getLogger (Executive.class);

    @Context
    private BundleContext ctx;

    @Requires
    private SystemAPI sapi;

    public Executive ()
    {
        log.info ("Executive loader");
    }

    @Validate
    public void start ()
    {
        log.info ("Executive START");
    }
}

// EOF
