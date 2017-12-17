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

package org.lucidj.html;

import org.lucidj.api.core.ComponentInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

// THIS COMPONENT IS INTENDED TO BE AN SIMPLE HTML FORMATTING TOOL

@Component (immediate = true, publicFactory = false)
@Instantiate
@Provides
public class Html implements ComponentInterface
{
    private final static Logger log = LoggerFactory.getLogger (Html.class);

    private String descriptor_id;
    private HashMap<String, Object> properties = new HashMap<>();
    private String content = "";

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

    @Override // ComponentInterface
    public Object fireEvent (Object source, Object event)
    {
        if (event instanceof String && event.equals ("run"))
        {
            return (content);
        }

        return (null);
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

//    @Override // Quark
//    public Map<String, Object> serializeObject ()
//    {
//        properties.put ("/", content);
//        return (properties);
//    }
//
//    @Override // Quark
//    public void deserializeObject (Map<String, Object> properties)
//    {
//        this.properties.putAll (properties);
//
//        if ((content = (String)properties.get ("/")) == null)
//        {
//            content = "";
//        }
//    }
}

// EOF
