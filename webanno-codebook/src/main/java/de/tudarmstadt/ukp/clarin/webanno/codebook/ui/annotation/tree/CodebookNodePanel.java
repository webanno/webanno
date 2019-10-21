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
package de.tudarmstadt.ukp.clarin.webanno.codebook.ui.annotation.tree;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.codebook.model.Codebook;
import de.tudarmstadt.ukp.clarin.webanno.codebook.model.CodebookFeature;
import de.tudarmstadt.ukp.clarin.webanno.codebook.model.CodebookTag;
import de.tudarmstadt.ukp.clarin.webanno.codebook.service.CodebookSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.codebook.ui.annotation.CodebookEditorPanel;
import de.tudarmstadt.ukp.clarin.webanno.codebook.ui.annotation.CodebookTagSelectionComboBox;
import de.tudarmstadt.ukp.clarin.webanno.support.DescriptionTooltipBehavior;

public class CodebookNodePanel
    extends Panel
{
    private static final long serialVersionUID = 5875644822389693657L;

    private DropDownChoice<CodebookTag> tagSelection;
    private CodebookTagSelectionComboBox tagSelectionComboBox;
    private @SpringBean CodebookSchemaService codebookService;

    public CodebookNodePanel(String id, IModel<CodebookNode> node, CodebookEditorPanel parentEditor)
    {
        super(id, new CompoundPropertyModel<>(node));

        // heading
        this.add(new Label("codebookNameLabel", node.getObject().getUiName()));

        // form
        IModel<CodebookTag> selectedTag = Model.of();
        Form<CodebookTag> tagSelectionForm = new Form<>("codebookTagSelectionForm",
                CompoundPropertyModel.of(selectedTag));
        tagSelectionForm.setOutputMarkupId(true);

        // combobox
        Codebook book = node.getObject().getCodebook();
        List<CodebookTag> tags = this.getTags(book);
        String existingCode = parentEditor.getExistingCode(book);
        tagSelectionComboBox = new CodebookTagSelectionComboBox("codebookTagBox",
                new Model<>(existingCode), tags);

        // update (persist) and tooltip behaviour for the combobox
        Codebook codebook = node.getObject().getCodebook();
        CodebookFeature feature = codebookService.listCodebookFeature(codebook).get(0);
        AjaxFormComponentUpdatingBehavior updatingBehavior = parentEditor
                .createOnChangeSaveUpdatingBehavior(tagSelectionComboBox, codebook, feature);
        tagSelectionComboBox.add(updatingBehavior);
        tagSelectionForm.add(tagSelectionComboBox);

        // label for the combobox
        tagSelectionForm.add(new Label("codebookTagLabel", "Annotation"));

        // tooltip for the codebooks
        this.add(new DescriptionTooltipBehavior(codebook.getUiName(), codebook.getDescription()));

        this.add(tagSelectionForm);
    }

    private List<CodebookTag> getTags(Codebook aCodebook)
    {
        if (codebookService.listCodebookFeature(aCodebook) == null
                || codebookService.listCodebookFeature(aCodebook).size() == 0) {
            return new ArrayList<>();
        }
        // TODO only get(0) because there is only one feature at the moment!
        CodebookFeature codebookFeature = codebookService.listCodebookFeature(aCodebook).get(0);
        if (codebookFeature.getCategory() == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(codebookService.listTags(codebookFeature.getCategory()));
    }
}
