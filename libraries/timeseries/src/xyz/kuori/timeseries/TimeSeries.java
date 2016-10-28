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
import java.io.FileInputStream;

import com.google.common.io.LittleEndianDataInputStream;

public class TimeSeries
{
    private long[] timestamps = null;
    private double[] values = null;
    private int capacity = 0;
    private long min_timestamp;
    private long max_timestamp;
    private double min_value;
    private double max_value;
    private boolean valid_minmax = false;

    private void init (int size)
    {
        this.capacity = size;

        if (size == 0)
        {
            timestamps = null;
            values = null;
        }
        else
        {
            timestamps = new long [size];
            values = new double [size];
        }
    }

    public TimeSeries (int size)
    {
        init (size);
    }

    public TimeSeries ()
    {
        init (0);
    }

    public TimeSeries (long[] timestamps, double[] values)
    {
        this.timestamps = timestamps;
        this.values = values;
        this.capacity = timestamps.length;
    }

    private void validate_minmax ()
    {
        if (!valid_minmax)
        {
            min_timestamp = Long.MAX_VALUE;
            max_timestamp = Long.MIN_VALUE;
            min_value = Double.MAX_VALUE;
            max_value = Double.MIN_VALUE;

            for (int i = 0; i < capacity; i++)
            {
                if (timestamps[i] > max_timestamp)
                {
                    max_timestamp = timestamps[i];
                }

                if (timestamps[i] < min_timestamp)
                {
                    min_timestamp = timestamps[i];
                }

                if (values[i] > max_value)
                {
                    max_value = values[i];
                }

                if (values[i] < min_value)
                {
                    min_value = values[i];
                }
            }

            valid_minmax = true;
        }
    }

    public long getMinTimeStamp ()
    {
        validate_minmax ();
        return (min_timestamp);
    }

    public long getMaxTimeStamp ()
    {
        validate_minmax ();
        return (max_timestamp);
    }

    public double getMinValue ()
    {
        validate_minmax ();
        return (min_value);
    }

    public double getMaxValue ()
    {
        validate_minmax ();
        return (max_value);
    }

    public long[] getTimeStamps ()
    {
        return (timestamps);
    }

    public double[] getValues ()
    {
        return (values);
    }

    public int size ()
    {
        return (this.capacity);
    }

    public double v (int step)
    {
        return (values[step]);
    }

    public long t (int step)
    {
        return (timestamps [step]);
    }

    public int load (File archive)
    {
        FileInputStream fin = null;

        try
        {
            fin = new FileInputStream (archive);
        }
        catch (Exception e)
        {
            init (0);
            return (-1);
        }

        int record_count = (int)(archive.length() / 16);
        init(record_count);

        LittleEndianDataInputStream in = new LittleEndianDataInputStream (fin);

        try
        {
            for (int i = 0; i < record_count; i++)
            {
                timestamps[i] = in.readLong();
                values[i] = in.readDouble();
            }
        }
        catch (Exception e)
        {
            init (0);
            return (-1);
        }

        return (record_count);
    }

    public int ffirst (long tstamp)
    {
        for (int i = 0; i < capacity; i++)
        {
            if (timestamps [i] >= tstamp)
            {
                return (i);
            }
        }

        return (-1);
    }

    public int flast (long tstamp)
    {
        for (int i = capacity - 1; i >= 0; i--)
        {
            if (timestamps [i] <= tstamp)
            {
                return (i);
            }
        }

        return (-1);
    }

    public TimeSeries cut (long ts_start, long ts_end)
    {
        TimeSeries new_ts = new TimeSeries ();

        int dp_start = ffirst (ts_start);

        if (dp_start == -1)
        {
            return (new TimeSeries ());
        }

        int dp_end = flast (ts_end);

        if (dp_end == -1)
        {
            return (new TimeSeries ());
        }

        int num_points = dp_end - dp_start;

        if (num_points < 1)
        {
            // Throw exception?
            return (null);
        }

        new_ts.init (num_points);

        System.arraycopy (this.timestamps, dp_start, new_ts.timestamps, 0, num_points);
        System.arraycopy (this.values, dp_start, new_ts.values, 0, num_points);

        return (new_ts);
    }

    public TimeSeries dup ()
    {
        TimeSeries new_ts = new TimeSeries (this.capacity);

        System.arraycopy (this.timestamps, 0, new_ts.timestamps, 0, this.capacity);
        System.arraycopy (this.values, 0, new_ts.values, 0, this.capacity);

        return (new_ts);
    }

    public TimeSeries rewind (long new_start)
    {
        TimeSeries new_ts = dup ();

        if (new_ts.capacity < 1)
        {
            return (new_ts);
        }

        long ts[] = new_ts.getTimeStamps ();
        long delta = new_start - ts[0];

        for (int i = 0; i < new_ts.capacity; i++)
        {
            ts [i] += delta;
        }

        return (new_ts);
    }

    public int load (String filename)
    {
        return (load (new File (filename)));
    }

    public String toString ()
    {
        StringBuilder s = new StringBuilder ();

        for (int i = 0; i < capacity; i++)
        {
            s.append ("[");
            s.append (i);
            s.append ("] ");
            s.append (timestamps [i]);
            s.append (" => ");
            s.append (values [i]);
            s.append ("\n");
        }

        return (s.toString ());
    }
}
