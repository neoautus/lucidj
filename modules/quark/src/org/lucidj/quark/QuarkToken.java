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

package org.lucidj.quark;

import java.util.HashMap;
import java.util.Map;

// TODO: GET RID OF THIS CLASS
public class QuarkToken
{
    Map<String, Object> properties = new HashMap<> ();
    private String name;

    public QuarkToken (String name)
    {
        this.name = name;
    }

    public String getName ()
    {
        return (name);
    }

    public void setProperties (Map<String, Object> properties)
    {
        this.properties.putAll (properties);
    }

    public Map<String, Object> getProperties ()
    {
        return (properties);
    }

    public void setProperty(String property, Object value)
    {
        properties.put(property, value);
    }

    public Object getProperty(String property)
    {
        return(properties.get(property));
    }
}

// EOF
