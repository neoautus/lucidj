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

package org.lucidj.eventhelper;

import org.lucidj.api.EventHelper;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

@Component (immediate = true, publicFactory = false)
@Instantiate
@Provides
public class DefaultEventHelperFactory implements EventHelper.Factory
{
    @Override
    public EventHelper newInstance ()
    {
        return (new DefaultEventHelper ());
    }

    class DefaultEventHelper implements EventHelper
    {
        private List<WeakReference<EventHelper.Subscriber>> subscribers = null;

        public void publish (Object event)
        {
            if (subscribers != null)
            {
                Subscriber subscriber;

                for (WeakReference<Subscriber> subscriber_ref: subscribers)
                {
                    if ((subscriber = subscriber_ref.get ()) != null)
                    {
                        subscriber.event (event);
                    }
                }
            }
        }

        public void subscribe (Subscriber handler)
        {
            if (subscribers == null)
            {
                subscribers = new ArrayList<> ();
            }
            subscribers.add (new WeakReference<> (handler));
        }

        @Override
        public void unsubscribe (Subscriber handler)
        {

        }
    }
}

// EOF
