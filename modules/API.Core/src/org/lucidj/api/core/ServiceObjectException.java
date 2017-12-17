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

package org.lucidj.api.core;

public class ServiceObjectException extends Exception
{
    private final Class<?> serviceType;

    /**
     * Constructor.
     *
     * @param serviceType
     *            class of the service that was not found (cannot be null)
     */
    public ServiceObjectException (final Class<?> serviceType)
    {
        super ("Service of type [" + serviceType.getName() + "] not available");
        this.serviceType = serviceType;
    }

    /**
     * Getter.
     *
     * @return class of the service that was not found
     */
    public Class<?> getServiceType()
    {
        return serviceType;
    }
}

// EOF
