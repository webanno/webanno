/*
 * Copyright 2020
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
 * Technische Universität Darmstadt
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
package de.tudarmstadt.ukp.clarin.webanno.ui.annotation.sidebar.docinsights;

import org.apache.wicket.model.IModel;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.codebook.ui.analysis.ngramstats.NGramTabsPanel;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.AnnotationPage;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.sidebar.AnnotationSidebar_ImplBase;

public class DocumentInsightsSidebar
    extends AnnotationSidebar_ImplBase
{
    private static final long serialVersionUID = -694508827886594987L;

    public DocumentInsightsSidebar(String aId, IModel<AnnotatorState> aModel,
            AnnotationPage aAnnotationPage)
    {
        super(aId, aModel, null, null, aAnnotationPage);

        NGramTabsPanel<SourceDocument> nGramTabPanel = new NGramTabsPanel<>("nGramTabsPanel",
                aModel.getObject().getDocument());
        this.add(nGramTabPanel);
    }
}
