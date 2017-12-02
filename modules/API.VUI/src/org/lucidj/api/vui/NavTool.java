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

package org.lucidj.api.vui;

import com.vaadin.data.Container;
import com.vaadin.server.Resource;

import java.net.URI;

public interface NavTool
{
    String PROPERTY_NAME = "Name";
    String PROPERTY_SIZE = "Size";
    String PROPERTY_ICON = "Icon";
    String PROPERTY_LASTMODIFIED = "Last Modified";
    String PROPERTY_URI = URI.class.getName ();

    int HANDLE_AUTO_START_RANGE = 10000;

    int     publish (String section, String caption);
    int     getHandle (String section, String caption);
    Object  addItem (int handle, Object parentItemId, String name, Resource icon, URI uri, Object itemId);
    Object  addItem (int handle, Object parentItemId, String name, String icon, String uri, Object itemId);
    int     addItem (int handle, int parentItemId, String name, String icon, String uri, int itemId);
    int     addItem (int handle, int parentItemId, String name, String icon, String uri);
    Object  addItem (int handle, String name, String icon, String uri, Object itemId);
    int     addItem (int handle, String name, String icon, String uri);
    boolean containsId (int handle, Object itemId);
    boolean containsId (int handle, int itemId);
    void    setChildrenAllowed (int handle, Object itemId, boolean childrenAllowed);
    void    setChildrenAllowed (int handle, int itemId, boolean childrenAllowed);
    void    setParent (int handle, Object itemId, Object newParentId);
    void    setParent (int handle, int itemId, int newParentId);
    void    setExpandItem (int handle, Object itemId);
    void    setExpandItem (int handle, int itemId);

    Container hackGetContainer (int handle);
}

// EOF
