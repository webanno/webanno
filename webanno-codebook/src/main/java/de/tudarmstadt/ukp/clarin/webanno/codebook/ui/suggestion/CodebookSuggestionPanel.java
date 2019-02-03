/*
 * Copyright 2019
 * Ubiquitous Knowledge Processing (UKP) Lab Technische Universität Darmstadt
 * and Language Technology lab Universität Hamburg
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
package de.tudarmstadt.ukp.clarin.webanno.codebook.ui.suggestion;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.uima.jcas.JCas;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.PageableListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.util.CollectionModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.AnnotationEditorExtensionRegistry;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.coloring.ColoringStrategy;
import de.tudarmstadt.ukp.clarin.webanno.codebook.adapter.CodebookAdapter;
import de.tudarmstadt.ukp.clarin.webanno.codebook.model.Codebook;
import de.tudarmstadt.ukp.clarin.webanno.codebook.model.CodebookFeature;
import de.tudarmstadt.ukp.clarin.webanno.codebook.service.CodebookDiff;
import de.tudarmstadt.ukp.clarin.webanno.codebook.service.CodebookSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.codebook.ui.CurationUtil;
import de.tudarmstadt.ukp.clarin.webanno.codebook.ui.curation.CodebookCurationModel;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff2.DiffResult;
import de.tudarmstadt.ukp.clarin.webanno.curation.storage.CurationDocumentService;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.Tag;

public class CodebookSuggestionPanel extends Panel {
    private static final long serialVersionUID = -9151455840010092452L;

    private @SpringBean ProjectService projectRepository;

    private @SpringBean DocumentService documentService;
    private @SpringBean CurationDocumentService curationDocumentService;
    private @SpringBean AnnotationSchemaService annotationService;
    private @SpringBean CodebookSchemaService codebookService;
    private @SpringBean AnnotationEditorExtensionRegistry extensionRegistry;

    private CodebookCurationModel cModel;
    private WebMarkupContainer codebooksGroup;
    private PageableListView<CodebookSuggestions> suggestions;

    public CodebookCurationModel getModelObject() {
        return (CodebookCurationModel) getDefaultModelObject();
    }

    public CodebookSuggestionPanel(String id, IModel<CodebookCurationModel> aModel) {
        super(id, aModel);

        setOutputMarkupId(true);

        cModel = aModel.getObject();
        suggestions = new PageableListView<CodebookSuggestions>("suggestions",
                cModel.getCodebooksuggestions(), cModel.getCodebooksuggestions().size()) {

            private static final long serialVersionUID = -3591948738133097041L;

            protected void populateItem(final ListItem<CodebookSuggestions> item) {
                final CodebookSuggestions suggestion = (CodebookSuggestions) item.getModelObject();
                item.add(new Label("codebook",suggestion.getCodebook().getUiName()));
                item.add(new Label("username", suggestion.getUsername()));
                item.add(new Label("annotation", suggestion.getAnnotation())
                        .add(new AttributeModifier("style",
                                ColoringStrategy.getCodebookDiffColor(suggestion.isHasDiff()))));

                AjaxLink<String> alink = new AjaxLink<String>("merge",
                        Model.of(suggestion.getAnnotation())) {

                    private static final long serialVersionUID = -4235258547224541472L;

                    @Override
                    public void onClick(AjaxRequestTarget aTarget) {
                        onMerge(aTarget, suggestion.getFeature(), suggestion.getAnnotation());
                        onShowSuggestions(aTarget, suggestion.getFeature());

                    }
                };
                // alink.add(new AttributeModifier("style",
                // ColoringStrategy.getCodebookDiffColor(suggestion.isHasDiff())));
                item.add(alink);
            }
        };
        suggestions.setOutputMarkupId(true);
        codebooksGroup = new WebMarkupContainer("codebooksGroup");
        codebooksGroup.setOutputMarkupId(true);

        codebooksGroup.add(suggestions);

        IModel<Collection<Codebook>> codebooksToAdeModel = new CollectionModel<>(new ArrayList<>());
        Form<Collection<Codebook>> form = new Form<>("form", codebooksToAdeModel);
        add(form);
        form.add(codebooksGroup);
    }

    public void setSuggestionModel(AjaxRequestTarget aTarget, CodebookFeature aFeature) {
        List<CodebookSuggestions> suggestionsModel = getSuggestions(aFeature);
        suggestions.setModelObject(suggestionsModel);
        suggestions.setItemsPerPage(suggestionsModel.size());
    }

    public void setModel(AjaxRequestTarget aTarget, CodebookCurationModel aModel) {
        cModel = aModel;
        setDefaultModelObject(cModel);
    }

    List<String> getTags(Codebook aCodebook) {
        if (codebookService.listCodebookFeature(aCodebook) == null
                || codebookService.listCodebookFeature(aCodebook).size() == 0) {
            return new ArrayList<>();
        }
        CodebookFeature codebookFeature = codebookService.listCodebookFeature(aCodebook).get(0);
        if (codebookFeature.getTagset() == null) {
            return new ArrayList<>();
        }
        List<String> tags = new ArrayList<>();
        for (Tag tag : annotationService.listTags(codebookFeature.getTagset())) {
            tags.add(tag.getName());
        }
        return tags;
    }

    private List<CodebookSuggestions> getSuggestions(CodebookFeature feature) {
        Map<String, JCas> jCases = setSuggestionCases();
        List<Codebook> types = new ArrayList<>();
        types.add(feature.getCodebook());
        CodebookAdapter adapter = new CodebookAdapter(feature.getCodebook());
        List<CodebookSuggestions> suggestions = new ArrayList<>();

        for (String username : jCases.keySet()) {

            if (username.equals(WebAnnoConst.CURATION_USER)) {
                continue;
            }
            String existingCode = (String) adapter.getExistingCodeValue(jCases.get(username),
                    feature);
            Map<String, JCas> suggestionCas = new HashMap<>();
            suggestionCas.put(username, jCases.get(username));
            suggestionCas.put(WebAnnoConst.CURATION_USER, jCases.get(WebAnnoConst.CURATION_USER));
            CodebookSuggestions suggestion = new CodebookSuggestions(username, existingCode,
                    isDiffs(feature.getCodebook(), types, suggestionCas), feature.getCodebook(),
                    feature);
            suggestions.add(suggestion);

        }
        return suggestions;
    }

    private Map<String, JCas> setSuggestionCases() {
        Map<String, JCas> jCases = new HashMap<>();
        List<AnnotationDocument> annotationDocuments = documentService
                .listAnnotationDocuments(cModel.getDocument());
        for (AnnotationDocument annotationDocument : annotationDocuments) {
            String username = annotationDocument.getUser();
            if (annotationDocument.getState().equals(AnnotationDocumentState.FINISHED)
                    || username.equals(WebAnnoConst.CURATION_USER)) {
                JCas jCas;
                try {
                    jCas = documentService.readAnnotationCas(annotationDocument);
                    jCases.put(username, jCas);
                } catch (IOException e) {
                    error("Unable to load the curation CASes" + e.getMessage());
                }

            }
        }
        try {
            jCases.put(WebAnnoConst.CURATION_USER, getCas());
        } catch (IOException e) {
            error("Unable to load the curation CASes" + e.getMessage());
        }

        return jCases;
    }

    private boolean isDiffs(Codebook codebook, List<Codebook> types, Map<String, JCas> jCases) {
        DiffResult diff = CodebookDiff
                .doCodebookDiff(codebookService, codebook.getProject(),
                        CurationUtil.getCodebookTypes(
                                jCases.get(CurationUtil.CURATION_USER), types),
                        null, jCases, 0, 0);
        if (diff.getIncompleteConfigurationSets().size() > 0) {
            return true;
        }
        if (diff.getDifferingConfigurationSets().size() > 0) {
            return true;
        }
        return false;
    }

    public JCas getCas() throws IOException {
        CodebookCurationModel state = getModelObject();
        return curationDocumentService.readCurationCas(state.getDocument());

    }

    protected void onMerge(AjaxRequestTarget aTarget, CodebookFeature aFeature,
            String aAnnotation) {
        // Overriden in CurationPanel
    }

    protected void onShowSuggestions(AjaxRequestTarget aTarget, CodebookFeature aFeature) {
        // Overriden in CurationPanel
    }

}
