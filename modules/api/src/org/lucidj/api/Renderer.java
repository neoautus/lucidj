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

import com.vaadin.ui.Component;

import java.util.Observer;

public interface Renderer
{
    boolean compatibleObject (Object obj_to_check);
    void objectLinked (Object obj);
    void objectUnlinked ();
    Component renderingComponent ();
    void objectUpdated ();

    interface Observable
    {
        void addObserver (Observer observer);
        void deleteObserver (Observer observer);
    }
}

// EOF
