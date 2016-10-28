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

package xyz.kuori.plotchart;

import com.vaadin.ui.Component;
import xyz.kuori.dygraphs.Dygraphs;
import xyz.kuori.timeseries.TimeSeries;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class PlotChart implements Serializable
{
    private List<TimeSeries> tsl = new ArrayList<TimeSeries> ();
    private Dygraphs chart = null;

    public PlotChart ()
    {
        chart = new Dygraphs ();
        chart.setFile (tsl);
        //chart.setOption ("title", "Título do Gráfico 2");
        //chart.setOption ("labels", new String[] {"Data", "Números"});
        chart.setOption ("connectSeparatedPoints", true);
        chart.setOption ("labelsDivStyles:background", "transparent");
        chart.setOption ("labelsDivStyles:text-align", "right");
        chart.setOption ("showRoller", false);
        chart.setOption ("digitsAfterDecimal", 2);
        chart.setOption ("legend", "always");
        chart.setOption ("axes:x:ticker", "@Dygraph.numericTicks");
        chart.setOption ("showRangeSelector", true);
        chart.setOption ("interactionModel", "@Dygraph.Interaction.defaultModel");
    }

    public void addTimeSeries (TimeSeries ts)
    {
        tsl.add (ts);
    }

    public Component renderComponent ()
    {
        chart.setFile(tsl);
        return (chart);
    }

    //===================
    // Interface fluente
    //===================

    public PlotChart plot (TimeSeries ts)
    {
        tsl.add (ts);
        return (this);
    }

    public PlotChart opt (String option, String value)
    {
        chart.setOption (option, value);
        return (this);
    }

    public PlotChart opt (String option, int value)
    {
        chart.setOption (option, value);
        return (this);
    }

    public PlotChart opt (String option, boolean value)
    {
        chart.setOption (option, value);
        return (this);
    }
}

// EOF
