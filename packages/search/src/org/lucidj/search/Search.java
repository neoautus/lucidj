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

package org.lucidj.search;

import org.lucidj.api.MenuInstance;
import org.lucidj.api.MenuProvider;
import org.lucidj.runtime.Kernel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewProvider;
import com.vaadin.server.ConnectorResource;
import com.vaadin.server.DownloadStream;
import com.vaadin.server.Resource;
import com.vaadin.util.FileTypeResolver;

import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.util.Map;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Context;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

@Component
@Instantiate
@Provides (specifications = MenuProvider.class)
public class Search implements MenuProvider, ViewProvider
{
    private final static transient Logger log = LoggerFactory.getLogger (Search.class);
    private final static String V_SEARCH = "search";

    @Context
    private BundleContext context;

    @Override // MenuProvider
    public Map<String, Object> getProperties ()
    {
        return (null);
    }

    // From: https://github.com/lunifera/lunifera-vaadin-examples/(...)/bundleresource/BundleResource.java
    public class BundleResource implements ConnectorResource, Serializable
    {

        /**
         * Default buffer size for this stream resource.
         */
        private int bufferSize = 0;

        /**
         * Default cache time for this stream resource.
         */
        private long cacheTime = DownloadStream.DEFAULT_CACHETIME;

        /**
         * The bundle where the resource should be loaded from.
         */
        private Bundle bundle;

        /**
         * Name of the resource is relative to the associated class.
         */
        private final String resourceName;

        /**
         * Creates a new bundle resource instance.
         *
         * @param bundle
         *            the bundle of the which the resource should be loaded.
         * @param resourceName
         *            the Unique identifier of the resource within the application.
         */
        public BundleResource(Bundle bundle, String resourceName) {
            this.bundle = bundle;
            this.resourceName = resourceName;
            if (bundle == null || resourceName == null) {
                throw new NullPointerException();
            }
        }

        /**
         * Gets the MIME type of this resource.
         *
         * @see com.vaadin.server.Resource#getMIMEType()
         */
        @Override
        public String getMIMEType() {
            return FileTypeResolver.getMIMEType(resourceName);
        }

        @Override
        public String getFilename() {
            String[] parts = resourceName.split("/");
            return parts[parts.length - 1];
        }

        @Override
        public DownloadStream getStream() {
            URL entry = bundle.getEntry(resourceName);
            if (entry == null) {
                return null;
            }

            try {
                DownloadStream ds = new DownloadStream(entry.openStream(),
                        getMIMEType(), getFilename());
                ds.setBufferSize(getBufferSize());
                ds.setCacheTime(getCacheTime());
                return ds;
            } catch (IOException e) {
                throw new IllegalArgumentException(e);
            }
        }

        /**
         * Gets the size of the download buffer used for this resource.
         *
         * <p>
         * If the buffer size is 0, the buffer size is decided by the terminal
         * adapter. The default value is 0.
         * </p>
         *
         * @return the size of the buffer in bytes.
         */
        public int getBufferSize() {
            return bufferSize;
        }

        /**
         * Sets the size of the download buffer used for this resource.
         *
         * @param bufferSize
         *            the size of the buffer in bytes.
         *
         * @see #getBufferSize()
         */
        public void setBufferSize(int bufferSize) {
            this.bufferSize = bufferSize;
        }

        /**
         * Gets the length of cache expiration time.
         *
         * <p>
         * This gives the adapter the possibility cache streams sent to the client.
         * The caching may be made in adapter or at the client if the client
         * supports caching. Default is {@link DownloadStream#DEFAULT_CACHETIME}.
         * </p>
         *
         * @return Cache time in milliseconds
         */
        public long getCacheTime() {
            return cacheTime;
        }

        /**
         * Sets the length of cache expiration time.
         *
         * <p>
         * This gives the adapter the possibility cache streams sent to the client.
         * The caching may be made in adapter or at the client if the client
         * supports caching. Zero or negative value disables the caching of this
         * stream.
         * </p>
         *
         * @param cacheTime
         *            the cache time in milliseconds.
         *
         */
        public void setCacheTime(long cacheTime) {
            this.cacheTime = cacheTime;
        }
    }

    @Override // MenuProvider
    public void buildMenuEntries (MenuInstance menu, Map<String, Object> properties)
    {
        Resource icon = new BundleResource (context.getBundle (), "Resources/icons/zoom-seach-icon-16x16.png");
        menu.addMenuEntry (menu.newMenuEntry ("Search", icon, 250, V_SEARCH));
        menu.registry ().register (this);
    }

    @Override // ViewProvider
    public String getViewName (String s)
    {
        if (V_SEARCH.equals (s))
        {
            return (V_SEARCH);
        }
        return null;
    }

    @Override // ViewProvider
    public View getView (String s)
    {
        if (V_SEARCH.equals (s))
        {
            return (Kernel.newComponent (SearchView.class));
        }
        return null;
    }
}

// EOF
