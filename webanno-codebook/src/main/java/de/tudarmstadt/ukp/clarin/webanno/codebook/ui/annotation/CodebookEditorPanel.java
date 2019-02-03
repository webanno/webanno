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
package de.tudarmstadt.ukp.clarin.webanno.codebook.ui.annotation;

import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.getAddr;

import java.io.IOException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.jcas.JCas;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.PageableListView;
import org.apache.wicket.markup.html.navigation.paging.PagingNavigator;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.util.CollectionModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.googlecode.wicket.kendo.ui.form.combobox.ComboBox;
import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.AnnotationEditorExtensionRegistry;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.coloring.ColoringStrategy;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.AnnotationException;
import de.tudarmstadt.ukp.clarin.webanno.codebook.adapter.CodebookAdapter;
import de.tudarmstadt.ukp.clarin.webanno.codebook.model.Codebook;
import de.tudarmstadt.ukp.clarin.webanno.codebook.model.CodebookFeature;
import de.tudarmstadt.ukp.clarin.webanno.codebook.service.CodebookFeatureState;
import de.tudarmstadt.ukp.clarin.webanno.codebook.service.CodebookSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.model.Tag;

public  class CodebookEditorPanel extends Panel {
    private static final long serialVersionUID = -9151455840010092452L;

    private @SpringBean ProjectService projectRepository;

    private @SpringBean DocumentService documentService;
    private @SpringBean AnnotationSchemaService annotationService;
    private @SpringBean CodebookSchemaService codebookService;
    private @SpringBean AnnotationEditorExtensionRegistry extensionRegistry;

    private CodebookEditorModel as;

    private WebMarkupContainer codebooksGroup;
    private PageableListView<CodebookEditorModel> codebooks;
    private PagingNavigator navigator;

    private static final Logger LOG = LoggerFactory.getLogger(CodebookEditorPanel.class);

    public CodebookEditorModel getModelObject() {
        return (CodebookEditorModel) getDefaultModelObject();
    }

    public CodebookEditorPanel(String id, IModel<CodebookEditorModel> aModel) {
        super(id, aModel);

        setOutputMarkupId(true);

        as = aModel.getObject();
        int codebooksPerPage = as == null ? 10 : as.getCodebooksPerPage();
        codebooks = new PageableListView<CodebookEditorModel>("codebooks", getCodebooksModel(),
                codebooksPerPage) {
            private static final long serialVersionUID = 1L;


            @Override
            protected void populateItem(final ListItem<CodebookEditorModel> item) {
                final CodebookEditorModel model = (CodebookEditorModel) item.getModelObject();
                Codebook codebook = model.getCodebook();
                item.add(new Label("codebook", codebook.getUiName()));
                List<String> codes = getTags(codebook);

                CodebookAdapter adapter = new CodebookAdapter(codebook);
                CodebookFeature feature = codebookService.listCodebookFeature(codebook).get(0);

                JCas jcas = null;
                try {
                    jcas = getCodebookCas();
                } catch (IOException e1) {
                    // TODO why it is here??
                }
                String existingCode = (String) adapter.getExistingCodeValue(jcas, feature);
                ComboBox<String> code = new ComboBox<String>("code",
                        new Model<String>(existingCode), codes);
                code.add(new AjaxFormComponentUpdatingBehavior("change") {
                    private static final long serialVersionUID = 5179816588460867471L;

                    @Override
                    public void onUpdate(AjaxRequestTarget aTarget) {
                        try {
                            JCas jcas  = getCodebookCas();
                            if (code.getModelObject() == null) {
                                CodebookAdapter adapter = new CodebookAdapter(codebook);
                                adapter.delete(jcas, feature);
                                writeCodebookCas(jcas);
                                return;
                            }
                            CodebookEditorModel state = CodebookEditorPanel.this.getModelObject();
                            state.getCodebookFeatureStates()
                                    .add(new CodebookFeatureState(feature, code.getModelObject()));
                            saveCodebookAnnotation(feature, jcas);
                        } catch (IOException | AnnotationException e) {
                            error("Unable to update" + e.getMessage());
                        }
                    }
                });
                code.add(new Behavior() {
                    private static final long serialVersionUID = -8375331706930026335L;

                    @Override
                    public void onConfigure(final Component component) {
                        super.onConfigure(component);
                    }
                });
                code.add(new AttributeModifier("style",
                        ColoringStrategy.getCodebookBgStyle()));
                item.add(new AttributeModifier("style",
                        ColoringStrategy.getCodebookBgStyle()));
                item.add(code);
            }
        };
        codebooks.setOutputMarkupId(true);
        codebooksGroup = new WebMarkupContainer("codebooksGroup");
        codebooksGroup.setOutputMarkupId(true);

        navigator = new PagingNavigator("navigator", codebooks);
        navigator.setOutputMarkupId(true);
        codebooksGroup.add(navigator);

        codebooksGroup.add(codebooks);

        IModel<Collection<Codebook>> codebooksToAdeModel = new CollectionModel<>(new ArrayList<>());
        Form<Collection<Codebook>> form = new Form<>("form", codebooksToAdeModel);
        add(form);
        form.add(codebooksGroup);
    }

    private List<CodebookEditorModel> getCodebooksModel() {
        List<CodebookEditorModel> codebooks = new ArrayList<CodebookEditorModel>();

        for (Codebook codebook : listCodebooks()) {
            codebooks.add(new CodebookEditorModel(codebook));
        }

        return codebooks;
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

    private List<Codebook> listCodebooks() {
        if (as == null) {
            return new ArrayList<>();
        }
        return codebookService.listCodebook(as.getProject());
    }

    public void setProjectModel(AjaxRequestTarget aTarget, CodebookEditorModel aState) {
        as = aState;
        setDefaultModelObject(as);
        codebooks.setModelObject(getCodebooksModel());
        codebooks.setItemsPerPage(as.getCodebooksPerPage());
        navigator.add(new AttributeModifier("style",
                getCodebooksModel().size() <= as.getCodebooksPerPage()
                        ? "visibility:hidden;display:none"
                        : "visibility:visible"));
        aTarget.add(navigator);
        aTarget.add(codebooksGroup);
        List<Codebook> codebooks = new ArrayList<>();
        getCodebooksModel().stream().forEach(c -> {
            codebooks.add(c.getCodebook());
        });
    }

    private void saveCodebookAnnotation(CodebookFeature aCodebookFeature, JCas aJCas)
            throws AnnotationException, IOException {

        CodebookAdapter adapter = new CodebookAdapter(aCodebookFeature.getCodebook());
        writeCodebookFeatureModelsToCas(adapter, aJCas);

        // persist changes
        writeCodebookCas(aJCas);

    }

    private void writeCodebookFeatureModelsToCas(CodebookAdapter aAdapter, JCas aJCas)
            throws IOException, AnnotationException {
        CodebookEditorModel state = getModelObject();
        List<CodebookFeatureState> featureStates = state.getCodebookFeatureStates();

        for (CodebookFeatureState featureState : featureStates) {

            // For string features with extensible tagsets, extend the tagset
            if (CAS.TYPE_NAME_STRING.equals(featureState.feature.getType())) {
                String value = (String) featureState.value;

                if (value != null && featureState.feature.getTagset() != null
                        && featureState.feature.getTagset().isCreateTag()
                        && !annotationService.existsTag(value, featureState.feature.getTagset())) {
                    Tag selectedTag = new Tag();
                    selectedTag.setName(value);
                    selectedTag.setTagSet(featureState.feature.getTagset());
                    annotationService.createTag(selectedTag);
                }
            }

            LOG.trace("writeFeatureEditorModelsToCas() " + featureState.feature.getUiName() + " = "
                    + featureState.value);

            AnnotationFS existingFs = aAdapter.getExistingFs(aJCas, featureState.feature);
            int annoId;

            if (existingFs != null) {
                annoId = getAddr(existingFs);
            } else {
                annoId = aAdapter.add(aJCas);
            }
            aAdapter.setFeatureValue(aJCas, featureState.feature, annoId, featureState.value);
        }
    }

    public JCas getCodebookCas() throws IOException {
        CodebookEditorModel state = getModelObject();

        if (state.getDocument() == null) {
            throw new IllegalStateException("Please open a document first!");
        }
        return (onGetJCas());
    }

    private void writeCodebookCas(JCas aJCas) throws IOException {
       
        CodebookEditorModel state = getModelObject();
        documentService.writeAnnotationCas(aJCas, state.getDocument(), state.getUser(), true);

        // Update timestamp in state
        Optional<Long> diskTimestamp = documentService
                .getAnnotationCasTimestamp(state.getDocument(), state.getUser().getUsername());
        if (diskTimestamp.isPresent()) {
            onJcasUpdate(diskTimestamp.get());
        }
    }
    
    protected void onJcasUpdate(Long aTimeStamp) {
        // Overriden in CurationPanel
    }
    
    protected JCas onGetJCas() throws IOException {     
        return null;
    }
}
