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

package xyz.kuori.vaadin;

import com.vaadin.server.UIClassSelectionEvent;
import com.vaadin.server.UICreateEvent;
import com.vaadin.server.UIProvider;
import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.Label;
import com.vaadin.ui.UI;

import java.util.logging.Logger;

public class EmptyUIProvider extends UIProvider
{
    private static final Logger log = Logger.getLogger ("EmptyUIProvider");

    @Override
    public Class<? extends UI> getUIClass (UIClassSelectionEvent uiClassSelectionEvent)
    {
        log.info ("getUIClass()");
        return EmptyUI.class;
    }

    @Override
    public UI createInstance (UICreateEvent event)
    {
        log.info ("createInstance()");
        return (new EmptyUI ());
    }
}

// EOF
