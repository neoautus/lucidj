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

package org.lucidj.search;

import org.commonmark.Extension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.html.HtmlRenderer;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.lucidj.uiaccess.UIAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import java.util.Arrays;
import java.util.List;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.handlers.event.Subscriber;

@Component
@Provides (specifications = com.vaadin.navigator.View.class)
public class SearchView extends VerticalLayout implements View
{
    private final static transient Logger log = LoggerFactory.getLogger (SearchView.class);
    private SearchView self = this;

    private Parser parser;
    private HtmlRenderer renderer;

    private String content = "";
    private String html = "";

    public SearchView ()
    {
        List<Extension> extensions = Arrays.asList (TablesExtension.create ());

        parser = Parser.builder ().extensions (extensions).build ();
        renderer = HtmlRenderer.builder ().extensions (extensions).build ();

        // Delay UI building
        log.info ("Search is visible now");
    }

    private void buildView()
    {
        setMargin(true);
        Node document = parser.parse ("## This is *Markdown*!");
        addComponent (new Label (renderer.render (document), ContentMode.HTML));
        addComponent (new Label ("No results to show."));
    }

    @Subscriber (name = "searchbox", topics = "search", dataKey = "args", dataType = "java.lang.String")
    private void receive (final String search_event)
    {
        new UIAccess (this)
        {
            @Override
            public void updateUI()
            {
                log.info ("SEARCH EVENT: {}", search_event);
                self.addComponent (new Label (search_event));
            }
        };
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
