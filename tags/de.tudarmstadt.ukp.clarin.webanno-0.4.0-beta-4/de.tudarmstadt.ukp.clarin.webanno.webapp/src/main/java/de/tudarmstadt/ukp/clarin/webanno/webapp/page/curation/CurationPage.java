/*******************************************************************************
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
 ******************************************************************************/
package de.tudarmstadt.ukp.clarin.webanno.webapp.page.curation;

import java.io.FileNotFoundException;
import java.io.IOException;

import javax.persistence.NoResultException;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.uima.UIMAException;
import org.apache.uima.jcas.JCas;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.NumberTextField;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.springframework.beans.BeansException;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.security.core.context.SecurityContextHolder;

import wicket.contrib.input.events.EventType;
import wicket.contrib.input.events.InputBehavior;
import wicket.contrib.input.events.key.KeyType;
import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationService;
import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryService;
import de.tudarmstadt.ukp.clarin.webanno.brat.ApplicationUtils;
import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.AnnotationPreference;
import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.BratAnnotatorModel;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAjaxCasController;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAjaxCasUtil;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.User;
import de.tudarmstadt.ukp.clarin.webanno.webapp.dialog.AnnotationPreferenceModalPanel;
import de.tudarmstadt.ukp.clarin.webanno.webapp.dialog.OpenDocumentModel;
import de.tudarmstadt.ukp.clarin.webanno.webapp.dialog.OpenModalWindowPanel;
import de.tudarmstadt.ukp.clarin.webanno.webapp.dialog.ReCreateMergeCASModalPanel;
import de.tudarmstadt.ukp.clarin.webanno.webapp.dialog.ReMergeCasModel;
import de.tudarmstadt.ukp.clarin.webanno.webapp.dialog.YesNoModalPanel;
import de.tudarmstadt.ukp.clarin.webanno.webapp.page.curation.component.CurationPanel;
import de.tudarmstadt.ukp.clarin.webanno.webapp.page.curation.component.model.CurationBuilder;
import de.tudarmstadt.ukp.clarin.webanno.webapp.page.curation.component.model.CurationContainer;
import de.tudarmstadt.ukp.clarin.webanno.webapp.page.project.SettingsPageBase;
import de.tudarmstadt.ukp.clarin.webanno.webapp.page.welcome.WelcomePage;

/**
 * This is the main class for the curation page. It contains an interface which displays differences
 * between user annotations for a specific document. The interface provides a tool for merging these
 * annotations and storing them as a new annotation.
 *
 * @author Andreas Straninger
 * @author Seid Muhie Yimam
 */
public class CurationPage
    extends SettingsPageBase
{
    private static final long serialVersionUID = 1378872465851908515L;

    @SpringBean(name = "documentRepository")
    private RepositoryService repository;

    @SpringBean(name = "annotationService")
    private AnnotationService annotationService;

    private CurationPanel curationPanel;
    private OpenDocumentModel openDataModel;

    private ReMergeCasModel reMerge;

    private CurationContainer curationContainer;
    private BratAnnotatorModel bratAnnotatorModel;

    public Label numberOfPages;
    private Label documentNameLabel;

    private int sentenceNumber = 1;
    private int totalNumberOfSentence;

    private long currentDocumentId;
    private long currentprojectId;

    // Open the dialog window on first load
    boolean firstLoad = true;

    private NumberTextField<Integer> gotoPageTextField;
    private int gotoPageAddress = -1;

    WebMarkupContainer finish;

    @SuppressWarnings("deprecation")
    public CurationPage()
    {
        openDataModel = new OpenDocumentModel();
        bratAnnotatorModel = new BratAnnotatorModel();
        reMerge = new ReMergeCasModel();

        curationContainer = new CurationContainer();
        curationContainer.setBratAnnotatorModel(bratAnnotatorModel);
        curationPanel = new CurationPanel("curationPanel", curationContainer);
        curationPanel.setOutputMarkupId(true);
        add(curationPanel);

        add(documentNameLabel = (Label) new Label("doumentName", new LoadableDetachableModel()
        {

            private static final long serialVersionUID = 1L;

            @Override
            protected String load()
            {
                String projectName;
                String documentName;
                if (bratAnnotatorModel.getProject() == null) {
                    projectName = "/";
                }
                else {
                    projectName = bratAnnotatorModel.getProject().getName() + "/";
                }
                if (bratAnnotatorModel.getDocument() == null) {
                    documentName = "";
                }
                else {
                    documentName = bratAnnotatorModel.getDocument().getName();
                }
                return projectName + documentName;

            }
        }).setOutputMarkupId(true));

        add(numberOfPages = (Label) new Label("numberOfPages", new LoadableDetachableModel()
        {

            private static final long serialVersionUID = 1L;

            @Override
            protected String load()
            {
                if (bratAnnotatorModel.getDocument() != null) {

                    JCas mergeJCas = null;
                    try {
                        mergeJCas = repository.getCurationDocumentContent(bratAnnotatorModel
                                .getDocument());

                        totalNumberOfSentence = BratAjaxCasUtil.getNumberOfPages(mergeJCas,
                                bratAnnotatorModel.getWindowSize());

                        // If only one page, start displaying from sentence 1
                        if (totalNumberOfSentence == 1) {
                            bratAnnotatorModel.setSentenceAddress(bratAnnotatorModel
                                    .getFirstSentenceAddress());
                        }
                        sentenceNumber = BratAjaxCasUtil.getSentenceNumber(mergeJCas,
                                bratAnnotatorModel.getSentenceAddress());
                        int firstSentenceNumber = sentenceNumber + 1;
                        int lastSentenceNumber;
                        if (firstSentenceNumber + bratAnnotatorModel.getWindowSize() - 1 < totalNumberOfSentence) {
                            lastSentenceNumber = firstSentenceNumber
                                    + bratAnnotatorModel.getWindowSize() - 1;
                        }
                        else {
                            lastSentenceNumber = totalNumberOfSentence;
                        }

                        return "showing " + firstSentenceNumber + "-" + lastSentenceNumber + " of "
                                + totalNumberOfSentence + " sentences";
                    }
                    catch (UIMAException e) {
                        return "";
                    }
                    catch (ClassNotFoundException e) {
                        return "";
                    }
                    catch (IOException e) {
                        return "";
                    }

                }
                else {
                    return "";// no document yet selected
                }

            }
        }).setOutputMarkupId(true));

        final ModalWindow openDocumentsModal;
        add(openDocumentsModal = new ModalWindow("openDocumentsModal"));
        openDocumentsModal.setOutputMarkupId(true);

        openDocumentsModal.setInitialWidth(500);
        openDocumentsModal.setInitialHeight(300);
        openDocumentsModal.setResizable(true);
        openDocumentsModal.setWidthUnit("px");
        openDocumentsModal.setHeightUnit("px");
        openDocumentsModal.setTitle("Open document");

        // Add project and document information at the top
        add(new AjaxLink<Void>("showOpenDocumentModal")
        {
            private static final long serialVersionUID = 7496156015186497496L;

            @Override
            public void onClick(AjaxRequestTarget aTarget)
            {
                openDocumentsModal.setContent(new OpenModalWindowPanel(openDocumentsModal
                        .getContentId(), openDataModel, openDocumentsModal, Mode.CURATION));
                openDocumentsModal.setWindowClosedCallback(new ModalWindow.WindowClosedCallback()
                {
                    private static final long serialVersionUID = -1746088901018629567L;

                    @Override
                    public void onClose(AjaxRequestTarget target)
                    {
                        String username = SecurityContextHolder.getContext().getAuthentication()
                                .getName();

                        User user = repository.getUser(username);
                        // If this source document has at least one annotation document "FINISHED",
                        // and curation not yet
                        // finished on it
                        if (openDataModel.getDocument() != null
                                && repository.existsFinishedAnnotation(openDataModel.getDocument(),
                                        openDataModel.getProject())) {
                            // Update source document state to CURRATION_INPROGRESS, if it was not
                            // ANNOTATION_FINISHED
                            if (!openDataModel.getDocument().getState()
                                    .equals(SourceDocumentState.CURATION_FINISHED)) {
                                openDataModel.getDocument().setState(
                                        SourceDocumentState.CURATION_IN_PROGRESS);
                            }
                            try {
                                repository.createSourceDocument(openDataModel.getDocument(), user);
                            }
                            catch (IOException e) {
                                error("Unable to update source document "
                                        + ExceptionUtils.getRootCauseMessage(e));
                            }

                            // Get settings from preferences, if available
                            // TEST - set window size to 10

                            bratAnnotatorModel.setDocument(openDataModel.getDocument());
                            bratAnnotatorModel.setProject(openDataModel.getProject());

                            try {
                                initBratAnnotatorDataModel();
                            }
                            catch (UIMAException e) {
                                error(ExceptionUtils.getRootCause(e));
                            }
                            catch (ClassNotFoundException e) {
                                error(ExceptionUtils.getRootCause(e));
                            }
                            catch (IOException e) {
                                error(ExceptionUtils.getRootCause(e));
                            }
                            // transform jcas to objects for wicket components
                            CurationBuilder builder = new CurationBuilder(repository);
                            curationContainer = builder.buildCurationContainer(bratAnnotatorModel);
                            curationContainer.setBratAnnotatorModel(bratAnnotatorModel);
                            updatePanel(curationContainer);
                            // target.add(curationPanel) should work!
                            target.add(finish.setOutputMarkupId(true));
                            target.appendJavaScript("Wicket.Window.unloadConfirmation=false;window.location.reload()");

                        }
                        else if (openDataModel.getDocument() == null) {
                            setResponsePage(WelcomePage.class);
                        }
                        else {
                            target.appendJavaScript("alert('Annotation in progress for document ["
                                    + openDataModel.getDocument().getName() + "]')");
                        }
                        target.add(documentNameLabel);
                    }
                });
                openDocumentsModal.show(aTarget);
            }
        });

        // dialog window to select annotation layer preferences
        final ModalWindow annotationLayerSelectionModal;
        add(annotationLayerSelectionModal = new ModalWindow("annotationLayerModal"));
        annotationLayerSelectionModal.setOutputMarkupId(true);
        annotationLayerSelectionModal.setInitialWidth(440);
        annotationLayerSelectionModal.setInitialHeight(250);
        annotationLayerSelectionModal.setResizable(true);
        annotationLayerSelectionModal.setWidthUnit("px");
        annotationLayerSelectionModal.setHeightUnit("px");
        annotationLayerSelectionModal
                .setTitle("Annotation Layer and window size configuration Window");

        add(new AjaxLink<Void>("showannotationLayerModal")
        {
            private static final long serialVersionUID = 7496156015186497496L;

            @Override
            public void onClick(AjaxRequestTarget target)
            {
                if (bratAnnotatorModel.getProject() == null) {
                    target.appendJavaScript("alert('Please open a project first!')");
                }
                else {

                    annotationLayerSelectionModal.setContent(new AnnotationPreferenceModalPanel(
                            annotationLayerSelectionModal.getContentId(),
                            annotationLayerSelectionModal, bratAnnotatorModel));

                    annotationLayerSelectionModal
                            .setWindowClosedCallback(new ModalWindow.WindowClosedCallback()
                            {
                                private static final long serialVersionUID = 1643342179335627082L;

                                @Override
                                public void onClose(AjaxRequestTarget target)
                                {
                                    // transform jcas to objects for wicket components
                                    CurationBuilder builder = new CurationBuilder(repository);
                                    curationContainer = builder
                                            .buildCurationContainer(bratAnnotatorModel);
                                    curationContainer.setBratAnnotatorModel(bratAnnotatorModel);
                                    updatePanel(curationContainer);
                                    target.appendJavaScript("Wicket.Window.unloadConfirmation=false;window.location.reload()");
                                }
                            });
                    annotationLayerSelectionModal.show(target);
                }

            }
        });

        gotoPageTextField = (NumberTextField<Integer>) new NumberTextField<Integer>("gotoPageText",
                new Model<Integer>(10));
        gotoPageTextField.setType(Integer.class);
        add(gotoPageTextField);
        gotoPageTextField.add(new AjaxFormComponentUpdatingBehavior("onchange")
        {

            @Override
            protected void onUpdate(AjaxRequestTarget target)
            {
                JCas mergeJCas = null;
                try {
                    mergeJCas = repository.getCurationDocumentContent(bratAnnotatorModel
                            .getDocument());
                }
                catch (UIMAException e) {
                    error(ExceptionUtils.getRootCause(e));
                }
                catch (ClassNotFoundException e) {
                    error(ExceptionUtils.getRootCause(e));
                }
                catch (IOException e) {
                    error(e.getMessage());
                }
                gotoPageAddress = BratAjaxCasUtil.getSentenceAddress(mergeJCas,
                        gotoPageTextField.getModelObject());

            }
        });

        add(new AjaxLink<Void>("gotoPageLink")
        {
            private static final long serialVersionUID = 7496156015186497496L;

            @Override
            public void onClick(AjaxRequestTarget target)
            {
                if (gotoPageAddress == -2) {
                    target.appendJavaScript("alert('This sentence number is either negative or beyond the last sentence number!')");
                }
                else if (bratAnnotatorModel.getDocument() != null) {

                    if (gotoPageAddress == -1) {
                        // Not Updated, default used
                        JCas mergeJCas = null;
                        try {
                            mergeJCas = repository.getCurationDocumentContent(bratAnnotatorModel
                                    .getDocument());
                        }
                        catch (UIMAException e) {
                            error(ExceptionUtils.getRootCause(e));
                        }
                        catch (ClassNotFoundException e) {
                            error(ExceptionUtils.getRootCause(e));
                        }
                        catch (IOException e) {
                            error(e.getMessage());
                        }
                        gotoPageAddress = BratAjaxCasUtil.getSentenceAddress(mergeJCas, 10);
                    }
                    if (bratAnnotatorModel.getSentenceAddress() != gotoPageAddress) {
                        bratAnnotatorModel.setSentenceAddress(gotoPageAddress);

                        // transform jcas to objects for wicket components
                        CurationBuilder builder = new CurationBuilder(repository);
                        curationContainer = builder.buildCurationContainer(bratAnnotatorModel);
                        curationContainer.setBratAnnotatorModel(bratAnnotatorModel);
                        updatePanel(curationContainer);
                        target.appendJavaScript("Wicket.Window.unloadConfirmation=false;window.location.reload()");
                    }
                    else {
                        target.appendJavaScript("alert('This sentence is on the same page!')");
                    }
                }
                else {
                    target.appendJavaScript("alert('Please open a document first!')");
                }
            }
        });

        finish = new WebMarkupContainer("finishImage");
        finish.add(new AttributeModifier("src", true, new LoadableDetachableModel<String>()
        {
            private static final long serialVersionUID = 1562727305401900776L;

            @Override
            protected String load()
            {

                if (bratAnnotatorModel.getProject() != null
                        && bratAnnotatorModel.getDocument() != null) {
                    if (repository
                            .getSourceDocument(bratAnnotatorModel.getDocument().getName(),
                                    bratAnnotatorModel.getDocument().getProject()).getState()
                            .equals(SourceDocumentState.CURATION_FINISHED)) {
                        return "images/cancel.png";
                    }
                    else {
                        return "images/accept.png";
                    }
                }
                else {
                    return "images/accept.png";
                }

            }
        }));

        final ModalWindow finishCurationModal;
        add(finishCurationModal = new ModalWindow("finishCurationModal"));
        finishCurationModal.setOutputMarkupId(true);

        finishCurationModal.setInitialWidth(400);
        finishCurationModal.setInitialHeight(50);
        finishCurationModal.setResizable(true);
        finishCurationModal.setWidthUnit("px");
        finishCurationModal.setHeightUnit("px");
        finishCurationModal.setTitle("Are you sure you want to finish curating?");

        AjaxLink<Void> showFinishCurationModal;
        add(showFinishCurationModal = new AjaxLink<Void>("showFinishCurationModal")
        {
            private static final long serialVersionUID = 7496156015186497496L;

            @Override
            public void onClick(AjaxRequestTarget target)
            {
                if (repository
                        .getSourceDocument(bratAnnotatorModel.getDocument().getName(),
                                bratAnnotatorModel.getDocument().getProject()).getState()
                        .equals(SourceDocumentState.CURATION_FINISHED)) {
                    target.appendJavaScript("alert('Document already closed!')");
                }
                else {
                    finishCurationModal
                            .setContent(new YesNoModalPanel(finishCurationModal.getContentId(),
                                    bratAnnotatorModel, finishCurationModal, Mode.CURATION));
                    finishCurationModal
                            .setWindowClosedCallback(new ModalWindow.WindowClosedCallback()
                            {
                                private static final long serialVersionUID = -1746088901018629567L;

                                @Override
                                public void onClose(AjaxRequestTarget target)
                                {
                                    target.add(finish.setOutputMarkupId(true));
                                }
                            });
                    finishCurationModal.show(target);
                }
            }
        });

        showFinishCurationModal.add(finish);

        final ModalWindow reCreateMergeCas;
        add(reCreateMergeCas = new ModalWindow("reCreateMergeCasModal"));
        reCreateMergeCas.setOutputMarkupId(true);

        reCreateMergeCas.setInitialWidth(400);
        reCreateMergeCas.setInitialHeight(50);
        reCreateMergeCas.setResizable(true);
        reCreateMergeCas.setWidthUnit("px");
        reCreateMergeCas.setHeightUnit("px");
        reCreateMergeCas
                .setTitle("are you sure? all curation annotations for this document will be lost");

        add(new AjaxLink<Void>("showreCreateMergeCasModal")
        {
            private static final long serialVersionUID = 7496156015186497496L;

            @Override
            public void onClick(AjaxRequestTarget target)
            {
                reCreateMergeCas.setContent(new ReCreateMergeCASModalPanel(reCreateMergeCas
                        .getContentId(), reCreateMergeCas, reMerge));
                reCreateMergeCas.setWindowClosedCallback(new ModalWindow.WindowClosedCallback()
                {

                    private static final long serialVersionUID = 4816615910398625993L;

                    @Override
                    public void onClose(AjaxRequestTarget target)
                    {
                        if (reMerge.isReMerege()) {
                            try {
                                repository.removeCurationDocumentContent(bratAnnotatorModel
                                        .getDocument());
                                initBratAnnotatorDataModel();
                                // transform jcas to objects for wicket components
                                CurationBuilder builder = new CurationBuilder(repository);
                                curationContainer = builder
                                        .buildCurationContainer(bratAnnotatorModel);
                                curationContainer.setBratAnnotatorModel(bratAnnotatorModel);
                                updatePanel(curationContainer);
                                target.appendJavaScript("Wicket.Window.unloadConfirmation=false;window.location.reload()");
                                target.appendJavaScript("alert('remerege finished!')");
                            }
                            catch (IOException e) {
                                error("Unable to delete the serialized Curation CAS object "
                                        + e.getMessage());
                            }
                            catch (UIMAException e) {
                                error(ExceptionUtils.getRootCause(e));
                            }
                            catch (ClassNotFoundException e) {
                                error(ExceptionUtils.getRootCause(e));
                            }

                        }
                    }
                });
                reCreateMergeCas.show(target);
            }
        });
        // Show the next page of this document
        add(new AjaxLink<Void>("showNext")
        {
            private static final long serialVersionUID = 7496156015186497496L;

            /**
             * Get the current beginning sentence address and add on it the size of the display
             * window
             */
            @Override
            public void onClick(AjaxRequestTarget target)
            {
                if (bratAnnotatorModel.getDocument() != null) {
                    JCas mergeJCas = null;
                    try {
                        mergeJCas = repository.getCurationDocumentContent(bratAnnotatorModel
                                .getDocument());
                    }
                    catch (UIMAException e) {
                        error(ExceptionUtils.getRootCause(e));
                    }
                    catch (ClassNotFoundException e) {
                        error(ExceptionUtils.getRootCause(e));
                    }
                    catch (IOException e) {
                        error(ExceptionUtils.getRootCause(e));
                    }
                    int nextSentenceAddress = BratAjaxCasUtil
                            .getNextDisplayWindowSentenceBeginAddress(mergeJCas,
                                    bratAnnotatorModel.getSentenceAddress(),
                                    bratAnnotatorModel.getWindowSize());
                    if (bratAnnotatorModel.getSentenceAddress() != nextSentenceAddress) {
                        bratAnnotatorModel.setSentenceAddress(nextSentenceAddress);

                        // transform jcas to objects for wicket components
                        CurationBuilder builder = new CurationBuilder(repository);
                        curationContainer = builder.buildCurationContainer(bratAnnotatorModel);
                        curationContainer.setBratAnnotatorModel(bratAnnotatorModel);
                        updatePanel(curationContainer);
                        target.appendJavaScript("Wicket.Window.unloadConfirmation=false;window.location.reload()");
                    }

                    else {
                        target.appendJavaScript("alert('This is last page!')");
                    }
                }
                else {
                    target.appendJavaScript("alert('Please open a document first!')");
                }
            }
        }.add(new InputBehavior(new KeyType[] { KeyType.Page_down }, EventType.click)));

        // SHow the previous page of this document
        add(new AjaxLink<Void>("showPrevious")
        {
            private static final long serialVersionUID = 7496156015186497496L;

            @Override
            public void onClick(AjaxRequestTarget target)
            {
                if (bratAnnotatorModel.getDocument() != null) {

                    JCas mergeJCas = null;
                    try {
                        mergeJCas = repository.getCurationDocumentContent(bratAnnotatorModel
                                .getDocument());
                    }
                    catch (UIMAException e) {
                        error(ExceptionUtils.getRootCause(e));
                    }
                    catch (ClassNotFoundException e) {
                        error(ExceptionUtils.getRootCause(e));
                    }
                    catch (IOException e) {
                        error(ExceptionUtils.getRootCause(e));
                    }

                    int previousSentenceAddress = BratAjaxCasUtil
                            .getPreviousDisplayWindowSentenceBeginAddress(mergeJCas,
                                    bratAnnotatorModel.getSentenceAddress(),
                                    bratAnnotatorModel.getWindowSize());
                    if (bratAnnotatorModel.getSentenceAddress() != previousSentenceAddress) {
                        bratAnnotatorModel.setSentenceAddress(previousSentenceAddress);
                        // transform jcas to objects for wicket components
                        CurationBuilder builder = new CurationBuilder(repository);
                        curationContainer = builder.buildCurationContainer(bratAnnotatorModel);
                        curationContainer.setBratAnnotatorModel(bratAnnotatorModel);
                        updatePanel(curationContainer);
                        target.appendJavaScript("Wicket.Window.unloadConfirmation=false;window.location.reload()");
                    }
                    else {
                        target.appendJavaScript("alert('This is First Page!')");
                    }
                }
                else {
                    target.appendJavaScript("alert('Please open a document first!')");
                }
            }
        }.add(new InputBehavior(new KeyType[] { KeyType.Page_up }, EventType.click)));

        add(new AjaxLink<Void>("showFirst")
        {
            private static final long serialVersionUID = 7496156015186497496L;

            @Override
            public void onClick(AjaxRequestTarget target)
            {
                if (bratAnnotatorModel.getDocument() != null) {
                    if (bratAnnotatorModel.getFirstSentenceAddress() != bratAnnotatorModel
                            .getSentenceAddress()) {
                        bratAnnotatorModel.setSentenceAddress(bratAnnotatorModel
                                .getFirstSentenceAddress());

                        // transform jcas to objects for wicket components
                        CurationBuilder builder = new CurationBuilder(repository);
                        curationContainer = builder.buildCurationContainer(bratAnnotatorModel);
                        curationContainer.setBratAnnotatorModel(bratAnnotatorModel);
                        updatePanel(curationContainer);
                        target.appendJavaScript("Wicket.Window.unloadConfirmation=false;window.location.reload()");

                    }
                    else {
                        target.appendJavaScript("alert('This is first page!')");
                    }
                }
                else {
                    target.appendJavaScript("alert('Please open a document first!')");
                }
            }
        }.add(new InputBehavior(new KeyType[] { KeyType.Home }, EventType.click)));

        add(new AjaxLink<Void>("showLast")
        {
            private static final long serialVersionUID = 7496156015186497496L;

            @Override
            public void onClick(AjaxRequestTarget target)
            {
                if (bratAnnotatorModel.getDocument() != null) {
                    JCas mergeJCas = null;
                    try {
                        mergeJCas = repository.getCurationDocumentContent(bratAnnotatorModel
                                .getDocument());
                    }
                    catch (UIMAException e) {
                        error(ExceptionUtils.getRootCause(e));
                    }
                    catch (ClassNotFoundException e) {
                        error(ExceptionUtils.getRootCause(e));
                    }
                    catch (IOException e) {
                        error(ExceptionUtils.getRootCause(e));
                    }
                    int lastDisplayWindowBeginingSentenceAddress = BratAjaxCasUtil
                            .getLastDisplayWindowFirstSentenceAddress(mergeJCas,
                                    bratAnnotatorModel.getWindowSize());
                    if (lastDisplayWindowBeginingSentenceAddress != bratAnnotatorModel
                            .getSentenceAddress()) {
                        bratAnnotatorModel
                                .setSentenceAddress(lastDisplayWindowBeginingSentenceAddress);
                        // transform jcas to objects for wicket components
                        CurationBuilder builder = new CurationBuilder(repository);
                        curationContainer = builder.buildCurationContainer(bratAnnotatorModel);
                        curationContainer.setBratAnnotatorModel(bratAnnotatorModel);
                        updatePanel(curationContainer);
                        target.appendJavaScript("Wicket.Window.unloadConfirmation=false;window.location.reload()");

                    }
                    else {
                        target.appendJavaScript("alert('This is last Page!')");
                    }
                }
                else {
                    target.appendJavaScript("alert('Please open a document first!')");
                }
            }
        }.add(new InputBehavior(new KeyType[] { KeyType.End }, EventType.click)));
    }

    // Update the curation panel.

    private void updatePanel(CurationContainer aCurationContainer)
    {
        // remove old panel, create new one, add it
        remove(curationPanel);
        curationPanel = new CurationPanel("curationPanel", aCurationContainer);
        curationPanel.setOutputMarkupId(true);
        add(curationPanel);
    }

    /**
     * for the first time, open the <b>open document dialog</b>
     */
    @Override
    public void renderHead(IHeaderResponse response)
    {
        String jQueryString = "";
        if (firstLoad) {
            jQueryString += "jQuery('#showOpenDocumentModal').trigger('click');";
            firstLoad = false;
        }
        response.renderOnLoadJavaScript(jQueryString);
    }

    @SuppressWarnings("unchecked")
    private void initBratAnnotatorDataModel()
        throws UIMAException, ClassNotFoundException, IOException
    {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User userLoggedIn = repository.getUser(SecurityContextHolder.getContext()
                .getAuthentication().getName());
        JCas jCas = null;
        try {
            AnnotationDocument logedInUserAnnotationDocument = repository.getAnnotationDocument(
                    bratAnnotatorModel.getDocument(), userLoggedIn);
            jCas = repository.getAnnotationDocumentContent(logedInUserAnnotationDocument);
        }
        catch (UIMAException e) {
            throw e;
        }
        catch (ClassNotFoundException e) {
            throw e;
        }
        // First time the Merge Cas is opened
        catch (IOException e) {
            throw e;
        }
        // Get information to be populated to bratAnnotatorModel from the JCAS of the logged in user
        //
        catch (DataRetrievalFailureException e) {
            BratAjaxCasController controller = new BratAjaxCasController(repository,
                    annotationService);
            jCas = controller.getJCas(bratAnnotatorModel.getDocument(), bratAnnotatorModel
                    .getDocument().getProject(), userLoggedIn);
        }
        catch (NoResultException e) {
            BratAjaxCasController controller = new BratAjaxCasController(repository,
                    annotationService);
            jCas = controller.getJCas(bratAnnotatorModel.getDocument(), bratAnnotatorModel
                    .getDocument().getProject(), userLoggedIn);
        }

        if (bratAnnotatorModel.getSentenceAddress() == -1
                || bratAnnotatorModel.getDocument().getId() != currentDocumentId
                || bratAnnotatorModel.getProject().getId() != currentprojectId) {

            try {
                bratAnnotatorModel
                        .setSentenceAddress(BratAjaxCasUtil.getFirstSenetnceAddress(jCas));
                bratAnnotatorModel.setLastSentenceAddress(BratAjaxCasUtil
                        .getLastSenetnceAddress(jCas));
                bratAnnotatorModel.setFirstSentenceAddress(bratAnnotatorModel.getSentenceAddress());

                AnnotationPreference preference = new AnnotationPreference();
                ApplicationUtils.setAnnotationPreference(preference, username, repository,
                        annotationService, bratAnnotatorModel, Mode.CURATION);
            }
            catch (DataRetrievalFailureException ex) {
                throw ex;
            }
            catch (BeansException e) {
                throw e;
            }
            catch (FileNotFoundException e) {
                throw e;
            }
            catch (IOException e) {
                throw e;
            }
        }
        // This is a Curation Operation, add to the data model a CURATION Mode
        bratAnnotatorModel.setMode(Mode.CURATION);
        bratAnnotatorModel.setUser(userLoggedIn);

        currentprojectId = bratAnnotatorModel.getProject().getId();
        currentDocumentId = bratAnnotatorModel.getDocument().getId();
    }
}
