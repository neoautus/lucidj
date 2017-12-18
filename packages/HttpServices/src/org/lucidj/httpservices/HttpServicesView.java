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

package org.lucidj.httpservices;

import org.lucidj.api.core.ServiceObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

public class HttpServicesView extends VerticalLayout implements View
{
    private final static Logger log = LoggerFactory.getLogger (HttpServicesView.class);

    public HttpServicesView ()
    {
        // Delay UI building
        log.info ("Search is visible now");
    }

    private void buildView ()
    {
        setMargin (true);
        addComponent (new Label ("HttpServices"));
    }

    @Override
    public void enter (ViewChangeListener.ViewChangeEvent event)
    {
        // TODO Auto-generated method stub
        if (getComponentCount () == 0)
        {
            buildView ();
        }
    }

    @ServiceObject.Validate
    public void validate ()
    {
        // Nothing
        log.info ("validate()");
    }

    @ServiceObject.Invalidate
    public void invalidate ()
    {
        // Nothing
        log.info ("invalidate()");
    }
}

// EOF
