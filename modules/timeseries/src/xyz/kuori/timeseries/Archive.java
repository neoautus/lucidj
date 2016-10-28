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

package xyz.kuori.timeseries;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Archive
{
    private String storage_path;

    public Archive (String storage_path)
    {
        this.storage_path = storage_path;
    }

    public List<String> listTimeSeries ()
    {
        List<String> listing = new ArrayList<String>();

        File[] filesList = new File (storage_path).listFiles ();

        for (File f : filesList)
        {
            if(f.isDirectory())
            {
                // Nada por enquanto
            }
            else if(f.isFile())
            {
                listing.add (f.getName());
            }
        }

        return (listing);
    }

    public static String info ()
    {
        return ("TimeSeries Online");
    }
}
