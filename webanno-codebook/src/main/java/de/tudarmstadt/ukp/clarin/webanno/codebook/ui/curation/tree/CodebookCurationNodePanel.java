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
package de.tudarmstadt.ukp.clarin.webanno.codebook.ui.curation.tree;

import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.getAddr;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.springframework.context.event.EventListener;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.AnnotationException;
import de.tudarmstadt.ukp.clarin.webanno.api.event.DocumentStateChangedEvent;
import de.tudarmstadt.ukp.clarin.webanno.codebook.adapter.CodebookAdapter;
import de.tudarmstadt.ukp.clarin.webanno.codebook.model.Codebook;
import de.tudarmstadt.ukp.clarin.webanno.codebook.model.CodebookFeature;
import de.tudarmstadt.ukp.clarin.webanno.codebook.model.CodebookNode;
import de.tudarmstadt.ukp.clarin.webanno.codebook.model.CodebookTag;
import de.tudarmstadt.ukp.clarin.webanno.codebook.service.CodebookSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.codebook.ui.curation.CodebookUserSuggestion;
import de.tudarmstadt.ukp.clarin.webanno.curation.storage.CurationDocumentService;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.support.DescriptionTooltipBehavior;

public class CodebookCurationNodePanel
    extends Panel
{
    private static final long serialVersionUID = 5875644822389693657L;
    private static final String HAS_DIFF = "panel-danger";
    private static final String HAS_NO_DIFF = "panel-success";
    private static final String HAS_DIFF_FOOTER = "bg-danger";
    private static final String HAS_NO_DIFF_FOOTER = "bg-success";
    private @SpringBean CodebookSchemaService codebookService;
    private @SpringBean CurationDocumentService curationDocumentService;
    private CodebookCurationComboBox codebookCurationComboBox;
    private CodebookCurationTreePanel parentTreePanel;
    private Form<CodebookTag> codebookCurationForm;
    private CodebookNode node;
    private List<CodebookUserSuggestion> codebookUserSuggestions;
    private WebMarkupContainer codebookCurationPanelFooter;
    private WebMarkupContainer codebookCurationPanel;

    public CodebookCurationNodePanel(String id, IModel<CodebookNode> node,
            CodebookCurationTreePanel parentTreePanel,
            List<CodebookUserSuggestion> codebookUserSuggestions)
    {
        super(id, new CompoundPropertyModel<>(node));

        this.node = node.getObject();
        this.parentTreePanel = parentTreePanel;
        this.codebookUserSuggestions = codebookUserSuggestions;

        this.codebookCurationPanel = new WebMarkupContainer("codebookCurationPanel");
        this.codebookCurationPanel.setOutputMarkupPlaceholderTag(true);
        boolean hasDiff = codebookUserSuggestions.get(0).hasDiff();
        this.codebookCurationPanel
                .add(AttributeModifier.append("class", hasDiff ? HAS_DIFF : HAS_NO_DIFF));

        // name of the CB
        this.codebookCurationPanel.add(new Label("codebookName", this.node.getUiName()));

        // suggestions list view
        this.codebookCurationPanel.add(
                new ListView<CodebookUserSuggestion>("suggestionsListView", codebookUserSuggestions)
                {
                    private static final long serialVersionUID = -3459331980449938289L;

                    @Override
                    protected void populateItem(ListItem item)
                    {
                        CodebookUserSuggestion userSuggestion = (CodebookUserSuggestion) item
                                .getModelObject();

                        item.add(new Label("userName", userSuggestion.getUser()));
                        item.add(new Label("codebookTag", userSuggestion.getValue()));
                    }
                });

        this.codebookCurationPanelFooter = new WebMarkupContainer("codebookCurationPanelFooter");
        this.codebookCurationPanelFooter.setOutputMarkupPlaceholderTag(true);
        this.codebookCurationPanelFooter.add(
                AttributeModifier.append("class", hasDiff ? HAS_DIFF_FOOTER : HAS_NO_DIFF_FOOTER));

        // codebook curation form
        IModel<CodebookTag> selectedTag = Model.of();
        this.codebookCurationForm = new Form<>("codebookCurationForm",
                CompoundPropertyModel.of(selectedTag));
        this.codebookCurationForm.setOutputMarkupId(true);
        this.codebookCurationForm.add(
                AttributeModifier.append("class", hasDiff ? HAS_DIFF_FOOTER : HAS_NO_DIFF_FOOTER));

        // codebook curation ComboBox
        this.codebookCurationComboBox = createCurationComboBox();
        this.codebookCurationForm.addOrReplace(this.codebookCurationComboBox);

        // tooltip for the codebooks
        Codebook codebook = this.node.getCodebook();
        this.codebookCurationPanel.add(
                new DescriptionTooltipBehavior(codebook.getUiName(), codebook.getDescription()));

        this.codebookCurationPanelFooter.add(this.codebookCurationForm);

        this.codebookCurationPanel.add(codebookCurationPanelFooter);
        this.add(codebookCurationPanel);
    }

    private CodebookCurationComboBox createCurationComboBox()
    {
        CAS curationCas = null;
        try {
            curationCas = curationDocumentService
                    .readCurationCas(codebookUserSuggestions.get(0).getDocument());
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        Codebook codebook = this.node.getCodebook();
        CodebookFeature feature = codebookService.listCodebookFeature(codebook).get(0);
        CodebookAdapter adapter = new CodebookAdapter(feature.getCodebook());

        String existingValue = (String) adapter.getExistingCodeValue(curationCas, feature);
        List<CodebookTag> tagChoices = this.getPossibleTagChoices();
        CodebookCurationComboBox codebookCurationComboBox = new CodebookCurationComboBox(this,
                "codebookCurationComboBox", Model.of(existingValue), tagChoices);
        // only enable if curation in progress
        codebookCurationComboBox.setEnabled(codebookUserSuggestions.get(0).getDocument().getState()
                .equals(SourceDocumentState.CURATION_IN_PROGRESS));

        codebookCurationComboBox.add(new AjaxFormComponentUpdatingBehavior("change")
        {
            @Override
            protected void onUpdate(AjaxRequestTarget target)
            {
                // persist changes in curation cas
                try {
                    CAS curationCas = curationDocumentService
                            .readCurationCas(codebookUserSuggestions.get(0).getDocument());

                    if (codebookCurationComboBox.getModelObject() == null) {
                        // combo box got cleared or NONE was selected
                        CodebookAdapter adapter = new CodebookAdapter(codebook);
                        adapter.delete(curationCas, feature);
                        writeCodebookCas(curationCas);
                    }
                    else {
                        saveCodebookAnnotation(feature, codebookCurationComboBox.getModelObject(),
                                curationCas);
                    }
                }
                catch (IOException | AnnotationException e) {
                    error("Unable to update" + e.getMessage());
                }
            }
        });

        codebookCurationComboBox.setOutputMarkupId(true);
        return codebookCurationComboBox;
    }

    private void saveCodebookAnnotation(CodebookFeature feature, String value, CAS curationCas)
        throws AnnotationException, IOException
    {

        CodebookAdapter adapter = new CodebookAdapter(feature.getCodebook());
        if (value == null) {
            adapter.delete(curationCas, feature);
            writeCodebookCas(curationCas);
            return;
        }

        writeCodebookToCas(adapter, feature, value, curationCas);

        // persist changes
        writeCodebookCas(curationCas);

    }

    private void writeCodebookToCas(CodebookAdapter aAdapter, CodebookFeature feature, String value,
            CAS aJCas)
        throws IOException, AnnotationException
    {

        AnnotationFS existingFs = aAdapter.getExistingFs(aJCas);
        int annoId;

        if (existingFs != null) {
            annoId = getAddr(existingFs);
        }
        else {
            annoId = aAdapter.add(aJCas);
        }
        aAdapter.setFeatureValue(aJCas, feature, annoId, value);
    }

    private void writeCodebookCas(CAS aCas) throws IOException
    {
        SourceDocument document = this.codebookUserSuggestions.get(0).getDocument();
        curationDocumentService.writeCurationCas(aCas, document, true);

        // Update timestamp in state
        Optional<Long> diskTimestamp = curationDocumentService.getCurationCasTimestamp(document);
        diskTimestamp.ifPresent(aLong -> parentTreePanel.getParentPage().getModelObject()
                .setAnnotationDocumentTimestamp(aLong));
    }

    private List<CodebookTag> getPossibleTagChoices()
    {
        // get the possible tag choices for the current node
        CodebookCurationNodePanel parentPanel = this.parentTreePanel.getNodePanels()
                .get(this.node.getParent());
        if (parentPanel == null)
            return codebookService.listTags(this.node.getCodebook());
        // TODO also check parents of parent
        CodebookTag parentTag = parentPanel.getCurrentlySelectedTag();
        if (parentTag == null) // TODO why is this null for leafs ?!?!?!?
            return codebookService.listTags(this.node.getCodebook());

        // only tags that have parentTag as parent
        List<CodebookTag> validTags = codebookService.listTags(this.node.getCodebook()).stream()
                .filter(codebookTag -> {
                    if (codebookTag.getParent() == null)
                        return false;
                    return codebookTag.getParent().equals(parentTag);
                }).collect(Collectors.toList());
        return validTags;
    }

    public CodebookTag getCurrentlySelectedTag()
    {
        String tagString = this.codebookCurationComboBox.getModelObject();
        if (tagString == null || tagString.isEmpty())
            return null;
        List<CodebookTag> tags = codebookService.listTags(this.node.getCodebook());
        Set<CodebookTag> tag = tags.stream().filter(t -> {
          return  t.getName().equals(tagString);
            
        })
                .collect(Collectors.toSet());
        assert tag.size() == 1; // TODO what to throw?
        if (tag.size() == 0) {
            return null;
        }
        return tag.iterator().next();
    }

    public CodebookNode getNode()
    {
        return node;
    }

    @EventListener
    public void onDocumentStateChangedEvent(DocumentStateChangedEvent changedEvent)
    {
        // TODO how to update the combo boxes without an AjaxRequestTarget?!
        if (changedEvent.getNewState().equals(SourceDocumentState.CURATION_FINISHED)) {
            this.codebookCurationComboBox.setEnabled(false);
        }
    }
}
