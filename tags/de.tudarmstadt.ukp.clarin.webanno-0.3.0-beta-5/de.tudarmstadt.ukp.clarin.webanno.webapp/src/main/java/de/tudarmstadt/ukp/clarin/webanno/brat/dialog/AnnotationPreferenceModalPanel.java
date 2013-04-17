/*******************************************************************************
 * Copyright 2012
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package de.tudarmstadt.ukp.clarin.webanno.brat.dialog;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.CheckBoxMultipleChoice;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.NumberTextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.springframework.security.core.context.SecurityContextHolder;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationService;
import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryService;
import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.AnnotationPreference;
import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.BratAnnotator;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.Subject;
import de.tudarmstadt.ukp.clarin.webanno.model.TagSet;

/**
 * Modal Window to configure {@link BratAnnotator#setAnnotationLayers(ArrayList), BratAnnotator#setWindowSize(int)...}
 * @author Seid Muhie Yimam
 * @author Richard Eckart de Castilho
 *
 */
public class AnnotationPreferenceModalPanel
    extends Panel
{
    private static final long serialVersionUID = -2102136855109258306L;

    private static final Log LOG = LogFactory.getLog(AnnotationPreferenceModalPanel.class);

    @SpringBean(name = "annotationService")
    private AnnotationService annotationService;

    @SpringBean(name = "documentRepository")
    private RepositoryService projectRepository;

    private AnnotationLayerDetailForm tagSelectionForm;

    private CheckBoxMultipleChoice<TagSet> tagSets;
    private NumberTextField<Integer> windowSizeField;

    private BratAnnotator annotator;

    private class AnnotationLayerDetailForm
        extends Form<AnnotationLayerDetailFormModel>
    {
        private static final long serialVersionUID = -683824912741426241L;

        @SuppressWarnings({ "unchecked", "rawtypes" })
        public AnnotationLayerDetailForm(String id, final ModalWindow modalWindow)
        {
            super(id, new CompoundPropertyModel<AnnotationLayerDetailFormModel>(
                    new AnnotationLayerDetailFormModel()));

            // Import current settings from the annotator
            getModelObject().numberOfSentences = annotator.bratAnnotatorModel.getWindowSize();
            getModelObject().scrollPage = annotator.bratAnnotatorModel.isScrollPage();
            getModelObject().displayLemma = annotator.bratAnnotatorModel.isDisplayLemmaSelected();

            for (TagSet tagSet : annotator.bratAnnotatorModel.getAnnotationLayers()) {
                getModelObject().annotationLayers.add(tagSet);
            }
            windowSizeField = (NumberTextField<Integer>) new NumberTextField<Integer>(
                    "numberOfSentences");
            windowSizeField.setType(Integer.class);
            windowSizeField.setMinimum(1);
            add(windowSizeField);
            add(new CheckBox("displayLemma"));

            add(tagSets = (CheckBoxMultipleChoice<TagSet>) new CheckBoxMultipleChoice<TagSet>(
                    "annotationLayers")
            {
                private static final long serialVersionUID = 1L;

                {
                    setChoices(new LoadableDetachableModel<List<TagSet>>()
                    {
                        private static final long serialVersionUID = 1L;

                        @Override
                        protected List<TagSet> load()
                        {
                            return annotationService.listTagSets(annotator.bratAnnotatorModel.getProject());
                        }
                    });
                    setChoiceRenderer(new ChoiceRenderer<TagSet>("name", "id"));
                }
            });
            // Add a Checkbox to enable/disable automatic page navigations while annotating
            add(new CheckBox("scrollPage"));

            add(new AjaxSubmitLink("saveButton")
            {
                private static final long serialVersionUID = -755759008587787147L;

                @Override
                protected void onSubmit(AjaxRequestTarget aTarget, Form<?> aForm)
                {
                    AnnotationPreference preference = new AnnotationPreference();
                    preference.setDisplayLemmaSelected(getModelObject().displayLemma);
                    preference.setScrollPage(getModelObject().scrollPage);
                    preference.setWindowSize(getModelObject().numberOfSentences);

                    ArrayList<Long> layers = new ArrayList<Long>();

                    for (TagSet tagset : getModelObject().annotationLayers) {
                        layers.add(tagset.getId());
                    }
                    preference.setAnnotationLayers(layers);
                    String username = SecurityContextHolder.getContext().getAuthentication()
                            .getName();
                    try {
                        projectRepository.saveUserSettings(username, annotator.bratAnnotatorModel.getProject(),
                                Subject.annotation, preference);
                    }
                    catch (FileNotFoundException e) {
                        error("Unable to save preferences in a property file: "
                                + ExceptionUtils.getRootCauseMessage(e));
                    }
                    catch (IOException e) {
                        error("Unable to save preferences in a property file: "
                                + ExceptionUtils.getRootCauseMessage(e));
                    }

                    annotator.bratAnnotatorModel.setDisplayLemmaSelected(getModelObject().displayLemma);
                    annotator.bratAnnotatorModel.setScrollPage(getModelObject().scrollPage);
                    annotator.bratAnnotatorModel.setAnnotationLayers(getModelObject().annotationLayers);
                    annotator.bratAnnotatorModel.setWindowSize(getModelObject().numberOfSentences);
                    aTarget.add(annotator);
                    modalWindow.close(aTarget);
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
                public void onClick(AjaxRequestTarget target)
                {
                    AnnotationLayerDetailForm.this.detach();
                    modalWindow.close(target);
                }
            });
        }
    }

    private static class AnnotationLayerDetailFormModel
        implements Serializable
    {
        private static final long serialVersionUID = -1L;
        public Project project;
        public SourceDocument document;
        public int numberOfSentences;
        public boolean displayLemma;
        public boolean scrollPage;
        public boolean reverseDependencyDirection;
        public HashSet<TagSet> annotationLayers = new HashSet<TagSet>();
    }

    public AnnotationPreferenceModalPanel(String aId, final ModalWindow modalWindow,
            BratAnnotator aAnnotator)
    {
        super(aId);
        this.annotator = aAnnotator;
        tagSelectionForm = new AnnotationLayerDetailForm("tagSelectionForm", modalWindow);
        add(tagSelectionForm);
    }

}
