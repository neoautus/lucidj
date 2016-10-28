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

public class Scatter extends Trace
{
    public Scatter ()
    {
        json.set ("type", "scatter");
    }

    public Scatter x (Double... points)
    {
        json.set ("x", points);
        return (this);
    }

    public Scatter x (double... points)
    {
        Double[] point_array = new Double [points.length];

        for (int i = 0; i < points.length; i++)
        {
            point_array [i] = points [i];
        }

        return (x (point_array));
    }


    public Scatter x (Integer... points)
    {
        json.set ("x", points);
        return (this);
    }

    public Scatter x (int... points)
    {
        Integer[] point_array = new Integer [points.length];

        for (int i = 0; i < points.length; i++)
        {
            point_array [i] = points [i];
        }

        return (x (point_array));
    }

    public Scatter y (Double... points)
    {
        json.set ("y", points);
        return (this);
    }

    public Scatter y (double... points)
    {
        Double[] point_array = new Double [points.length];

        for (int i = 0; i < points.length; i++)
        {
            point_array [i] = points [i];
        }

        return (y (point_array));
    }

    public Scatter y (Long... points)
    {
        json.set ("y", points);
        return (this);
    }

    public Scatter y (long... points)
    {
        Long[] point_array = new Long [points.length];

        for (int i = 0; i < points.length; i++)
        {
            point_array [i] = points [i];
        }

        return (y (point_array));
    }

    public Scatter y (Integer... points)
    {
        json.set ("y", points);
        return (this);
    }

    public Scatter y (int... points)
    {
        Integer[] point_array = new Integer [points.length];

        for (int i = 0; i < points.length; i++)
        {
            point_array [i] = points [i];
        }

        return (y (point_array));
    }

    public Scatter mode (String mode)
    {
        json.set ("mode", mode);
        return (this);
    }

    public Scatter line (String color, Integer width, String dash, Integer opacity, String shape,
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

    public Scatter line (String color, Integer width, String dash, Integer opacity, String shape,
                         Integer smoothing, String outliercolor)
    {
        return (line (color, width, dash, opacity, shape, smoothing, outliercolor, null));
    }

    public Scatter line (String color, Integer width, String dash, Integer opacity, String shape,
                         Integer smoothing)
    {
        return (line (color, width, dash, opacity, shape, smoothing, null, null));
    }

    public Scatter line (String color, Integer width, String dash, Integer opacity)
    {
        return (line (color, width, dash, opacity, null, null, null, null));
    }

    public Scatter line (String color, Integer width, String dash)
    {
        return (line (color, width, dash, null, null, null, null, null));
    }

    public Scatter line (String color, Integer width)
    {
        return (line (color, width, null, null, null, null, null, null));
    }

    public Scatter line (String color)
    {
        return (line (color, null, null, null, null, null, null, null));
    }

    public Scatter marker (String color, Integer size, Integer maxdisplayed, String symbol)
    {
        Map submap = json.getJsonSubmap ("marker");

        if (color != null)
        {
            submap.put ("color", color);
        }

        if (size != null)
        {
            submap.put ("size", size);
        }

        if (maxdisplayed != null)
        {
            submap.put ("maxdisplayed", maxdisplayed);
        }

        if (symbol != null)
        {
            submap.put ("symbol", symbol);
        }

        json.setJsonChanged ();
        return (this);
    }

    public Scatter marker (String color, Integer size, Integer maxdisplayed)
    {
        return (marker (color, size, maxdisplayed, null));
    }

    public Scatter marker (String color, Integer size)
    {
        return (marker (color, size, null, null));
    }

    public Scatter marker (String color)
    {
        return (marker (color, null, null, null));
    }

    public Scatter marker (Marker custom)
    {
        json.set ("marker", custom.getLinkableMap ());
        return (this);
    }

    public Boolean showlegend;
    public Double opacity;
    public XAxis xaxis = new XAxis ();
    public YAxis yaxis = new YAxis ();
    public String xsrc;
    public String ysrc;
    public Marker marker = new Marker ();
    public String[] text;
    public Error_X error_x = new Error_X ();
    public Error_Y error_y = new Error_Y ();
    public Integer[] r;
    public Object[] t;
    public Line line = new Line ();
    public String fillcolor;
    public Boolean connectgaps;
    public String mode; // lines/markers/text/lines+markers/lines+text/markers+text/lines+markers+text
    public String textposition; // top left/etc
    public String fill; // none/tozeroy/tonexty/tozerox/tonextx
    public Font textfont = new Font ();
}

// EOF
