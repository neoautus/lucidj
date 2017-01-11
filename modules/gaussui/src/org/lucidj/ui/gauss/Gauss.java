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

import org.lucidj.api.ManagedObject;
import org.lucidj.api.ManagedObjectProvider;
import org.lucidj.api.MenuManager;
import org.lucidj.api.NavigatorManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import org.osgi.framework.BundleContext;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Context;
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

    private static final String[] provided_classes = new String[]
    {
        GaussUI.class.getCanonicalName ()
    };

    @Context
    private BundleContext context;

    @Requires
    private MenuManager menu_manager;

    @Requires
    private NavigatorManager nav_manager;

    @Validate
    private boolean validate ()
    {
        log.info ("Gauss UI Provider started");
        return (true);
    }

    @Invalidate
    private void invalidate ()
    {
        log.info ("Gauss UI Provider stopped");
    }

    @Override
    public String[] getProvidedClasses ()
    {
        return (provided_classes);
    }

    @Override
    public ManagedObject newInstance (String clazz, Map<String, Object> properties)
    {
        ManagedObject new_ui = new GaussUI (properties);

        new_ui.putObject (BundleContext.class, context);
        new_ui.putObject (MenuManager.class, menu_manager);
        new_ui.putObject (NavigatorManager.class, nav_manager);
        return (new_ui);
    }
}

// EOF
