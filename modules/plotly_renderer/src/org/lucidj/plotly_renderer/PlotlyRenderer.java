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

package org.lucidj.plotly_renderer;

import java.util.HashMap;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.lucidj.api.vui.Renderer;
import org.lucidj.plotly.Plotly;
import org.lucidj.plotly.Trace;
import xyz.kuori.timeseries.TimeSeries;

import com.vaadin.annotations.JavaScript;
import com.vaadin.ui.AbstractJavaScriptComponent;
import com.vaadin.ui.Component;

import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

/* Plotly, what a neat library!
 * Be sure to visit https://plot.ly/ to know more.
 */

@JavaScript ({ "vaadin://~/libraries_plotly_renderer/plotly-latest.min.js", "plotlyrenderer.js" })
@org.apache.felix.ipojo.annotations.Component (immediate = true)
@Instantiate
@Provides (specifications = Renderer.class)
public class PlotlyRenderer extends AbstractJavaScriptComponent implements Renderer
{
    private HashMap attrs = new HashMap ();
    private transient Gson gson = new Gson ();
    private transient Gson pretty = new GsonBuilder().setPrettyPrinting().create();

    private Plotly source;

    public PlotlyRenderer ()
    {
        getState ().data = "[[0,0],[0,1]]";
        getState ().layout = "{}";
        setWidth (100, Unit.PERCENTAGE);
    }

    @Override
    protected PlotlyRendererState getState ()
    {
        return ((PlotlyRendererState)super.getState ());
    }

    public void setData (String data)
    {
        getState().data = data;
        markAsDirty();
    }

    public void setData (TimeSeries ts)
    {
        StringBuilder s = new StringBuilder ();

        long[] t = ts.getTimeStamps();
        double[] v = ts.getValues();

        s.append('[');

        for (int i = 0; i < t.length; i++)
        {
            s.append ('[');
            s.append (t [i]);
            s.append (',');
            s.append (v [i]);
            s.append (']');

            if (i < (t.length - 1))
            {
                s.append (',');
            }
        }

        s.append (']');

        getState ().data = s.toString ();
    }

    public void setData (List<TimeSeries> timeseries_list)
    {
        int i;

        //======================================================
        // Calcula o número somado de datapoints em todas as TS
        // e inicializa as referências de trabalho
        //======================================================

        int num_timeseries = timeseries_list.size ();
        int total_length = 0;

        long[][] ts_timestamps = new long [num_timeseries][];
        double[][] ts_values = new double [num_timeseries][];
        int[] ts_size = new int [num_timeseries];
        int[] cursor = new int [num_timeseries];

        for (i = 0; i < timeseries_list.size (); i++)
        {
            ts_timestamps [i] = timeseries_list.get (i).getTimeStamps ();
            ts_values [i] = timeseries_list.get (i).getValues ();
            ts_size [i] = timeseries_list.get (i).size ();
            total_length += ts_size [i];
            cursor [i] = 0;
        }

        //========================================================
        // Gera uma tabela com os dados justapostos por timestamp
        //========================================================

        StringBuilder s = new StringBuilder ();

        boolean more;
        long lowest_timestamp;

        s.append ('[');

        for (int count = 0; count < total_length; count++)
        {
            more = false;

            lowest_timestamp = -1;

            //======================================
            // Busca o menor timestamp não inserido
            //======================================

            for (i = 0; i < num_timeseries; i++)
            {
                if (cursor [i] < ts_size [i])
                {
                    more = true;

                    if (lowest_timestamp == -1 || lowest_timestamp > ts_timestamps [i][cursor [i]])
                    {
                        lowest_timestamp = ts_timestamps [i][cursor [i]];
                    }
                }
            }

            if (!more)
            {
                break;
            }

            //=========================================================
            // Copia o valor do datapoint correspondente, se ele
            // existir no TimeSeries de origem, ou null caso contrário
            //=========================================================

            if (count > 0)
            {
                s.append (',');
            }

            // Formato: [timestamp,v1,v2,v3,vN]

            s.append ('[');
            s.append (lowest_timestamp);

            for (i = 0; i < num_timeseries; i++)
            {
                s.append (',');

                if (cursor [i] < ts_size [i] &&
                    ts_timestamps [i][cursor [i]] == lowest_timestamp)
                {
                    if (Double.isNaN (ts_values [i][cursor [i]]))
                    {
                        s.append ("\"NaN\"");
                    }
                    else
                    {
                        s.append (ts_values [i][cursor [i]]);
                    }
                    cursor [i]++;
                }
                else
                {
                    s.append ("null");
                }
            }

            s.append (']');
        }

        s.append (']');

        //========================================
        // Resultado:
        // s = "[
        //        [ tstamp1, v1a, v2a, v3a, vNa ],
        //        [ tstamp2, v1b, v2b, v3b, vNb ],
        //        [ ... ]
        //      ]";
        //========================================
        getState ().data = s.toString ();
    }

    @SuppressWarnings("unchecked")
    private void set_layout (String key, Object value)
    {
        HashMap attr_map = attrs;

        if (key.contains (":"))
        {
            String[] keys = key.split (":");

            for (int i = 0; i < keys.length - 1; i++)
            {
                HashMap next_map = (HashMap)attr_map.get (keys [i]);

                if (next_map == null)
                {
                    next_map = new HashMap ();
                    attr_map.put (keys [i], next_map);
                }

                attr_map = next_map;
            }

            key = keys [keys.length - 1];
        }

        attr_map.put (key, value);

        getState().layout = gson.toJson (attrs);
        markAsDirty();
    }

    public String getPrettyJson ()
    {
        return (pretty.toJson (attrs));
    }

    public void setOption (String key, boolean value)
    {
        set_layout (key, value);
    }

    public void setOption (String key, int value)
    {
        set_layout (key, value);
    }

    public void setOption (String key, String value)
    {
        set_layout (key, value);
    }

    public void setOption (String key, Object[] value)
    {
        set_layout (key, value);
    }

    public static boolean isCompatible (Object object)
    {
        return (object instanceof Plotly || object instanceof Trace);
    }

    @Override // Renderer
    public void objectLinked (Object obj)
    {
        if (obj instanceof Trace)
        {
            source = Plotly.newPlot ()
                .margin (16, 32, 32, 32)
                .addTrace ((Trace)obj);
        }
        else
        {
            source = (Plotly)obj;
        }
    }

    @Override // Renderer
    public void objectUnlinked ()
    {
        source = null;
    }

    @Override // Renderer
    public Component renderingComponent ()
    {
        return (this);
    }

    @Override // Renderer
    public void objectUpdated ()
    {
        PlotlyRendererState state = getState ();

        state.data = source.getJsonData ();
        state.layout = source.getJsonLayout ();
    }
}

// EOF
