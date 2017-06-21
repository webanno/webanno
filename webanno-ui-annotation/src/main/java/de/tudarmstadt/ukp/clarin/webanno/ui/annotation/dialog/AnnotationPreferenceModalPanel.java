/*
 * Copyright 2012
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
package de.tudarmstadt.ukp.clarin.webanno.ui.annotation.dialog;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.CheckBoxMultipleChoice;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.NumberTextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.AnnotationEditorFactory;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.AnnotationEditorRegistry;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.AnnotationException;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotationPreference;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.PreferencesUtil;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.detail.AnnotationDetailEditorPanel;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

/**
 * Modal Window to configure layers, window size, etc.
 */
public class AnnotationPreferenceModalPanel
    extends Panel
{
    private static final long serialVersionUID = -2102136855109258306L;

    private @SpringBean AnnotationSchemaService annotationService;
    private @SpringBean ProjectService projectService;
    private @SpringBean AnnotationEditorRegistry annotationEditorRegistry;

    private final AnnotationLayerDetailForm tagSelectionForm;

    private NumberTextField<Integer> windowSizeField;
    private NumberTextField<Integer> curationWindowSizeField;
    private NumberTextField<Integer> sidebarSizeField;
    private NumberTextField<Integer> fontSizeField;

    private final AnnotatorState bModel;

    private class AnnotationLayerDetailForm
        extends Form<AnnotationLayerDetailFormModel>
    {
        private static final long serialVersionUID = -683824912741426241L;

        public AnnotationLayerDetailForm(String id, final ModalWindow modalWindow,
                AnnotationDetailEditorPanel aEditor)
        {
            super(id, new CompoundPropertyModel<>(
                    new AnnotationLayerDetailFormModel()));

            // Import current settings from the annotator
            getModelObject().windowSize = bModel.getPreferences().getWindowSize() < 1 ? 1
                    : bModel.getPreferences().getWindowSize();
            getModelObject().curationWindowSize = bModel.getPreferences().getCurationWindowSize();
            getModelObject().sidebarSize = bModel.getPreferences().getSidebarSize();
            getModelObject().fontSize = bModel.getPreferences().getFontSize();
            getModelObject().scrollPage = bModel.getPreferences().isScrollPage();
            getModelObject().staticColor = bModel.getPreferences().isStaticColor();
            getModelObject().rememberLayer = bModel.getPreferences().isRememberLayer();

            String editorId = bModel.getPreferences().getEditor();
            AnnotationEditorFactory editorFactory = annotationEditorRegistry.getEditorFactory(editorId);
            if (editorFactory == null) {
                editorFactory = annotationEditorRegistry.getDefaultEditorFactory();
            }
            getModelObject().editor = Pair.of(editorFactory.getBeanName(), editorFactory.getDisplayName());
            
            for (AnnotationLayer layer : bModel.getAnnotationLayers()) {
                getModelObject().annotationLayers.add(layer);
            }
            
            windowSizeField = new NumberTextField<>("windowSize");
            windowSizeField.setType(Integer.class);
            windowSizeField.setMinimum(1);
            add(windowSizeField);

            sidebarSizeField = new NumberTextField<>("sidebarSize");
            sidebarSizeField.setType(Integer.class);
            sidebarSizeField.setMinimum(AnnotationPreference.SIDEBAR_SIZE_MIN);
            sidebarSizeField.setMaximum(AnnotationPreference.SIDEBAR_SIZE_MAX);
            add(sidebarSizeField);

            fontSizeField = new NumberTextField<>("fontSize");
            fontSizeField.setType(Integer.class);
            fontSizeField.setMinimum(AnnotationPreference.FONT_SIZE_MIN);
            fontSizeField.setMaximum(AnnotationPreference.FONT_SIZE_MAX);
            add(fontSizeField);

            curationWindowSizeField = new NumberTextField<>("curationWindowSize");
            curationWindowSizeField.setType(Integer.class);
            curationWindowSizeField.setMinimum(1);
            if (!bModel.getMode().equals(Mode.CURATION)) {
                curationWindowSizeField.setEnabled(false);
            }
           // add(curationWindowSizeField);

            List<Pair<String, String>> editorChoices = annotationEditorRegistry.getEditorFactories()
                    .stream().map(f -> Pair.of(f.getBeanName(), f.getDisplayName())).collect(Collectors.toList());
            
            add(new DropDownChoice<Pair<String, String>>("editor", editorChoices,
                    new ChoiceRenderer<>("value"))
            {
                private static final long serialVersionUID = 1L;

                @Override
                protected void onConfigure()
                {
                    setVisible(getChoices().size() > 1);
                }
            });

            add(new CheckBoxMultipleChoice<AnnotationLayer>("annotationLayers")
            {
                private static final long serialVersionUID = 1L;

                {
                    setSuffix("<br>");
                    setChoices(new LoadableDetachableModel<List<AnnotationLayer>>()
                    {
                        private static final long serialVersionUID = 1L;

                        @Override
                        protected List<AnnotationLayer> load()
                        {
                            // disable corefernce annotation for correction/curation pages for 0.4.0
                            // release
                            List<AnnotationLayer> layers = annotationService
                                    .listAnnotationLayer(bModel.getProject());
                            List<AnnotationLayer> corefTagSets = new ArrayList<>();
                            List<AnnotationLayer> hideLayer = new ArrayList<>();
                            for (AnnotationLayer layer : layers) {
                                if (!layer.isEnabled()) {
                                    hideLayer.add(layer);
                                    continue;
                                }
                                if (layer.getName().equals(Token.class.getName())) {
                                    hideLayer.add(layer);
                                }
                                else if (layer.getType().equals(WebAnnoConst.CHAIN_TYPE)) {
                                    corefTagSets.add(layer);
                                }
                            }

                            if (bModel.getMode().equals(Mode.CORRECTION)
                                    || bModel.getMode().equals(Mode.CURATION)) {
                                layers.removeAll(corefTagSets);
                            }
                            layers.removeAll(hideLayer);
                            return layers;
                        }
                    });
                    setChoiceRenderer(new ChoiceRenderer<>("uiName", "id"));
                }
            });

            // Add a Checkbox to enable/disable automatic page navigations while annotating
            add(new CheckBox("scrollPage"));
            
            add(new CheckBox("rememberLayer"));

            add(new CheckBox("staticColor"));

            add(new Label("scrollPageLabel", "Auto-scroll document while annotating :"));

            add(new AjaxSubmitLink("saveButton")
            {
                private static final long serialVersionUID = -755759008587787147L;

                @Override
                protected void onSubmit(AjaxRequestTarget aTarget, Form<?> aForm)
                {
                    bModel.getPreferences().setScrollPage(getModelObject().scrollPage);
                    bModel.getPreferences().setRememberLayer(getModelObject().rememberLayer);
                    bModel.setAnnotationLayers(getModelObject().annotationLayers);
                    bModel.getPreferences().setWindowSize(getModelObject().windowSize);
                    bModel.getPreferences().setSidebarSize(getModelObject().sidebarSize);
                    bModel.getPreferences().setFontSize(getModelObject().fontSize);
                  /*  bModel.getPreferences().setCurationWindowSize(
                            getModelObject().curationWindowSize);*/
                    bModel.getPreferences().setStaticColor(getModelObject().staticColor);
                    bModel.getPreferences().setEditor(getModelObject().editor.getKey());
                    try {
                        PreferencesUtil.savePreference(bModel, projectService);
                        aEditor.loadFeatureEditorModels(aTarget);
                    }
                    catch (IOException | AnnotationException e) {
                        error("Preference file not found");
                    }
                    modalWindow.close(aTarget);                   
                    aTarget.add(aEditor);
                }

                @Override
                protected void onError(AjaxRequestTarget aTarget, Form<?> aForm)
                {

                }
            });

            add(new AjaxLink<Void>("cancelButton")
            {
                private static final long serialVersionUID = 7202600912406469768L;

                @Override
                public void onClick(AjaxRequestTarget aTarget)
                {
                    AnnotationLayerDetailForm.this.detach();
                    onCancel(aTarget);
                    modalWindow.close(aTarget);
                }
            });
        }
    }

    protected void onCancel(AjaxRequestTarget aTarget)
    {
    }

    public static class AnnotationLayerDetailFormModel
        implements Serializable
    {
        private static final long serialVersionUID = -1L;
        public Pair<String, String> editor;
        public Project project;
        public SourceDocument document;
        public int windowSize;
        public int sidebarSize;
        public int fontSize;
        public int curationWindowSize;
        public boolean scrollPage;
        public boolean rememberLayer;
        public boolean staticColor;
        public List<AnnotationLayer> annotationLayers = new ArrayList<>();
    }

    public AnnotationPreferenceModalPanel(String aId, final ModalWindow modalWindow,
            AnnotatorState aBModel, AnnotationDetailEditorPanel aEditor)
    {
        super(aId);
        this.bModel = aBModel;
        tagSelectionForm = new AnnotationLayerDetailForm("tagSelectionForm", modalWindow, aEditor);
        add(tagSelectionForm);
    }
}
