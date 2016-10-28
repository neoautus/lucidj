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

import java.util.logging.Logger;

import com.vaadin.server.ClientConnector;
import com.vaadin.server.UIClassSelectionEvent;
import com.vaadin.server.UICreateEvent;
import com.vaadin.server.UIProvider;
import com.vaadin.ui.UI;
import org.apache.felix.ipojo.*;
import org.apache.felix.ipojo.annotations.*;

@Component
@Instantiate
@Provides(specifications = UIProvider.class)
public class BaseVaadinUIProvider extends UIProvider
{
    private static final Logger log = Logger.getLogger ("UIProvider");

    @Requires(optional = true, specification = com.vaadin.ui.UI.class)
    private UI base_ui;

    @Override
    public Class<? extends UI> getUIClass (UIClassSelectionEvent uiClassSelectionEvent)
    {
        log.info ("getUIClass");

        return ((base_ui == null)? EmptyUI.class: base_ui.getClass());
    }

    @Override
    public UI createInstance (UICreateEvent event)
    {
        log.info("base_ui = " + base_ui);

        if (base_ui != null)
        {
            Factory factory = ((Pojo)base_ui).getComponentInstance ().getFactory();

            try
            {
                final ComponentInstance new_comp = factory.createComponentInstance (null);

                log.info("new_comp = " + new_comp.toString ());

                final UI new_ui = (UI)((InstanceManager)new_comp).getPojoObject();

                log.info("new_ui = " + new_ui.toString ());

                new_ui.addDetachListener (new ClientConnector.DetachListener()
                {
                    public void detach(ClientConnector.DetachEvent event)
                    {
                        log.info ("######### Detached " + new_comp + " ##########");
                        new_ui.close ();
                        new_comp.dispose();
                    }
                });

                return (new_ui);
            }
            catch (UnacceptableConfiguration | MissingHandlerException | ConfigurationException e)
            {
                log.info ("createInstance: Exception " + e.toString ());
            }
        }

        log.info ("EmptyUI()");
        return (new EmptyUI ());
    }
}

// EOF
