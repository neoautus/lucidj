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

package org.lucidj.vaadinui;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.server.Resource;

import java.net.URL;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Context;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;

// This is a rough draft for the IconHelper. It should follow the baselines
// from: https://standards.freedesktop.org/icon-theme-spec/icon-theme-spec-latest.html
// With some work we'll be able to use icon packs from KDE/OpenDesktop :)
// A compact version of Breeze (https://github.com/KDE/breeze-icons) is included, since
// the original is around 32Mb.

@Component (immediate = true, publicFactory = false)
@Instantiate
@Provides
public class IconHelper implements org.lucidj.api.IconHelper
{
    private final static Logger log = LoggerFactory.getLogger (IconHelper.class);

    @Requires
    private org.lucidj.api.ThemeHelper themeHelper;

    @Context
    private BundleContext bundleContext;

    private Bundle bundle;
    private String icon_theme_name;
    private URL default_icon_url;

    @Validate
    private void validate ()
    {
        bundle = bundleContext.getBundle ();
        icon_theme_name = themeHelper.getIconThemeName ();
        default_icon_url = bundle.getResource ("public/default-icon.svg");
    }

    @Override
    public URL getIconURL (String theme, String familyAndName, int size, int scale)
    {
        // Sanity check returning default icon
        if (familyAndName == null
            || familyAndName.equals ("default")
            || familyAndName.equals ("default/default"))
        {
            return (default_icon_url);
        }

        // For now we ignore both size and scale, since breeze is a vectorial theme
        String family, name;

        if (familyAndName.contains ("/"))
        {
            int slash_pos = familyAndName.indexOf ("/");
            family = familyAndName.substring (0, slash_pos);
            name = familyAndName.substring (slash_pos + 1);
        }
        else
        {
            family = "app";
            name = familyAndName;
        }

        // Family is either actions/apps/categories/.../status OR the app name
        String icon_path = "public/" + theme + "/" + family + "/32/" + name + ".svg";

        // If not found, let's try other alternative path
        if (bundle.getEntry (icon_path) == null)
        {
            icon_path = "public/" + theme + "/" + family + "/22/" + name + ".svg";
        }

        if (bundle.getEntry (icon_path) == null)
        {
            return (default_icon_url);
        }
        return (bundle.getResource (icon_path));
    }

    @Override
    public URL getIconURL (String theme, String familyAndName, int size)
    {
        return (getIconURL (theme, familyAndName, size, 1));
    }

    @Override
    public URL getIconURL (String familyAndName, int size, int scale)
    {
        return (getIconURL (icon_theme_name, familyAndName, size, scale));
    }

    @Override
    public URL getIconURL (String familyAndName, int size)
    {
        return (getIconURL (icon_theme_name, familyAndName, size));
    }

    @Override
    public Resource getIcon (URL url)
    {
        return (new BundleResource (url));
    }

    @Override
    public Resource getIcon (String theme, String familyAndName, int size, int scale)
    {
        return (new BundleResource (getIconURL (theme, familyAndName, size, scale)));
    }

    @Override
    public Resource getIcon (String theme, String familyAndName, int size)
    {
        return (getIcon (theme, familyAndName, size, 1));
    }

    @Override
    public Resource getIcon (String familyAndName, int size, int scale)
    {
        return (getIcon (icon_theme_name, familyAndName, size, scale));
    }

    @Override
    public Resource getIcon (String familyAndName, int size)
    {
        return (getIcon (icon_theme_name, familyAndName, size));
    }
}

// EOF
