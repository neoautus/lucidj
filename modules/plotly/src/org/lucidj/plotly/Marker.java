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

package org.lucidj.plotly;

import java.util.Map;

public class Marker
{
    private JsonMap json = new JsonMap ();

    public Map getLinkableMap ()
    {
        return (json.linkableMap ());
    }

    public Marker color (String color)
    {
        json.set ("color", color);
        return (this);
    }

    public Marker color (String[] color)
    {
        json.set ("color", color);
        return (this);
    }

    public Marker size (int size)
    {
        json.set ("size", size);
        return (this);
    }

    public Marker symbol (String symbol)
    {
        json.set ("symbol", symbol);
        return (this);
    }

    public Marker line (String color, Integer width, String dash, Integer opacity, String shape,
                        Integer smoothing, String outliercolor, String outlierwidth)
    {
        Map submap = json.getJsonSubmap ("line");

        if (color != null)
        {
            submap.put ("color", color);
        }

        if (width != null)
        {
            submap.put ("width", width);
        }

        if (dash != null)
        {
            submap.put ("dash", dash);
        }

        if (opacity != null)
        {
            submap.put ("opacity", opacity);
        }

        if (shape != null)
        {
            submap.put ("shape", shape);
        }

        if (smoothing != null)
        {
            submap.put ("smoothing", smoothing);
        }

        if (outliercolor != null)
        {
            submap.put ("outliercolor", outliercolor);
        }

        if (outlierwidth != null)
        {
            submap.put ("outlierwidth", outlierwidth);
        }

        json.setJsonChanged ();
        return (this);
    }

    public Marker line (String color, Integer width, String dash, Integer opacity, String shape,
                         Integer smoothing, String outliercolor)
    {
        return (line (color, width, dash, opacity, shape, smoothing, outliercolor, null));
    }

    public Marker line (String color, Integer width, String dash, Integer opacity, String shape,
                         Integer smoothing)
    {
        return (line (color, width, dash, opacity, shape, smoothing, null, null));
    }

    public Marker line (String color, Integer width, String dash, Integer opacity)
    {
        return (line (color, width, dash, opacity, null, null, null, null));
    }

    public Marker line (String color, Integer width, String dash)
    {
        return (line (color, width, dash, null, null, null, null, null));
    }

    public Marker line (String color, Integer width)
    {
        return (line (color, width, null, null, null, null, null, null));
    }

    public Marker line (String color)
    {
        return (line (color, null, null, null, null, null, null, null));
    }

    public Marker opacity (Double opacity)
    {
        json.set ("opacity", opacity);
        return (this);
    }

    public Marker sizeref (Double sizeref)
    {
        json.set ("sizeref", sizeref);
        return (this);
    }

    public Marker sizemode (String sizemode)
    {
        json.set ("sizemode", sizemode);
        return (this);
    }

    public Marker colorscale (String colorscale)
    {
        json.set ("colorscale", colorscale);
        return (this);
    }

    public Marker colorscale (String[] colorscale)
    {
        json.set ("colorscale", colorscale);
        return (this);
    }

    public Marker cauto (boolean flag)
    {
        json.set ("cauto", flag);
        return (this);
    }

    public Marker cauto ()
    {
        return (cauto (true));
    }

    public Marker cmin (int cmin)
    {
        json.set ("cmin", cmin);
        return (this);
    }

    public Marker cmax (int cmax)
    {
        json.set ("cmax", cmax);
        return (this);
    }

    public Marker outliercolor (String color)
    {
        json.set ("outliercolor", color);
        return (this);
    }

    public Marker maxdisplayed (int number)
    {
        json.set ("maxdisplayed", number);
        return (this);
    }
}

// EOF
