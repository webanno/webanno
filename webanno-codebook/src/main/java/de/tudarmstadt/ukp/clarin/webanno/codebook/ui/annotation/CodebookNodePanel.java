/*
 * Copyright 2019
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
package de.tudarmstadt.ukp.clarin.webanno.codebook.ui.annotation;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.codebook.model.Codebook;
import de.tudarmstadt.ukp.clarin.webanno.codebook.model.CodebookFeature;
import de.tudarmstadt.ukp.clarin.webanno.codebook.model.CodebookNode;
import de.tudarmstadt.ukp.clarin.webanno.codebook.model.CodebookTag;
import de.tudarmstadt.ukp.clarin.webanno.codebook.service.CodebookSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.support.DescriptionTooltipBehavior;

public class CodebookNodePanel
    extends Panel
{
    private static final long serialVersionUID = 5875644822389693657L;

    private CodebookTagSelectionComboBox tagSelectionComboBox;
    private @SpringBean CodebookSchemaService codebookService;
    private CodebookEditorPanel parentEditor;
    private Form<CodebookTag> tagSelectionForm;
    private CodebookNode node;

    public CodebookNodePanel(String id, IModel<CodebookNode> node, CodebookEditorPanel parentEditor)
    {
        super(id, new CompoundPropertyModel<>(node));

        this.node = node.getObject();
        this.parentEditor = parentEditor;

        // form
        IModel<CodebookTag> selectedTag = Model.of();
        this.tagSelectionForm = new Form<>("codebookTagSelectionForm",
                CompoundPropertyModel.of(selectedTag));
        this.tagSelectionForm.setOutputMarkupId(true);

        // combobox
        this.tagSelectionComboBox = createTagSelectionComboBox();
        this.tagSelectionForm.addOrReplace(this.tagSelectionComboBox);

        // label for the combobox
        this.tagSelectionForm.add(new Label("codebookNameLabel", this.node.getUiName()));

        // tooltip for the codebooks
        Codebook codebook = this.node.getCodebook();
        this.add(new DescriptionTooltipBehavior(codebook.getUiName(), codebook.getDescription()));

        this.add(this.tagSelectionForm);
    }

    private CodebookTagSelectionComboBox createTagSelectionComboBox()
    {

        List<CodebookTag> tagChoices = this.getPossibleTagChoices();
        String existingCode = this.parentEditor.getExistingCode(this.node.getCodebook());
        CodebookTagSelectionComboBox tagSelection = new CodebookTagSelectionComboBox(this,
                "codebookTagBox", new Model<>(existingCode), tagChoices);

        Codebook codebook = this.node.getCodebook();
        CodebookFeature feature = codebookService.listCodebookFeature(codebook).get(0);
        AjaxFormComponentUpdatingBehavior updatingBehavior = parentEditor
                .createOnChangeSaveUpdatingBehavior(tagSelection, codebook, feature);
        tagSelection.add(updatingBehavior);
        tagSelection.setOutputMarkupId(true);
        return tagSelection;
    }

    private List<CodebookTag> getPossibleTagChoices()
    {

        // get the possible tag choices for the current node
        CodebookNodePanel parentPanel = this.parentEditor.getNodePanels()
                .get(this.node.getParent());
        if (parentPanel == null)
            return codebookService.listTags(this.node.getCodebook());
        // TODO also check parents of parent
        CodebookTag parentTag = parentPanel.getCurrentlySelectedTag();
        if (parentTag == null) // TODO why is this null for street ?!?!?!?
            return codebookService.listTags(this.node.getCodebook());

        // only tags that have parentTag as parent
        List<CodebookTag> validTags = codebookService.listTags(this.node.getCodebook()).stream()
                .filter(codebookTag -> {
                    if (codebookTag.getParent() == null)
                        return false;
                    return codebookTag.getParent().equals(parentTag);})
                .collect(Collectors.toList());
        return validTags;
    }

    public CodebookTag getCurrentlySelectedTag()
    {
        String tagString = this.tagSelectionComboBox.getModelObject();
        if (tagString == null || tagString.isEmpty())
            return null;
        List<CodebookTag> tags = codebookService.listTags(this.node.getCodebook());
        Set<CodebookTag> tag = tags.stream().filter(t -> t.getName().equals(tagString))
                .collect(Collectors.toSet());
        assert tag.size() == 1; // TODO what to throw?
        return tag.iterator().next();
    }

    // package private by intention
    void clearSelection()
    {
        this.tagSelectionComboBox.setModelObject(null);
    }

    // package private by intention
    void updateTagSelectionCombobox()
    {
        this.tagSelectionForm.addOrReplace(createTagSelectionComboBox());
    }

    public CodebookSchemaService getCodebookService() {
        return codebookService;
    }

    public CodebookEditorPanel getParentEditor() {
        return parentEditor;
    }

    public CodebookNode getNode() {
        return node;
    }
}
