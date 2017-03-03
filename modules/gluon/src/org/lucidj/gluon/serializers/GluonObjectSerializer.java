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

import org.lucidj.api.SerializerInstance;
import org.lucidj.gluon.GluonObject;
import org.lucidj.gluon.GluonPrimitive;

import java.util.regex.Pattern;

public class GluonObjectSerializer implements GluonPrimitive
{
    Pattern INT_PATTERN = Pattern.compile ("^-?\\d{1,10}$");

    @Override
    public boolean serializeObject (SerializerInstance instance, Object object)
    {
        GluonObject go = (GluonObject)object;
        instance.setValue (go.getValue ());
        return (true);
    }

    @Override
    public Object deserializeObject (SerializerInstance instance)
    {
        GluonObject go = new GluonObject (null);
        go.setValue (instance.getValue ());
        return (go);
    }

    @Override
    public boolean match (String charseq)
    {
        return (charseq.contains ("@"));
    }
}

// EOF
