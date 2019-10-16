/*
 * Copyright 2019
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
package de.tudarmstadt.ukp.clarin.webanno.codebook.ui.curation;

import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.getAddr;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.Behavior;
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

import com.googlecode.wicket.kendo.ui.form.combobox.ComboBox;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.AnnotationEditorExtensionRegistry;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.AnnotationException;
import de.tudarmstadt.ukp.clarin.webanno.codebook.adapter.CodebookAdapter;
import de.tudarmstadt.ukp.clarin.webanno.codebook.api.coloring.ColoringStrategy;
import de.tudarmstadt.ukp.clarin.webanno.codebook.config.CodebookLayoutCssResourceBehavior;
import de.tudarmstadt.ukp.clarin.webanno.codebook.model.Codebook;
import de.tudarmstadt.ukp.clarin.webanno.codebook.model.CodebookFeature;
import de.tudarmstadt.ukp.clarin.webanno.codebook.model.CodebookTag;
import de.tudarmstadt.ukp.clarin.webanno.codebook.service.CodebookDiff;
import de.tudarmstadt.ukp.clarin.webanno.codebook.service.CodebookFeatureState;
import de.tudarmstadt.ukp.clarin.webanno.codebook.service.CodebookSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.codebook.ui.CurationUtil;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff.DiffResult;
import de.tudarmstadt.ukp.clarin.webanno.curation.storage.CurationDocumentService;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;

public class CodebookCurationPanel
    extends Panel
{
    private static final long serialVersionUID = -9151455840010092452L;

    private @SpringBean ProjectService projectRepository;

    private @SpringBean DocumentService documentService;
    private @SpringBean CurationDocumentService curationDocumentService;
    private @SpringBean AnnotationSchemaService annotationService;
    private @SpringBean CodebookSchemaService codebookService;
    private @SpringBean AnnotationEditorExtensionRegistry extensionRegistry;

    private CodebookCurationModel cModel;
    private WebMarkupContainer codebooksGroup;
    private PageableListView<CodebookCurations> curations;

    public CodebookCurationModel getModelObject()
    {
        return (CodebookCurationModel) getDefaultModelObject();
    }

    public CodebookCurationPanel(String id, IModel<CodebookCurationModel> aModel)
    {
        super(id, aModel);

        setOutputMarkupId(true);
        add(CodebookLayoutCssResourceBehavior.get());
        cModel = aModel.getObject();

        curations = new PageableListView<CodebookCurations>("curations",
                cModel.getCodebookCurations(), cModel.getCodebookCurations().size())
        {

            private static final long serialVersionUID = -8038013944939655952L;

            @Override
            protected void populateItem(final ListItem<CodebookCurations> item)
            {
                final CodebookCurations curationsModel = item.getModelObject();
                item.add(new Label("codebook", curationsModel.getCodebook().getUiName()));

                AjaxLink<String> alink = new AjaxLink<String>("showSuggestors",
                        Model.of(curationsModel.getAnnotation()))
                {

                    private static final long serialVersionUID = -5714648706174235978L;

                    @Override
                    public void onClick(AjaxRequestTarget aTarget)
                    {

                        onShowSuggestions(aTarget, curationsModel.getFeature());
                    }
                };
                // alink.add(new AttributeModifier("style",
                // ColoringStrategy.getCodebookDiffColor(suggestion.isHasDiff())));
                item.add(alink);
                List<String> codes = getTags(curationsModel.getCodebook());
                ComboBox<String> code = new ComboBox<String>("annottaion",
                        Model.of(curationsModel.getAnnotation()), codes);
                code.add(new AjaxFormComponentUpdatingBehavior("change")
                {
                    private static final long serialVersionUID = 5179816588460867471L;

                    @Override
                    public void onUpdate(AjaxRequestTarget aTarget)
                    {
                        System.out.println("BLUR");
                        try {
                            saveCodebookAnnotation(curationsModel.getFeature(),
                                    code.getModelObject());
                            onShowSuggestions(aTarget, curationsModel.getFeature());
                            System.out.println("Saved");
                        }
                        catch (IOException | AnnotationException e) {
                            error("Unable to save the annotation " + e.getMessage());
                        }
                    }
                });
                code.add(new Behavior()
                {
                    private static final long serialVersionUID = -8375331706930026335L;

                    @Override
                    public void onConfigure(final Component component)
                    {
                        super.onConfigure(component);
                        // component.setEnabled(!codes.isEmpty());
                    }
                });

                code.add(new AttributeModifier("style", ColoringStrategy.getCodebookDiffColor(
                        curationsModel.isHasDiff(), curationsModel.getAnnotation() == null)));
                item.add(code);
            }
        };
        curations.setOutputMarkupId(true);
        codebooksGroup = new WebMarkupContainer("codebooksGroup");
        codebooksGroup.setOutputMarkupId(true);

        codebooksGroup.add(curations);

        IModel<Collection<Codebook>> codebooksToAdeModel = new CollectionModel<>(new ArrayList<>());
        Form<Collection<Codebook>> form = new Form<>("form", codebooksToAdeModel);
        add(form);
        form.add(codebooksGroup);
    }

    public void setProjectModel(AjaxRequestTarget aTarget, CodebookCurationModel aState)
    {
        cModel = aState;
        setDefaultModelObject(cModel);
        cModel.setCodebookCurations(getCurations());
        setDefaultModelObject(cModel);
        curations.setModelObject(cModel.getCodebookCurations());
        curations.setItemsPerPage(cModel.getCodebookCurations().size());
        // navigator.add(new AttributeModifier("style",
        // codebooksModel.size() <= 10 ? "visibility:hidden;display:none"
        // : "visibility:visible"));
        // aTarget.add(navigator);
        // aTarget.add(codebooksGroup);

    }

    public void setSuggestionModel(AjaxRequestTarget aTarget, CodebookFeature aFeature)
    {
        List<CodebookCurations> suggestionsModel = getCurations();
        curations.setModelObject(suggestionsModel);
        curations.setItemsPerPage(suggestionsModel.size());
    }

    private List<CodebookCurations> getCurations()
    {
        List<CodebookCurations> codebooksModel = new ArrayList<>();

        if (cModel == null || cModel.getDocument() == null) {
            return codebooksModel;
        }

        Map<String, CAS> jCases = setSuggestionCases();
        for (Codebook codebook : listCodebooks()) {

            List<Codebook> types = new ArrayList<>();
            types.add(codebook);

            CodebookAdapter adapter = new CodebookAdapter(codebook);
            CodebookFeature feature = codebookService.listCodebookFeature(codebook).get(0);

            String annotation = (String) adapter
                    .getExistingCodeValue(jCases.get(WebAnnoConst.CURATION_USER), feature);
            codebooksModel.add(new CodebookCurations(annotation,
                    isDiffs(feature.getCodebook(), types, jCases), codebook, feature));
        }
        return codebooksModel;
    }

    private boolean isDiffs(Codebook codebook, List<Codebook> types, Map<String, CAS> jCases)
    {
        DiffResult diff = CodebookDiff.doCodebookDiff(codebookService, codebook.getProject(),
                CurationUtil.getCodebookTypes(jCases.get(CurationUtil.CURATION_USER), types), null,
                jCases, 0, 0);
        if (diff.getIncompleteConfigurationSets().size() > 0) {
            return true;
        }
        return diff.getDifferingConfigurationSets().size() > 0;
    }

    private List<Codebook> listCodebooks()
    {
        if (cModel == null) {
            return new ArrayList<>();
        }
        return codebookService.listCodebook(cModel.getProject());
    }

    public void setModel(AjaxRequestTarget aTarget, CodebookCurationModel aModel)
    {
        cModel = aModel;
        setDefaultModelObject(cModel);
    }

    List<String> getTags(Codebook aCodebook)
    {
        if (codebookService.listCodebookFeature(aCodebook) == null
                || codebookService.listCodebookFeature(aCodebook).size() == 0) {
            return new ArrayList<>();
        }
        CodebookFeature codebookFeature = codebookService.listCodebookFeature(aCodebook).get(0);
        if (codebookFeature.getCategory() == null) {
            return new ArrayList<>();
        }
        List<String> tags = new ArrayList<>();
        for (CodebookTag tag : codebookService.listTags(codebookFeature.getCategory())) {
            tags.add(tag.getName());
        }
        return tags;
    }

    private Map<String, CAS> setSuggestionCases()
    {
        Map<String, CAS> jCases = new HashMap<>();
        List<AnnotationDocument> annotationDocuments = documentService
                .listAnnotationDocuments(cModel.getDocument());
        for (AnnotationDocument annotationDocument : annotationDocuments) {
            String username = annotationDocument.getUser();
            if (annotationDocument.getState().equals(AnnotationDocumentState.FINISHED)
                    || username.equals(WebAnnoConst.CURATION_USER)) {
                CAS jCas;
                try {
                    jCas = documentService.readAnnotationCas(annotationDocument);
                    jCases.put(username, jCas);
                }
                catch (IOException e) {
                    error("Unable to load the curation CASes" + e.getMessage());
                }

            }
        }
        try {
            jCases.put(WebAnnoConst.CURATION_USER, getCas());
        }
        catch (IOException e) {
            error("Unable to load the curation CASes" + e.getMessage());
        }

        return jCases;
    }

    public CAS getCas() throws IOException
    {
        CodebookCurationModel state = getModelObject();
        return curationDocumentService.readCurationCas(state.getDocument());

    }

    public void update(AjaxRequestTarget aTarget, CodebookFeature aFeature, String aAnnotation)
    {
        try {
            saveCodebookAnnotation(aFeature, aAnnotation);
            List<CodebookCurations> curationsModel = getCurations();
            curations.setModelObject(curationsModel);
            curations.setItemsPerPage(curationsModel.size() + 1);

        }
        catch (AnnotationException | IOException e) {
            error("Unable to save the annotation " + e.getMessage());
        }
    }

    private void saveCodebookAnnotation(CodebookFeature aCodebookFeature, String aAnnotation)
        throws AnnotationException, IOException
    {

        CodebookAdapter adapter = new CodebookAdapter(aCodebookFeature.getCodebook());
        CAS jcas = getCas();
        if (aAnnotation == null) {
            adapter.delete(jcas, aCodebookFeature);
            writerCas(jcas);
            return;
        }

        CodebookCurationModel state = this.getModelObject();
        state.getCodebookFeatureStates()
                .add(new CodebookFeatureState(aCodebookFeature, aAnnotation));
        writeCodebookFeatureModelsToCas(adapter, jcas);
        // persist changes
        writerCas(jcas);

    }

    private void writeCodebookFeatureModelsToCas(CodebookAdapter aAdapter, CAS aJCas)
        throws IOException, AnnotationException
    {
        CodebookCurationModel state = getModelObject();
        List<CodebookFeatureState> featureStates = state.getCodebookFeatureStates();

        for (CodebookFeatureState featureState : featureStates) {

            // For string features with extensible tagsets, extend the tagset
            if (CAS.TYPE_NAME_STRING.equals(featureState.feature.getType())) {
                String value = (String) featureState.value;

                if (value != null && featureState.feature.getCategory() != null
                        && featureState.feature.getCategory().isCreateTag() && !codebookService
                                .existsCodebookTag(value, featureState.feature.getCategory())) {
                    CodebookTag selectedTag = new CodebookTag();
                    selectedTag.setName(value);
                    selectedTag.setCategory(featureState.feature.getCategory());
                    codebookService.createCodebookTag(selectedTag);
                }
            }

            AnnotationFS existingFs = aAdapter.getExistingFs(aJCas/* , featureState.feature */);
            int annoId;

            if (existingFs != null) {
                annoId = getAddr(existingFs);
            }
            else {
                annoId = aAdapter.add(aJCas);
            }
            aAdapter.setFeatureValue(aJCas, featureState.feature, annoId, featureState.value);
        }
    }

    private void writerCas(CAS aJCas) throws IOException
    {
        CodebookCurationModel model = getModelObject();
        curationDocumentService.writeCurationCas(aJCas, model.getDocument(), true);

        // Update timestamp in state
        Optional<Long> diskTimestamp = curationDocumentService
                .getCurationCasTimestamp(model.getDocument());
        if (diskTimestamp.isPresent()) {
            onJcasUpdate(diskTimestamp.get());
        }
    }

    protected void onShowSuggestions(AjaxRequestTarget aTarget, CodebookFeature aFeature)
    {
        // Overriden in CurationPanel
    }

    protected void onJcasUpdate(Long aTimeStamp)
    {
        // Overriden in CurationPanel
    }

}
