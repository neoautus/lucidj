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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

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
public class IconHelper implements org.lucidj.api.vui.IconHelper
{
    private final static Logger log = LoggerFactory.getLogger (IconHelper.class);

    @Requires
    private org.lucidj.api.core.ThemeHelper themeHelper;

    @Context
    private BundleContext bundleContext;

    private Bundle bundle;
    private String icon_theme_name;
    private URL default_icon_url;
    private Map<String, String> mime_extensions = new HashMap<> ();
    private Map<String, Resource> resource_icon_cache = new HashMap<> ();

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
        String family, name, icon_path;

        if (familyAndName.contains ("/"))
        {
            // First, test for a pure mime-type
            icon_path = "public/" + theme + "/mimetypes/32/" + familyAndName.replace ("/", "-") + ".svg";

            if (bundle.getEntry (icon_path) != null)
            {
                return (bundle.getResource (icon_path));
            }

            // Not a pure mime-type, look into the family/name icon archives
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
        icon_path = "public/" + theme + "/" + family + "/32/" + name + ".svg";

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

    private Resource get_caching_resource (URL icon_url)
    {
        String icon_url_location = icon_url.toString ();

        if (resource_icon_cache.containsKey (icon_url_location))
        {
            return (resource_icon_cache.get (icon_url_location));
        }
        Resource icon_resource = new BundleResource (icon_url);
        resource_icon_cache.put (icon_url_location, icon_resource);
        return (icon_resource);
    }

    @Override
    public Resource getIcon (URL url)
    {
        return (get_caching_resource (url));
    }

    @Override
    public Resource getIcon (String theme, String familyAndName, int size, int scale)
    {
        return (get_caching_resource (getIconURL (theme, familyAndName, size, scale)));
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

    public String get_icon_descriptor (String filenameOrExtension, boolean is_directory)
    {
        if (filenameOrExtension != null)
        {
            // Directory extensions are prefixed on mime.types with '/'
            String extension = (is_directory? "/": "")
                + filenameOrExtension.substring (filenameOrExtension.lastIndexOf (".") + 1);

            if (mime_extensions.containsKey (extension))
            {
                return (mime_extensions.get (extension));
            }
        }
        return (is_directory? "places/folder": "mimetypes/unknown");
    }

    @Override
    public String getMimeIconDescriptor (String filenameOrExtension)
    {
        return (get_icon_descriptor (filenameOrExtension, false));
    }

    @Override
    public String getMimeIconDescriptor (File file)
    {
        if (file == null)
        {
            return ("mimetypes/unknown");
        }
        return (get_icon_descriptor (file.getName (), file.isDirectory ()));
    }

    private void init_mime_types ()
    {
        String mime_types = System.getProperty ("system.conf") + "/mime.types";

        try (BufferedReader br = new BufferedReader(new FileReader (mime_types)))
        {
            for (String line; (line = br.readLine()) != null;)
            {
                if (line.trim ().startsWith ("#") || line.trim ().isEmpty ())
                {
                    continue;
                }

                // Split a line like:
                //     image/jpeg     jpeg jpg jpe
                String[] tokens = line.split ("\\s+");

                if (tokens.length < 2)
                {
                    continue;
                }

                String family_and_name = "mimetypes/" + tokens [0].replace ("/", "-");

                for (int i = 1; i < tokens.length; i++)
                {
                    if (mime_extensions.containsKey (tokens [i]))
                    {
                        log.warn ("Overwriting mime mapping for '{}' from {} to {}",
                            tokens [i], mime_extensions.get (tokens [i]), family_and_name);
                    }
                    mime_extensions.put (tokens [i], family_and_name);
                }
            }
        }
        catch (IOException e)
        {
            log.warn ("Unable to configure mime-types: {}", e.getMessage ());
        }
    }

    @Validate
    private void validate ()
    {
        bundle = bundleContext.getBundle ();
        icon_theme_name = themeHelper.getIconThemeName ();
        default_icon_url = bundle.getResource ("public/default-icon.svg");
        init_mime_types ();
    }
}

// EOF
