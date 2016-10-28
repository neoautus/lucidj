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

package xyz.kuori.buttons_and_links;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.server.ExternalResource;
import com.vaadin.server.FontAwesome;
import com.vaadin.server.Resource;
import com.vaadin.ui.*;
import org.apache.felix.ipojo.annotations.*;
import org.apache.felix.ipojo.annotations.Component;

@Component
@Instantiate
@Provides (specifications = com.vaadin.navigator.View.class)
public class ButtonsAndLinks extends VerticalLayout implements View
{
    final Logger log = LoggerFactory.getLogger (ButtonsAndLinks.class);

    @Property public String title = "Buttons & Links";
    @Property public int weight = 250;
    @Property public Resource icon = FontAwesome.PAPER_PLANE;
    @Property public String badge = "123";

    public ButtonsAndLinks()
    {
        // Delay UI building
        log.info("Slf4j is working: {}", title);
    }

    private void buildView()
    {
        setMargin(true);

        Label h1 = new Label("Buttons");
        h1.addStyleName("h1");
        addComponent(h1);

        HorizontalLayout row = new HorizontalLayout();
        row.addStyleName("wrapping");
        row.setSpacing(true);
        addComponent(row);

        Button button = new Button("Normal");
        row.addComponent(button);

        button = new Button("Disabled");
        button.setEnabled(false);
        row.addComponent(button);

        button = new Button("Primary");
        button.addStyleName("primary");
        row.addComponent(button);

        button = new Button("Friendly");
        button.addStyleName("friendly");
        row.addComponent(button);

        button = new Button("Danger");
        button.addStyleName("danger");
        row.addComponent(button);

        button = new Button("Small");
        button.addStyleName("small");
        button.setIcon(FontAwesome.BITBUCKET);
        row.addComponent(button);

        button = new Button("Large");
        button.addStyleName("large");
        button.setIcon(FontAwesome.BITBUCKET);
        row.addComponent(button);

        button = new Button("Top");
        button.addStyleName("icon-align-top");
        button.setIcon(FontAwesome.BITBUCKET);
        row.addComponent(button);

        button = new Button("Image icon");
        button.setIcon(FontAwesome.BITBUCKET);
        row.addComponent(button);

        button = new Button("Image icon");
        button.addStyleName("icon-align-right");
        button.setIcon(FontAwesome.BITBUCKET);
        row.addComponent(button);

        button = new Button("Photos");
        button.setIcon(FontAwesome.BITBUCKET);
        row.addComponent(button);

        button = new Button();
        button.setIcon(FontAwesome.BITBUCKET);
        button.addStyleName("icon-only");
        row.addComponent(button);

        button = new Button("Borderless");
        button.setIcon(FontAwesome.BITBUCKET);
        button.addStyleName("borderless");
        row.addComponent(button);

        button = new Button("Borderless, colored");
        button.setIcon(FontAwesome.BITBUCKET);
        button.addStyleName("borderless-colored");
        row.addComponent(button);

        button = new Button("Quiet");
        button.setIcon(FontAwesome.BITBUCKET);
        button.addStyleName("quiet");
        row.addComponent(button);

        button = new Button("Link style");
        button.setIcon(FontAwesome.BITBUCKET);
        button.addStyleName("link");
        row.addComponent(button);

        button = new Button("Icon on right");
        button.setIcon(FontAwesome.BITBUCKET);
        button.addStyleName("icon-align-right");
        row.addComponent(button);

        CssLayout group = new CssLayout();
        group.addStyleName("v-component-group");
        row.addComponent(group);

        button = new Button("One");
        group.addComponent(button);
        button = new Button("Two");
        group.addComponent(button);
        button = new Button("Three");
        group.addComponent(button);

        button = new Button("Tiny");
        button.addStyleName("tiny");
        row.addComponent(button);

        button = new Button("Huge");
        button.addStyleName("huge");
        row.addComponent(button);

        NativeButton nbutton = new NativeButton("Native");
        row.addComponent(nbutton);

        h1 = new Label("Links");
        h1.addStyleName("h1");
        addComponent(h1);

        row = new HorizontalLayout();
        row.addStyleName("wrapping");
        row.setSpacing(true);
        addComponent(row);

        Link link = new Link("vaadin.com", new ExternalResource(
                "https://vaadin.com"));
        row.addComponent(link);

        link = new Link("Link with icon", new ExternalResource(
                "https://vaadin.com"));
        link.addStyleName("color3");
        link.setIcon(FontAwesome.BITBUCKET);
        row.addComponent(link);

        link = new Link("Small", new ExternalResource("https://vaadin.com"));
        link.addStyleName("small");
        row.addComponent(link);

        link = new Link("Large", new ExternalResource("https://vaadin.com"));
        link.addStyleName("large");
        row.addComponent(link);

        link = new Link(null, new ExternalResource("https://vaadin.com"));
        link.setIcon(FontAwesome.BITBUCKET);
        link.addStyleName("large");
        row.addComponent(link);
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

}

// EOF
