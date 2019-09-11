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
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.googlecode.wicket.jquery.core.Options;
import com.googlecode.wicket.jquery.ui.widget.tooltip.TooltipBehavior;
import com.googlecode.wicket.kendo.ui.KendoUIBehavior;
import com.googlecode.wicket.kendo.ui.form.combobox.ComboBox;
import com.googlecode.wicket.kendo.ui.form.combobox.ComboBoxBehavior;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.AnnotationEditorExtensionRegistry;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.AnnotationException;
import de.tudarmstadt.ukp.clarin.webanno.codebook.adapter.CodebookAdapter;
import de.tudarmstadt.ukp.clarin.webanno.codebook.api.coloring.ColoringStrategy;
import de.tudarmstadt.ukp.clarin.webanno.codebook.config.CodebookLayoutCssResourceBehavior;
import de.tudarmstadt.ukp.clarin.webanno.codebook.model.Codebook;
import de.tudarmstadt.ukp.clarin.webanno.codebook.model.CodebookFeature;
import de.tudarmstadt.ukp.clarin.webanno.codebook.model.CodebookTag;
import de.tudarmstadt.ukp.clarin.webanno.codebook.service.CodebookFeatureState;
import de.tudarmstadt.ukp.clarin.webanno.codebook.service.CodebookSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.support.DescriptionTooltipBehavior;
import de.tudarmstadt.ukp.clarin.webanno.support.StyledComboBox;

public class CodebookEditorPanel extends Panel {
    /**
     * Function to return tooltip using jquery Docs for the JQuery tooltip widget
     * that we configure below: https://api.jqueryui.com/tooltip/
     */
    protected static final String FUNCTION_FOR_TOOLTIP = "function() { return "
            + "'<div class=\"tooltip-title\">'+($(this).text() "
            + "? $(this).text() : 'no title')+'</div>"
            + "<div class=\"tooltip-content tooltip-pre\">'+($(this).attr('title') "
            + "? $(this).attr('title') : 'no description' )+'</div>' }";
    private static final long serialVersionUID = -9151455840010092452L;
    private static final Logger LOG = LoggerFactory.getLogger(CodebookEditorPanel.class);
    private @SpringBean ProjectService projectRepository;
    private @SpringBean DocumentService documentService;
    private @SpringBean AnnotationSchemaService annotationService;
    private @SpringBean CodebookSchemaService codebookService;
    private @SpringBean AnnotationEditorExtensionRegistry extensionRegistry;
    private CodebookEditorModel as;
    private WebMarkupContainer codebooksGroup;
    private PageableListView<CodebookEditorModel> codebooks;
    private PagingNavigator navigator;

    public CodebookEditorPanel(String id, IModel<CodebookEditorModel> aModel) {
        super(id, aModel);

        setOutputMarkupId(true);
        add(CodebookLayoutCssResourceBehavior.get());
        as = aModel.getObject();
        int codebooksPerPage = as == null ? 10 : as.getCodebooksPerPage();
        codebooks = new PageableListView<CodebookEditorModel>("codebooks", getCodebooksModel(),
                codebooksPerPage) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(final ListItem<CodebookEditorModel> item) {
                final CodebookEditorModel model = item.getModelObject();
                Codebook codebook = model.getCodebook();
                item.add(new Label("codebook", codebook.getUiName()));
                List<CodebookTag> codes = getTags(codebook);

                CodebookAdapter adapter = new CodebookAdapter(codebook);
                CodebookFeature feature = codebookService.listCodebookFeature(codebook).get(0);

                CAS jcas = null;
                try {
                    jcas = getCodebookCas();
                } catch (IOException e1) {
                    // TODO why it is here??
                }
                String existingCode = (String) adapter.getExistingCodeValue(jcas, feature);

                // this adds a ComboBox where each item has a DescriptionTooltipBehavior
                // TODO I would encapsulate this in an own class for better reusability
                ComboBox<CodebookTag> code = new StyledComboBox<CodebookTag>("code",
                        new Model<>(existingCode), codes) {
                    private static final long serialVersionUID = -1735612345658462932L; // TODO
                                                                                        // generate
                                                                                        // valid ID

                    @Override
                    protected void onInitialize() {
                        super.onInitialize();

                        // Ensure proper order of the initializing JS header items: first combo box
                        // behavior (in super.onInitialize()), then tooltip.
                        Options options = new Options(
                                DescriptionTooltipBehavior.makeTooltipOptions());
                        options.set("content", FUNCTION_FOR_TOOLTIP);
                        add(new TooltipBehavior("#" + getMarkupId() + "_listbox *[title]",
                                options) {
                            private static final long serialVersionUID = 1854141593969780149L;

                            @Override
                            protected String $() {
                                // REC: It takes a moment for the KendoDatasource to load the data
                                // and
                                // for the Combobox to render the hidden dropdown. I did not find
                                // a way to hook into this process and to get notified when the
                                // data is available in the dropdown, so trying to handle this
                                // with a slight delay hoping that all is set up after 1 second.
                                return "try {setTimeout(function () { " + super.$()
                                        + " }, 1000); } catch (err) {}; ";
                            }
                        });
                    }

                    @Override
                    protected void onConfigure() {
                        super.onConfigure();

                        // Trigger a re-loading of the tagset from the server as constraints may
                        // have
                        // changed the ordering
                        Optional<AjaxRequestTarget> target = RequestCycle.get()
                                .find(AjaxRequestTarget.class);
                        if (target.isPresent()) {
                            LOG.trace("onInitialize() requesting datasource re-reading");
                            target.get()
                                    .appendJavaScript(String.format(
                                            "var $w = %s; if ($w) { $w.dataSource.read(); }",
                                            KendoUIBehavior.widget(this, ComboBoxBehavior.METHOD)));
                        }
                    }
                };

                code.add(new AjaxFormComponentUpdatingBehavior("change") {
                    private static final long serialVersionUID = 5179816588460867471L;

                    @Override
                    public void onUpdate(AjaxRequestTarget aTarget) {
                        try {
                            CAS jcas = getCodebookCas();
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
                code.add(new AttributeModifier("style", ColoringStrategy.getCodebookBgStyle()));
                item.add(new AttributeModifier("style", ColoringStrategy.getCodebookBgStyle()));
                item.add(new DescriptionTooltipBehavior(codebook.getUiName(),
                        codebook.getDescription()));
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

    public CodebookEditorModel getModelObject() {
        return (CodebookEditorModel) getDefaultModelObject();
    }

    private List<CodebookEditorModel> getCodebooksModel() {
        List<CodebookEditorModel> codebooks = new ArrayList<CodebookEditorModel>();

        for (Codebook codebook : listCodebooks()) {
            codebooks.add(new CodebookEditorModel(codebook));
        }

        return codebooks;
    }

    List<CodebookTag> getTags(Codebook aCodebook) {
        if (codebookService.listCodebookFeature(aCodebook) == null
                || codebookService.listCodebookFeature(aCodebook).size() == 0) {
            return new ArrayList<>();
        }
        CodebookFeature codebookFeature = codebookService.listCodebookFeature(aCodebook).get(0);
        if (codebookFeature.getCategory() == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(codebookService.listTags(codebookFeature.getCategory()));
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

    private void saveCodebookAnnotation(CodebookFeature aCodebookFeature, CAS aJCas)
            throws AnnotationException, IOException {

        CodebookAdapter adapter = new CodebookAdapter(aCodebookFeature.getCodebook());
        writeCodebookFeatureModelsToCas(adapter, aJCas);

        // persist changes
        writeCodebookCas(aJCas);

    }

    private void writeCodebookFeatureModelsToCas(CodebookAdapter aAdapter, CAS aJCas)
            throws IOException, AnnotationException {
        CodebookEditorModel state = getModelObject();
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

            LOG.trace("writeFeatureEditorModelsToCas() " + featureState.feature.getUiName() + " = "
                    + featureState.value);

            AnnotationFS existingFs = aAdapter.getExistingFs(aJCas/* , featureState.feature */);
            int annoId;

            if (existingFs != null) {
                annoId = getAddr(existingFs);
            } else {
                annoId = aAdapter.add(aJCas);
            }
            aAdapter.setFeatureValue(aJCas, featureState.feature, annoId, featureState.value);
        }
    }

    public CAS getCodebookCas() throws IOException {
        CodebookEditorModel state = getModelObject();

        if (state.getDocument() == null) {
            throw new IllegalStateException("Please open a document first!");
        }
        return (onGetJCas());
    }

    private void writeCodebookCas(CAS aJCas) throws IOException {

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

    protected CAS onGetJCas() throws IOException {
        return null;
    }
}
