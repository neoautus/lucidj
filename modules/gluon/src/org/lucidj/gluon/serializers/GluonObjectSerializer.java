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

import java.text.CharacterIterator;
import java.text.StringCharacterIterator;

public class GluonObjectSerializer implements GluonPrimitive
{
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
        GluonObject go = new GluonObject ();
        go.setValue (instance.getValue ());
        return (go);
    }

    @Override
    public boolean match (String charseq)
    {
        CharacterIterator iter = new StringCharacterIterator (charseq);
        boolean at_least_one_dot = false;

        // Matches FQCN or FQCN@ID
        for (;;)
        {
            char c = iter.next ();

            // Starting char from identifier
            if (c == CharacterIterator.DONE
                || (!Character.isJavaIdentifierStart (c) && !Character.isIdentifierIgnorable (c)))
            {
                return (false);
            }

            // Remaining chars from identifier
            do
            {
                if ((c = iter.next ()) == CharacterIterator.DONE)
                {
                    return (at_least_one_dot);
                }
            }
            while (Character.isJavaIdentifierPart (c) || Character.isIdentifierIgnorable (c));

            if (c == '.')
            {
                // Optional '.', fetch next valid identifier
                at_least_one_dot = true;
            }
            else if (c == '@')
            {
                // Optional ID, continue below
                break;
            }
            else
            {
                // Non optional WTF :)
                return (false);
            }
        }

        char c = iter.next ();

        // Starting digit from ID
        if (c == CharacterIterator.DONE)
        {
            return (false);
        }

        do
        {
            // Remaining digits from ID
            if (!Character.isDigit (c))
            {
                return (false);
            }
        }
        while ((c = iter.next ()) != CharacterIterator.DONE);
        return (true);
    }
}

// EOF
