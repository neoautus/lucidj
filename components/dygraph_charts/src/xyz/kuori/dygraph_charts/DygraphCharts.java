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

package xyz.kuori.dygraph_charts;

import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.server.FontAwesome;
import com.vaadin.server.Resource;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import xyz.kuori.dygraphs.Dygraphs;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Property;
import org.apache.felix.ipojo.annotations.Provides;

@org.apache.felix.ipojo.annotations.Component
@Instantiate
@Provides(specifications = com.vaadin.navigator.View.class)
public class DygraphCharts extends VerticalLayout implements View
{
    @Property public String title = "Dygraph Charts";
    @Property public int weight = 250;
    @Property public Resource icon = FontAwesome.BAR_CHART_O;

    public void buildView ()
    {
        setMargin(true);

        Label h1 = new Label("Dygraph Charts");
        h1.addStyleName("h1");
        addComponent(h1);

        VerticalLayout content = new VerticalLayout();
        content.setSpacing(true);
        addComponent(content);

        Dygraphs chart1 = new Dygraphs ();
        chart1.setFile ("[[1443493924,0],[1443494925,1],[1443495926,4],[1443496927,9],[1443497928,16],[1443498929,25]]");
        chart1.setOption ("labels", new String[] {"Data", "Valores"});
        chart1.setOption ("labelsDivStyles:background", "transparent");
        chart1.setOption ("labelsDivStyles:text-align", "right");
        chart1.setOption ("gridLineColor", "#ddd");
        chart1.setOption ("axisLabelColor", "#555");
        chart1.setOption ("showRoller", false);
        chart1.setOption ("digitsAfterDecimal", 2);
        chart1.setOption ("legend", "always");
        chart1.setOption ("axes:x:valueFormatter", "@function (x) { return new Date (x*1000).toDateString (); }");
        chart1.setOption ("axes:x:axisLabelFormatter", "@function (x) { return new Date (x*1000).toDateString (); }");
        chart1.setOption ("axes:x:ticker", "@Dygraph.dateTicker");
        chart1.setOption ("showRangeSelector", true);
        chart1.setWidth ("100%");
        content.addComponent (chart1);

        Dygraphs chart2 = new Dygraphs ();
        chart2.setFile ("[[0,0],[1,1],[2,4],[3,9],[4,16],[5,25]]");
        chart2.setOption ("title", "Título do Gráfico 2");
        chart2.setOption ("labels", new String[] {"Data", "Números"});
        chart2.setOption ("labelsDivStyles:background", "transparent");
        chart2.setOption ("labelsDivStyles:text-align", "right");
        chart2.setOption ("showRoller", false);
        chart2.setOption ("digitsAfterDecimal", 2);
        chart2.setOption ("legend", "always");
        chart2.setOption ("axes:x:ticker", "@Dygraph.numericTicks");
        chart2.setOption ("showRangeSelector", true);
        chart2.setWidth ("100%");
        content.addComponent (chart2);
    }

    @Override
    public void enter(ViewChangeEvent event)
    {
        if (getComponentCount() == 0)
        {
            buildView();
        }
    }
}

// EOF
