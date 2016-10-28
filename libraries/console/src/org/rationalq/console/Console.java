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

package org.rationalq.console;

import org.lucidj.renderer.Renderer;
import org.lucidj.renderer.SimpleObservable;
import org.rationalq.quark.Quark;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Observer;

public class Console implements Quark, Renderer.Observable
{
    private final transient Logger log = LoggerFactory.getLogger (Console.class);

    private HashMap<String, Object> properties = new HashMap<>();
    private String content = "";
    private SimpleObservable observers = new SimpleObservable ();

    public void output (String tag, String text)
    {
        log.debug ("output: {}:{}", tag, text);
        content += "[" + tag + "] " + text;
        observers.notifyNow ();
    }

    public void clear ()
    {
        content = "";
        observers.notifyNow ();
    }

    public String getContent ()
    {
        return (content);
    }

    @Override // Renderer.Observable
    public void addObserver (Observer observer)
    {
        log.info ("addListener: observer={} content={}", observer, content);
        observers.addObserver (observer);
    }

    @Override // Renderer.Observable
    public void deleteObserver (Observer observer)
    {
        observers.deleteObserver (observer);
    }

    @Override // Quark
    public Map<String, Object> serializeObject ()
    {
        properties.put ("/", content);
        return (properties);
    }

    @Override // Quark
    public void deserializeObject (Map<String, Object> properties)
    {
        this.properties.putAll (properties);
        content = (String)properties.get ("/");

        if (content == null)
        {
            content = "";
        }
    }
}

// EOF
