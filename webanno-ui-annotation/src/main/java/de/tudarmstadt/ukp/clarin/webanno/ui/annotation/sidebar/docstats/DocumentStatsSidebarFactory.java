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
import java.util.Collection;

import org.apache.uima.cas.CASException;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.Url;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.request.resource.UrlResourceReference;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.api.CasProvider;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.AnnotationPage;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.sidebar.AnnotationSidebarFactory_ImplBase;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.sidebar.AnnotationSidebar_ImplBase;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

@Component("documentStatsSidebar")
public class DocumentStatsSidebarFactory
    extends AnnotationSidebarFactory_ImplBase
{
    private static final ResourceReference ICON = new UrlResourceReference(
            Url.parse("images/document_stats.png")).setContextRelative(true);

    private @SpringBean DocumentService documentService;

    @Override
    public String getDisplayName()
    {
        return "Document Statistics";
    }

    @Override
    public ResourceReference getIcon()
    {
        return ICON;
    }

    @Override
    public AnnotationSidebar_ImplBase create(String aId, IModel<AnnotatorState> aModel,
            AnnotationActionHandler aActionHandler, CasProvider aCasProvider,
            AnnotationPage aAnnotationPage)
    {

        try {
            JCas jCas = aCasProvider.get().getJCas();
            Collection<Token> tokens = JCasUtil.select(jCas, Token.class);
            DocStats docStats = new DocStats(); // do we want this inside a IModel?!
            for (Token t : tokens)
                docStats.add(t.getText());
            return new DocumentStatsSidebar(aId, aModel, aAnnotationPage, docStats);
        }
        catch (CASException e) {
            e.printStackTrace();
        }
        catch (IOException e) {
            // TODO what to throw or log?!
            e.printStackTrace();
        }
        return null;
    }
}
