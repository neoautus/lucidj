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

package org.lucidj.accounts;

import org.lucidj.api.ManagedObject;
import org.lucidj.api.ManagedObjectInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.ui.Button;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.ui.VerticalLayout;

public class AccountsView extends VerticalLayout implements ManagedObject, View
{
    final Logger log = LoggerFactory.getLogger (AccountsView.class);

    private void buildView()
    {
        setMargin (true);

        Label h1 = new Label ("Accounts");
        h1.addStyleName ("h1");
        addComponent (h1);

        addComponent (local_accounts ());
    }

    private Panel local_accounts ()
    {
        Panel p = new Panel ("Local accounts");
        VerticalLayout content = new VerticalLayout ();
        p.setContent(content);
        content.setSpacing(true);
        content.setMargin(true);
        content.addComponent(new Label ("You can test the loading indicator by pressing the buttons."));
        content.addComponent (new Button ("Hello world!"));
        return (p);
    }

    @Override
    public void enter(ViewChangeListener.ViewChangeEvent event)
    {
        // TODO Auto-generated method stub
        if (getComponentCount() == 0)
        {
            buildView();
        }
    }

    @Override
    public void validate (ManagedObjectInstance instance)
    {
        // Nothing
    }

    @Override
    public void invalidate (ManagedObjectInstance instance)
    {
        // Nothing
    }
}

// EOF
