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
package de.tudarmstadt.ukp.clarin.webanno.codebook.ui.analysis.codebookstats;

import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.model.Model;

import de.tudarmstadt.ukp.clarin.webanno.codebook.ui.analysis.ListViewPanelFilterForm;
import de.tudarmstadt.ukp.clarin.webanno.codebook.ui.analysis.UpdateListViewCallback;

public class CodebookStatsFilterPanel
    extends ListViewPanelFilterForm
{
    private CheckBox annotators;
    private CheckBox curators;

    public CodebookStatsFilterPanel(String id, UpdateListViewCallback updateListViewCallBack)
    {
        super(id, updateListViewCallBack);
        // create tag checkbox
        annotators = new CheckBox("annotators", Model.of(Boolean.TRUE));
        curators = new CheckBox("curators", Model.of(Boolean.TRUE));
        this.filterForm.add(annotators, curators);
        this.setOutputMarkupId(true);
    }

    public boolean showFromCurators()
    {
        return this.curators.getModelObject();
    }

    public boolean showFromAnnotators()
    {
        return this.annotators.getModelObject();
    }

}
