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

package org.lucidj.quarkextender;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.Enumeration;

import org.osgi.framework.Bundle;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Validate;
import org.apache.felix.ipojo.extender.Extender;

@Component
@Instantiate
@Extender (extension = "X-Package", onArrival = "onArrival", onDeparture = "onDeparture")
public class QuarkExtender
{
    private final static transient Logger log = LoggerFactory.getLogger (QuarkExtender.class);

    @Validate
    private boolean validate ()
    {
        log.info ("QuarkExtender started");
        return (true);
    }

    @Invalidate
    private void invalidate ()
    {
        log.info ("QuarkExtender terminated");
    }

    void onArrival (Bundle bnd, String header)
    {
        log.info ("onArrival: bnd={} header={}", bnd, header);

        // List ALL bundle entries
        Enumeration<URL> e = bnd.findEntries("/", null, true);

        // Find a specific localization file
        while (e.hasMoreElements ())
        {
            URL entry = e.nextElement ();
            log.info ("{} ----> {}", bnd, entry.toString ());
        }
    }

    void onDeparture (Bundle bnd)
    {
        log.info ("onDeparture: bnd={}", bnd);
    }
}

// EOF
