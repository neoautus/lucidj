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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.server.ClientConnector;
import com.vaadin.server.UIClassSelectionEvent;
import com.vaadin.server.UICreateEvent;
import com.vaadin.server.UIProvider;
import com.vaadin.ui.UI;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.ConfigurationException;
import org.apache.felix.ipojo.Factory;
import org.apache.felix.ipojo.InstanceManager;
import org.apache.felix.ipojo.MissingHandlerException;
import org.apache.felix.ipojo.Pojo;
import org.apache.felix.ipojo.UnacceptableConfiguration;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

@Component
@Instantiate
@Provides (specifications = UIProvider.class)
public class BaseVaadinUIProvider extends UIProvider
{
    private final static transient Logger log = LoggerFactory.getLogger (BaseVaadinUIProvider.class);

    // TODO: Test default-implementation=MyLogService.class, policy=BindingPolicy.DYNAMIC_PRIORITY
    @Requires (optional = true, specification = com.vaadin.ui.UI.class)
    private UI base_ui;

    @Override
    public Class<? extends UI> getUIClass (UIClassSelectionEvent uiClassSelectionEvent)
    {
        log.debug ("getUIClass: base_ui = {}", base_ui);
        return ((base_ui == null)? EmptyUI.class: base_ui.getClass());
    }

    @Override
    public UI createInstance (UICreateEvent event)
    {
        log.debug ("base_ui = {}", base_ui);

        if (base_ui != null)
        {
            Factory factory = ((Pojo)base_ui).getComponentInstance ().getFactory();

            try
            {
                final ComponentInstance new_comp = factory.createComponentInstance (null);
                final UI new_ui = (UI)((InstanceManager)new_comp).getPojoObject();

                log.debug ("new_comp={} new_ui={}", new_comp, new_ui);

                new_ui.addDetachListener (new ClientConnector.DetachListener()
                {
                    public void detach(ClientConnector.DetachEvent event)
                    {
                        log.debug ("######### Detached {}  ##########", new_comp);
                        new_ui.close ();
                        new_comp.dispose();
                    }
                });

                return (new_ui);
            }
            catch (UnacceptableConfiguration | MissingHandlerException | ConfigurationException e)
            {
                log.error ("Exception creating Vaadin UI", e);
            }
        }

        log.info ("Notice: No Vaadin UI available");
        return (new EmptyUI ());
    }
}

// EOF
