/*
 * Copyright 2020
 * Ubiquitous Knowledge Processing (UKP) Lab Technische Universität Darmstadt
 * and  Language Technology Universität Hamburg
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.clarin.webanno.codebook.ui.analysis.ngram;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.extensions.ajax.markup.html.tabs.AjaxTabbedPanel;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import de.tudarmstadt.ukp.clarin.webanno.support.bootstrap.BootstrapAjaxTabbedPanel;

public class NGramTabsPanel
    extends Panel
{
    private static final long serialVersionUID = 7406558747437511453L;

    private AjaxTabbedPanel<ITab> nGramTabPanel;

    public NGramTabsPanel(String id, IModel<NGramStats> statsModel)
    {
        super(id, statsModel);
        this.setOutputMarkupPlaceholderTag(true);
        this.createTabPanel();
    }

    @Override
    protected void onModelChanged()
    {
        super.onModelChanged();
        this.createTabPanel();
    }

    private void createTabPanel()
    {

        nGramTabPanel = new BootstrapAjaxTabbedPanel<>("tabPanel", makeTabs());
        nGramTabPanel.setOutputMarkupPlaceholderTag(true);
        this.addOrReplace(nGramTabPanel);
    }

    private List<ITab> makeTabs()
    {
        List<ITab> tabs = new ArrayList<>();

        for (int n = 0; n < NGramStatsFactory.MAX_N_GRAM; n++) {
            // create 1 tab per n-gram, maybe extend to keywords and other useful statistics
            // like summaries
            int finalN = n;
            String panelName = (finalN + 1) + "-grams";
            tabs.add(new AbstractTab(Model.of(panelName))
            {
                private static final long serialVersionUID = 2809743572231646654L;

                @Override
                public WebMarkupContainer getPanel(String panelId)
                {
                    return new NGramStatsPanel(panelId,
                            (IModel<NGramStats>) NGramTabsPanel.this.getDefaultModel(), finalN);
                }
            });
        }

        return tabs;
    }
}
