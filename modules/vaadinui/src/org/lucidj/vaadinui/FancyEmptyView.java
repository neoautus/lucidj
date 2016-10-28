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

package org.lucidj.vaadinui;

import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.Label;

public class FancyEmptyView extends CssLayout implements View
{
    // Fancy empty view, because white is just too empty :)
    public FancyEmptyView ()
    {
        super ();
        addStyleName ("custom-empty-view");
    }

    public FancyEmptyView (String message)
    {
        super ();
        addStyleName ("custom-empty-view");
        addComponent(new Label (message, ContentMode.PREFORMATTED));
    }

    @Override
    public void enter (ViewChangeListener.ViewChangeEvent event)
    {
        // Nothing to do
    }
}

// EOF
