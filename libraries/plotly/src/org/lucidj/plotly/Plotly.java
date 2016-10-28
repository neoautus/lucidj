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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.lucidj.renderer.Renderer;
import org.lucidj.renderer.SimpleObservable;
import org.rationalq.quark.Quark;

/* Plotly, what a neat library!
 * Be sure to visit https://plot.ly/ to know more.
 */

public class Plotly implements Quark, Renderer.Observable
{
    private transient Gson gson = new Gson ();
    private transient Gson pretty = new GsonBuilder ().setPrettyPrinting().create();
    private SimpleObservable observers = new SimpleObservable ();

    private HashMap properties = new HashMap ();
    private JsonMap layout = new JsonMap ();

    private List<Trace> trace_data = new ArrayList<> ();
    private List<Map> map_data = new ArrayList<> ();
    private String json_data;

    //=============
    // Layout data
    //=============

    public XAxis xaxis = new XAxis ();
    public YAxis yaxis = new YAxis ();
    public Legend legend = new Legend ();
    public List<Annotation> annotation = new ArrayList<Annotation> ();
    public Margin margin = new Margin ();
    public String paper_bgcolor;
    public String plot_bgcolor;
    public String hovermode; // closest/x/y
    public String dragmode; // zoom/pan/rotate
    public String separators;
    public String barmode;
    public Integer bargap;
    public Integer bargroupgap;
    public String barnorm;
    public String boxmode;
    public Integer boxgap;
    public Integer boxgapgroup;
    public RadialAxis radialaxis = new RadialAxis ();
    public AngularAxis angularaxis = new AngularAxis ();
    public Scene scene = new Scene ();
    public String direction;
    public Integer orientation;
    public Boolean hidesources;

    public Plotly ()
    {
    }

    public void update ()
    {
        observers.notifyNow ();
    }

    public Plotly title (String str)
    {
        layout.set ("title", str);
        return (this);
    }

    public Plotly titlefont (String family, int size, String color, String outlinecolor)
    {
        Map font = layout.getJsonSubmap ("titlefont");

        if (family != null)
        {
            font.put ("family", family);
        }

        if (size != -1)
        {
            font.put ("size", size);
        }

        if (color != null)
        {
            font.put ("color", color);
        }

        if (outlinecolor != null)
        {
            font.put ("outlinecolor", outlinecolor);
        }

        layout.setJsonChanged ();
        return (this);
    }

    public Plotly titlefont (String family, int size, String color)
    {
        titlefont (family, size, color, null);
        return (this);
    }

    public Plotly titlefont (String family, int size)
    {
        titlefont (family, size, null, null);
        return (this);
    }

    public Plotly titlefont (String family)
    {
        titlefont (family, -1, null, null);
        return (this);
    }

    public Plotly font (String family, int size, String color, String outlinecolor)
    {
        Map font = layout.getJsonSubmap ("titlefont");

        if (family != null)
        {
            font.put ("family", family);
        }

        if (size != -1)
        {
            font.put ("size", size);
        }

        if (color != null)
        {
            font.put ("color", color);
        }

        if (outlinecolor != null)
        {
            font.put ("outlinecolor", outlinecolor);
        }

        layout.setJsonChanged ();
        return (this);
    }

    public Plotly font (String family, int size, String color)
    {
        font (family, size, color, null);
        return (this);
    }

    public Plotly font (String family, int size)
    {
        font (family, size, null, null);
        return (this);
    }

    public Plotly font (String family)
    {
        font (family, -1, null, null);
        return (this);
    }

    public Plotly showlegend (boolean flag)
    {
        layout.set ("showlegend", flag);
        return (this);
    }

    public Plotly showlegend ()
    {
        return (showlegend (true));
    }

    public Plotly autosize (boolean flag)
    {
        layout.set ("autosize", flag);
        return (this);
    }

    public Plotly autosize ()
    {
        return (autosize (true));
    }

    public Plotly width (int pix_width)
    {
        layout.set ("width", pix_width);
        return (this);
    }

    public Plotly height (int pix_height)
    {
        layout.set ("height", pix_height);
        return (this);
    }

    public Plotly margin (Integer t, Integer r, Integer b, Integer l, Integer pad, Boolean autoexpand)
    {
        Map submap = layout.getJsonSubmap ("margin");

        if (t != null)
        {
            submap.put ("t", t);
        }

        if (r != null)
        {
            submap.put ("r", r);
        }

        if (b != null)
        {
            submap.put ("b", b);
        }

        if (l != null)
        {
            submap.put ("l", l);
        }

        if (pad != null)
        {
            submap.put ("pad", pad);
        }

        if (autoexpand != null)
        {
            submap.put ("autoexpand", autoexpand);
        }

        layout.setJsonChanged ();
        return (this);

    }

    public Plotly margin (Integer t, Integer r, Integer b, Integer l, Integer pad)
    {
        return (margin (t, r, b, l, pad, null));
    }

    public Plotly margin (Integer t, Integer r, Integer b, Integer l)
    {
        return (margin (t, r, b, l, null, null));
    }

    public Plotly margin (Integer t, Integer rl, Integer b)
    {
        return (margin (t, rl, b, rl, null, null));
    }

    public Plotly margin (Integer tb, Integer lr)
    {
        return (margin (tb, lr, tb, lr, null, null));
    }

    public Plotly margin (Integer margin)
    {
        return (margin (margin, margin, margin, margin, null, null));
    }

    public Plotly margin_top (int margin)
    {
        return (margin (null, null, null, margin, null, null));
    }

    public Plotly margin_bottom (int margin)
    {
        return (margin (null, null, margin, null, null, null));
    }

    public Plotly margin_left (int margin)
    {
        return (margin (margin, null, null, null, null, null));
    }

    public Plotly margin_right (int margin)
    {
        return (margin (null, margin, null, null, null, null));
    }

    public Plotly data (Trace... traces)
    {
        trace_data.clear ();
        map_data.clear ();
        json_data = null;

        for (Trace trace: traces)
        {
            trace_data.add (trace);
            map_data.add (trace.getLinkableMap ());
        }
        return (this);
    }

    public Plotly addTrace (Trace trace)
    {
        trace_data.add (trace);
        map_data.add (trace.getLinkableMap ());
        json_data = null;
        return (this);
    }

    public static Trace newTrace (String type)
    {
        Trace new_trace;

        switch (type.toLowerCase ())
        {
            case "area":                new_trace = new org.lucidj.plotly.Area (); break;
            case "box":                 new_trace = new org.lucidj.plotly.Box (); break;
            case "bar":                 new_trace = new org.lucidj.plotly.Bar (); break;
            case "contour":             new_trace = new org.lucidj.plotly.Contour (); break;
            case "heatmap":             new_trace = new org.lucidj.plotly.Heatmap (); break;
            case "histogram":           new_trace = new org.lucidj.plotly.Histogram (); break;
            case "histogram2d":         new_trace = new org.lucidj.plotly.Histogram2D (); break;
            case "histogram2dcontour":  new_trace = new org.lucidj.plotly.Histogram2DContour (); break;
            case "scatter3d":           new_trace = new org.lucidj.plotly.Scatter3D (); break;
            case "surface":             new_trace = new org.lucidj.plotly.Surface (); break;
            case "scatter":
            default:
                                        new_trace = new org.lucidj.plotly.Scatter (); break;
        }

        // Automatically add the new Trace
        return (new_trace);
    }

    public static Trace newTrace ()
    {
        return (newTrace ("")); // Return default Trace
    }

    public static Marker newMarker ()
    {
        return (new Marker ());
    }

    public static Plotly newPlot ()
    {
        return (new Plotly ());
    }

    public static Plotly newPlot (Trace t0, Trace t1, Trace t2, Trace t3, Trace t4,
                                  Trace t5, Trace t6, Trace t7, Trace t8, Trace t9)
    {
        Plotly plot = newPlot ();

        if (t0 != null)
        {
            plot.addTrace (t0);
        }

        if (t1 != null)
        {
            plot.addTrace (t1);
        }

        if (t2 != null)
        {
            plot.addTrace (t2);
        }

        if (t2 != null)
        {
            plot.addTrace (t2);
        }

        if (t3 != null)
        {
            plot.addTrace (t3);
        }

        if (t4 != null)
        {
            plot.addTrace (t4);
        }

        if (t5 != null)
        {
            plot.addTrace (t5);
        }

        if (t6 != null)
        {
            plot.addTrace (t6);
        }

        if (t7 != null)
        {
            plot.addTrace (t7);
        }

        if (t8 != null)
        {
            plot.addTrace (t8);
        }

        if (t9 != null)
        {
            plot.addTrace (t9);
        }

        return (plot);
    }

    public static Plotly newPlot (Trace t0, Trace t1, Trace t2, Trace t3, Trace t4,
                                  Trace t5, Trace t6, Trace t7, Trace t8)
    {
        return (newPlot (t0, t1, t2, t3, t4, t5, t6, t7, t8, null));
    }

    public static Plotly newPlot (Trace t0, Trace t1, Trace t2, Trace t3, Trace t4,
                                  Trace t5, Trace t6, Trace t7)
    {
        return (newPlot (t0, t1, t2, t3, t4, t5, t6, t7, null, null));
    }

    public static Plotly newPlot (Trace t0, Trace t1, Trace t2, Trace t3, Trace t4,
                                  Trace t5, Trace t6)
    {
        return (newPlot (t0, t1, t2, t3, t4, t5, t6, null, null, null));
    }

    public static Plotly newPlot (Trace t0, Trace t1, Trace t2, Trace t3, Trace t4,
                                  Trace t5)
    {
        return (newPlot (t0, t1, t2, t3, t4, t5, null, null, null, null));
    }

    public static Plotly newPlot (Trace t0, Trace t1, Trace t2, Trace t3, Trace t4)
    {
        return (newPlot (t0, t1, t2, t3, t4, null, null, null, null, null));
    }

    public static Plotly newPlot (Trace t0, Trace t1, Trace t2, Trace t3)
    {
        return (newPlot (t0, t1, t2, t3, null, null, null, null, null, null));
    }

    public static Plotly newPlot (Trace t0, Trace t1, Trace t2)
    {
        return (newPlot (t0, t1, t2, null, null, null, null, null, null, null));
    }

    public static Plotly newPlot (Trace t0, Trace t1)
    {
        return (newPlot (t0, t1, null, null, null, null, null, null, null, null));
    }

    public static Plotly newPlot (Trace t0)
    {
        return (newPlot (t0, null, null, null, null, null, null, null, null, null));
    }

    public String getJsonData ()
    {
        if (json_data == null)
        {
            json_data = pretty.toJson (map_data);
        }
        return (json_data);
    }

    public String getJsonLayout ()
    {
        return (layout.toPrettyJson ());
    }

    @Override
    public void addObserver (Observer observer)
    {
        observers.addObserver (observer);
    }

    @Override
    public void deleteObserver (Observer observer)
    {
        observers.deleteObserver (observer);
    }

    @Override
    public Map<String, Object> serializeObject ()
    {
        for (int i = 0; i < trace_data.size (); i++)
        {
            properties.put ("data" + i, trace_data.get (i));
        }

        properties.put ("/", layout.toPrettyJson ());
        return (properties);
    }

    @Override
    public void deserializeObject (Map<String, Object> properties)
    {
        trace_data.clear ();
        map_data.clear ();

        this.properties.putAll (properties);

        Object obj;
        int i = 0;

        while ((obj = properties.get ("data" + i)) != null)
        {
            if (obj instanceof Trace)
            {
                addTrace ((Trace)obj);
            }
            i++;
        }

        layout.fromJson ((String)properties.get ("/"));
    }
}

// EOF
