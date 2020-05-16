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
package de.tudarmstadt.ukp.clarin.webanno.ui.annotation.sidebar.docstats;

import java.io.IOException;

import org.apache.uima.cas.CASException;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.codebook.ui.analysis.ngram.NGramStats;
import de.tudarmstadt.ukp.clarin.webanno.codebook.ui.analysis.ngram.NGramStatsFactory;
import de.tudarmstadt.ukp.clarin.webanno.codebook.ui.analysis.ngram.NGramTabsPanel;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.AnnotationPage;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.sidebar.AnnotationSidebar_ImplBase;

public class DocumentStatsSidebar
    extends AnnotationSidebar_ImplBase
{
    private static final long serialVersionUID = -694508827886594987L;

    private @SpringBean NGramStatsFactory nGramStatsFactory;
    private LoadableDetachableModel<NGramStats> stats;
    private NGramTabsPanel nGramTabPanel;

    public DocumentStatsSidebar(String aId, IModel<AnnotatorState> aModel,
            AnnotationPage aAnnotationPage)
    {
        super(aId, aModel, null, null, aAnnotationPage);

        this.stats = new LoadableDetachableModel<NGramStats>()
        {
            private static final long serialVersionUID = 4666440621462423520L;

            @Override
            protected NGramStats load()
            {
                try {
                    return nGramStatsFactory.create(aModel.getObject().getDocument());
                }
                catch (IOException | CASException e) {
                    // TODO what to throw or do?!
                    e.printStackTrace();
                }
                return null;
            }
        };

        nGramTabPanel = new NGramTabsPanel("nGramTabsPanel", this.stats);
        this.add(nGramTabPanel);
    }
}
