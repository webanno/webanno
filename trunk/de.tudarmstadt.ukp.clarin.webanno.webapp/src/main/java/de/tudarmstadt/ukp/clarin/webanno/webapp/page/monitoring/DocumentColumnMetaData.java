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
package de.tudarmstadt.ukp.clarin.webanno.webapp.page.monitoring;

import java.io.IOException;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.DataGridView;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.resource.ContextRelativeResource;
import org.springframework.security.core.context.SecurityContextHolder;

import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryService;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentStateTransition;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentStateTransition;
import de.tudarmstadt.ukp.clarin.webanno.model.User;
import de.tudarmstadt.ukp.clarin.webanno.webapp.page.curation.component.CurationPanel;
import de.tudarmstadt.ukp.clarin.webanno.webapp.support.EmbeddableImage;

/**
 * Build dynamic columns for the user's annotation documents status {@link DataGridView}
 *
 * @author Seid Muhie Yimam
 *
 */
public class DocumentColumnMetaData
    extends AbstractColumn<List<String>>
{

    private RepositoryService projectRepositoryService;
    private static final Log LOG = LogFactory.getLog(DocumentColumnMetaData.class);

    private static final long serialVersionUID = 1L;
    private int columnNumber;

    private Project project;

    public DocumentColumnMetaData(final TableDataProvider prov, final int colNumber,
            Project aProject, RepositoryService aProjectreRepositoryService)
    {
        super(new AbstractReadOnlyModel<String>()
        {
            private static final long serialVersionUID = 1L;

            @Override
            public String getObject()
            {
                return prov.getColNames().get(colNumber);

            }
        });
        columnNumber = colNumber;
        project = aProject;
        projectRepositoryService = aProjectreRepositoryService;
    }

    @Override
    public void populateItem(final Item<ICellPopulator<List<String>>> aCellItem,
            final String componentId, final IModel<List<String>> rowModel)
    {
        int rowNumber = aCellItem.getIndex();
        aCellItem.setOutputMarkupId(true);

        final String value = getCellValue(rowModel.getObject().get(columnNumber)).trim();
        if (rowNumber == 0) {
            aCellItem.add(new Label(componentId, value.substring(value.indexOf(":") + 1)));
        }
        else if (value.substring(0, value.indexOf(":")).equals(CurationPanel.CURATION_USER)) {
            SourceDocument document = projectRepositoryService.getSourceDocument(
                    value.substring(value.indexOf(":") + 1), project);
            SourceDocumentState state = document.getState();
            String iconNameForState;
            // If state is annotation finished or annotation in progress, curation is not yet
            // started
            if (state.equals(SourceDocumentState.ANNOTATION_FINISHED)) {
                iconNameForState = SourceDocumentState.NEW.toString();
            }
            else if (state.equals(SourceDocumentState.ANNOTATION_IN_PROGRESS)) {
                iconNameForState = SourceDocumentState.NEW.toString();
            }
            else {
                iconNameForState = state.toString();
            }
            aCellItem.add(new EmbeddableImage(componentId, new ContextRelativeResource(
                    "/images_small/" + iconNameForState + ".png")));
            aCellItem.add(AttributeModifier.append("class", "centering"));
            aCellItem.add(new AjaxEventBehavior("onclick")
            {
                private static final long serialVersionUID = -4213621740511947285L;

                @Override
                protected void onEvent(AjaxRequestTarget aTarget)
                {
                    SourceDocument document = projectRepositoryService.getSourceDocument(
                            value.substring(value.indexOf(":") + 1), project);
                    String username = SecurityContextHolder.getContext().getAuthentication()
                            .getName();
                    User user = projectRepositoryService.getUser(username);
                    SourceDocumentState state = document.getState();
                    switch (state) {
                    case CURATION_FINISHED:
                        try {
                            changeSourceDocumentState(
                                    document,
                                    user,
                                    SourceDocumentStateTransition.CURATION_FINISHED_TO_CURATION_IN_PROGRESS);
                        }
                        catch (IOException e) {
                            LOG.info(e.getMessage());
                        }
                        break;
                    case CURATION_IN_PROGRESS:
                        try {
                            changeSourceDocumentState(
                                    document,
                                    user,
                                    SourceDocumentStateTransition.CURATION_IN_PROGRESS_TO_CURATION_FINISHED);
                        }
                        catch (IOException e) {
                            LOG.info(e.getMessage());
                        }
                        break;
                    default:
                        aTarget.appendJavaScript("alert('the state can only be changed explicitly by the curator')");
                    }

                    aTarget.add(aCellItem);
                }
            });
        }
        else if (value.substring(0, value.indexOf(":")).equals(MonitoringPage.SOURCE_DOCUMENT)) {
            SourceDocument document = projectRepositoryService.getSourceDocument(
                    value.substring(value.indexOf(":") + 1), project);
            aCellItem.add(new Label(componentId, document.getState().toString()));
            aCellItem.add(AttributeModifier.append("class", "centering"));
            aCellItem.add(new AjaxEventBehavior("onclick")
            {
                private static final long serialVersionUID = -3607394592031059409L;

                @Override
                protected void onEvent(AjaxRequestTarget aTarget)
                {
                    aTarget.appendJavaScript("alert('You cannot change the state of Source Documents')");
                    aTarget.add(aCellItem);
                }
            });
        }
        else {
            SourceDocument document = projectRepositoryService.getSourceDocument(
                    value.substring(value.indexOf(":") + 1), project);
            User user = projectRepositoryService.getUser(value.substring(0, value.indexOf(":")));

            AnnotationDocumentState state;
            if (projectRepositoryService.existsAnnotationDocument(document, user)) {
                AnnotationDocument annoDoc = projectRepositoryService.getAnnotationDocument(
                        document, user);
                state = annoDoc.getState();
            }
            // user didn't even start working on it
            else {
                state = AnnotationDocumentState.NEW;
                AnnotationDocument annotationDocument = new AnnotationDocument();
                annotationDocument.setDocument(document);
                annotationDocument.setName(document.getName());
                annotationDocument.setProject(project);
                annotationDocument.setUser(user);
                annotationDocument.setState(state);
                projectRepositoryService.createAnnotationDocument(annotationDocument);
            }

            aCellItem.add(new EmbeddableImage(componentId, new ContextRelativeResource(
                    "/images_small/" + state.toString() + ".png")));
            aCellItem.add(AttributeModifier.append("class", "centering"));
            aCellItem.add(new AjaxEventBehavior("onclick")
            {
                private static final long serialVersionUID = -5089819284917455111L;

                @Override
                protected void onEvent(AjaxRequestTarget aTarget)
                {
                    SourceDocument document = projectRepositoryService.getSourceDocument(
                            value.substring(value.indexOf(":") + 1), project);
                    User user = projectRepositoryService.getUser(value.substring(0,
                            value.indexOf(":")));

                    AnnotationDocumentState state;
                    if (projectRepositoryService.existsAnnotationDocument(document, user)) {
                        AnnotationDocument annoDoc = projectRepositoryService
                                .getAnnotationDocument(document, user);
                        state = annoDoc.getState();

                        switch (state) {
                        case FINISHED:
                            changeAnnotationDocumentState(
                                    document,
                                    user,
                                    AnnotationDocumentStateTransition.ANNOTATION_FINISHED_TO_ANNOTATION_IN_PROGRESS);
                            break;
                        case IN_PROGRESS:
                            changeAnnotationDocumentState(
                                    document,
                                    user,
                                    AnnotationDocumentStateTransition.ANNOTATION_IN_PROGRESS_TO_ANNOTATION_FINISHED);
                            break;
                        case NEW:
                            changeAnnotationDocumentState(document, user,
                                    AnnotationDocumentStateTransition.NEW_TO_IGNORE);
                            break;
                        case IGNORE:
                            changeAnnotationDocumentState(document, user,
                                    AnnotationDocumentStateTransition.IGNORE_TO_NEW);
                            break;
                        }
                    }
                    // user didn't even start working on it
                    else {
                        AnnotationDocument annotationDocument = new AnnotationDocument();
                        annotationDocument.setDocument(document);
                        annotationDocument.setName(document.getName());
                        annotationDocument.setProject(project);
                        annotationDocument.setUser(user);
                        annotationDocument.setState(AnnotationDocumentStateTransition
                                .transition(AnnotationDocumentStateTransition.NEW_TO_ANNOTATION_IN_PROGRESS));
                        projectRepositoryService.createAnnotationDocument(annotationDocument);

                        try {
                            changeSourceDocumentState(project, document);
                        }
                        catch (IOException e) {
                            LOG.info("Unable to change state of the document");
                        }
                    }
                    aTarget.add(aCellItem);
                }
            });
        }
    }

    /**
     * Helper method to get the cell value for the user-annotation document status as
     * <b>username:documentName</b>
     *
     * @param aValue
     * @return
     */
    private String getCellValue(String aValue)
    {
        // It is the user column, return user name
        if (aValue.startsWith(MonitoringPage.DOCUMENT)) {
            return aValue.substring(aValue.indexOf(MonitoringPage.DOCUMENT));
        }
        // Initialization of the appliaction, no project selected
        else if (project.getId() == 0) {
            return "";
        }
        // It is document column, get the status from the database
        else {

            String username = aValue.substring(0, aValue.indexOf(MonitoringPage.DOCUMENT) - 1);
            String documentName = aValue.substring(aValue.indexOf(MonitoringPage.DOCUMENT)
                    + MonitoringPage.DOCUMENT.length());
            return username + ":" + documentName;
        }
    }

    /**
     * Change the state of the source document when the annotation document is changed.
     *
     * @param aProject
     * @param aSourceDocument
     * @throws IOException
     */
    private void changeSourceDocumentState(Project aProject, SourceDocument aSourceDocument)
        throws IOException
    {

        boolean allAnnotationDocumentStateFinished = true;
        for (AnnotationDocument annotationDocument : projectRepositoryService
                .listAnnotationDocument(aProject, aSourceDocument)) {
            if (!annotationDocument.getState().equals(AnnotationDocumentState.FINISHED)) {
                allAnnotationDocumentStateFinished = false;
                break;
            }
        }
        if (allAnnotationDocumentStateFinished) {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            User user = projectRepositoryService.getUser(username);

            aSourceDocument
                    .setState(SourceDocumentStateTransition
                            .transition(SourceDocumentStateTransition.ANNOTATION_IN_PROGRESS_TO_ANNOTATION_FINISHED));
            projectRepositoryService.createSourceDocument(aSourceDocument, user);
        }
        else {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            User user = projectRepositoryService.getUser(username);

            aSourceDocument
                    .setState(SourceDocumentStateTransition
                            .transition(SourceDocumentStateTransition.ANNOTATION_FINISHED_TO_ANNOTATION_IN_PROGRESS));
            projectRepositoryService.createSourceDocument(aSourceDocument, user);
        }
    }

    /**
     * change the state of an annotation document. used to re-open closed documents
     *
     * @param aSourceDocument
     * @param aUser
     * @param aAnnotationDocumentStateTransition
     */
    private void changeAnnotationDocumentState(SourceDocument aSourceDocument, User aUser,
            AnnotationDocumentStateTransition aAnnotationDocumentStateTransition)
    {

        AnnotationDocument annotationDocument = projectRepositoryService.getAnnotationDocument(
                aSourceDocument, aUser);
        annotationDocument.setState(AnnotationDocumentStateTransition
                .transition(aAnnotationDocumentStateTransition));
        projectRepositoryService.createAnnotationDocument(annotationDocument);
        try {
            changeSourceDocumentState(project, aSourceDocument);
        }
        catch (IOException e) {
            LOG.info("Unable to change state of the document");
        }

    }

    /**
     * change source document state when curation document state is changed.
     *
     * @param aSourceDocument
     * @param aUser
     * @param aSourceDocumentStateTransition
     * @throws IOException
     */
    private void changeSourceDocumentState(SourceDocument aSourceDocument, User aUser,
            SourceDocumentStateTransition aSourceDocumentStateTransition)
        throws IOException
    {
        aSourceDocument.setState(SourceDocumentStateTransition
                .transition(aSourceDocumentStateTransition));
        projectRepositoryService.createSourceDocument(aSourceDocument, aUser);
    }
}