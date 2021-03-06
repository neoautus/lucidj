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

import java.util.Arrays;
import java.util.List;

public class Query
{
    private Archive root;

    public Query ()
    {
        root = new Archive ("/srv/lab/var");
    }

    public List<String> show ()
    {
        return (root.listTimeSeries());
    }

    public TimeSeries load (String tsname)
    {
        TimeSeries ts = new TimeSeries ();

        int num_recs = ts.load ("/srv/lab/var/" + tsname);

        if (num_recs == -1)
        {
            System.err.println ("Erro lendo arquivo: " + tsname);
            return (null);
        }

        return (ts);
    }

    public TimeSeries create (long[] timestamps, double[] values)
    {
        return (new TimeSeries (timestamps, values));
    }

    public String status ()
    {
        return ("The quick brown fox jumped over the lazy dogs.");
    }
}
