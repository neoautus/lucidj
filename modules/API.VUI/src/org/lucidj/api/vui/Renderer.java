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

import org.lucidj.api.core.EventHelper;

import com.vaadin.ui.Component;

public interface Renderer
{
    void objectLinked (Object obj);
    void objectUnlinked ();
    Component renderingComponent ();
    void objectUpdated ();

    interface Observable
    {
        void addObserver (EventHelper.Subscriber observer);
        void deleteObserver (EventHelper.Subscriber observer);
    }
}

// EOF
