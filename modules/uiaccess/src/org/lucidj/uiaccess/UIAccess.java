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

package org.lucidj.uiaccess;

import com.vaadin.ui.Component;
import com.vaadin.ui.UI;

public abstract class UIAccess
{
    private Component base_access_component;

    // TODO: ALLOW USAGE AS TimerTask
    public UIAccess(Component access)
    {
        base_access_component = access;
        spawn_runnable ();
    }

    abstract public void updateUI ();

    private void spawn_runnable ()
    {
        // As Anna suggests at https://vaadin.com/pro/support#1123
        final UI ui = base_access_component.getUI();

        if (ui == null || !ui.isAttached ())
        {
            updateUI();
        }
        else // Valid, attached UI
        {
            ui.access (new Runnable()
            {
                @Override
                public void run()
                {
                    updateUI();

                    // TODO: CHECK IF IT'S BETTER JUST TOUCH parent
                    base_access_component.markAsDirtyRecursive();
                }
            });
        }
    }
}

// EOF
