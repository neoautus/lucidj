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

package org.lucidj.markdown;

import org.commonmark.Extension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.html.HtmlRenderer;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.rationalq.editor.ComponentInterface;
import org.rationalq.quark.Quark;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

@Component (immediate = true)
@Instantiate
@Provides
public class Markdown implements Quark, ComponentInterface
{
    private HashMap<String, Object> properties = new HashMap<>();

    private Parser parser;
    private HtmlRenderer renderer;

    private String content = "";
    private String html = "";

    public Markdown ()
    {
        List<Extension> extensions = Arrays.asList (TablesExtension.create ());

        parser = Parser.builder ().extensions (extensions).build ();
        renderer = HtmlRenderer.builder ().extensions (extensions).build ();
    }

    public String getHtml ()
    {
        return (html);
    }

    public String markdownToHtml ()
    {
        Node document = parser.parse (content);
        return (html = renderer.render (document));
    }

    @Override // ComponentInterface
    public void setProperty (String name, Object value)
    {
        properties.put (name, value);
    }

    @Override // ComponentInterface
    public Object getProperty (String name)
    {
        return (properties.get (name));
    }

    @Override // ComponentInterface
    public String getIconTitle ()
    {
        return ("Markdown");
    }

    @Override // ComponentInterface
    public Object fireEvent (Object source, Object event)
    {
        return (null);
    }

    @Override // ComponentInterface
    public void setValue (Object value)
    {
        content = (String)value;
    }

    @Override // ComponentInterface
    public Object getValue ()
    {
        return (content);
    }

    @Override // Quark
    public Map<String, Object> serializeObject ()
    {
        properties.put ("/", content);
        properties.put ("Html", html);
        return (properties);
    }

    @Override // Quark
    public void deserializeObject(Map<String, Object> properties)
    {
        this.properties.putAll (properties);

        if ((content = (String)properties.get ("/")) == null)
        {
            content = "";
        }

        if ((html = (String)properties.get ("Html")) == null)
        {
            html = "";
        }
    }
}

// EOF
