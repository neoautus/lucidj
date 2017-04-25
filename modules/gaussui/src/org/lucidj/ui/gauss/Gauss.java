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

package org.lucidj.ui.gauss;

import org.lucidj.api.DesktopInterface;
import org.lucidj.api.ManagedObject;
import org.lucidj.api.ManagedObjectFactory;
import org.lucidj.api.ManagedObjectInstance;
import org.lucidj.api.ManagedObjectProvider;
import org.lucidj.api.MenuManager;
import org.lucidj.api.NavigatorManager;
import org.lucidj.api.RendererFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;

@Component (immediate = true, publicFactory = false)
@Instantiate
@Provides
public class Gauss implements ManagedObjectProvider
{
    private final static transient Logger log = LoggerFactory.getLogger (Gauss.class);

    @Requires
    private ManagedObjectFactory objectFactory;

    @Requires
    private MenuManager menuManager;

    @Requires
    private NavigatorManager navigatorManager;

    @Requires
    private RendererFactory rendererFactory;

    @Validate
    private boolean validate ()
    {
        log.info ("Gauss UI Provider started");
        objectFactory.register (DesktopInterface.class, this, null);
        return (true);
    }

    @Invalidate
    private void invalidate ()
    {
        log.info ("Gauss UI Provider stopped");
    }

    @Override
    public ManagedObject newObject (String clazz, ManagedObjectInstance instance)
    {
        instance.putObject (MenuManager.class, menuManager);
        instance.putObject (NavigatorManager.class, navigatorManager);
        instance.putObject (RendererFactory.class, rendererFactory);
        return (new GaussUI ());
    }
}

// EOF
