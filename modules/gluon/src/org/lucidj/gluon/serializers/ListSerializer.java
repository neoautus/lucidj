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

package org.lucidj.gluon.serializers;

import org.lucidj.api.Serializer;
import org.lucidj.api.SerializerInstance;
import org.lucidj.gluon.GluonConstants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ListSerializer implements Serializer
{
    @Override
    public Map<String, Object> serializeObject (SerializerInstance engine, Object object)
    {
        Map<String, Object> data = new HashMap<> ();
        List list_object = (List)object;
        List<Map<String, Object>> list_serialized = new ArrayList<> ();

        for (Object item: list_object)
        {
            list_serialized.add (engine.serializeObject (item));
        }

        data.put (GluonConstants.CONTENTS, list_serialized);
        return (data);
    }

    @Override
    public Object deserializeObject (SerializerInstance engine, Map<String, Object> properties)
    {
        return null;
    }
}

// EOF
