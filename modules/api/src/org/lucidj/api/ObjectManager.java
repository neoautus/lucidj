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

import java.util.Set;

public interface ObjectManager
{
    void showObject (Object obj);
    void showObject (int index, Object obj);
    void setObjectTag (Object obj, String tag);
    void removeObject (Object obj);
    void clearObjects ();
    void restrain ();
    void release ();
    int available ();
    Object read () throws InterruptedException;
    Object readAll () throws InterruptedException;
    Object getObject (int index);
    Object getObject (String tag);
    Object[] getObjects ();
    void markAsDirty (Object obj);
    void markAsClean (Object obj);
    Set<Object> getDirtyObjects ();
    void setObjectEventListener (ObjectEventListener listener);

    public interface ObjectEventListener
    {
        void restrain ();
        void release ();
        Object addingObject (Object obj, int index);
        void changingObject (Object obj);
        void removingObject (Object obj, int index);
    }
}

// EOF
