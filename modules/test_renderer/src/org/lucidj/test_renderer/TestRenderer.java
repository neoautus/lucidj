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

package org.lucidj.test_renderer;

import org.lucidj.api.Renderer;
import org.lucidj.test_component.TestComponent;

import com.vaadin.ui.AbstractComponent;
import com.vaadin.ui.Button;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

@Component (immediate = true)
@Instantiate
@Provides
public class TestRenderer implements Renderer
{
    private TestComponent source;
    private Button component;

    @Override
    public boolean compatibleObject (Object obj_to_check)
    {
        return (obj_to_check instanceof TestComponent);
    }

    @Override
    public void objectLinked (Object obj)
    {
        source = (TestComponent)obj;
        component = new Button (source.getValue ());
    }

    @Override
    public void objectUnlinked ()
    {
        source = null;
        component = null;
    }

    @Override
    public AbstractComponent renderingComponent ()
    {
        return (component);
    }

    @Override
    public void objectUpdated ()
    {
        component.setCaption (source.getValue ());
    }
}

// EOF
