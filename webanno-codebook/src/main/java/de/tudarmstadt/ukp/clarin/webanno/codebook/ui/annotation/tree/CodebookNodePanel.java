package de.tudarmstadt.ukp.clarin.webanno.codebook.ui.annotation.tree;

import de.agilecoders.wicket.core.markup.html.bootstrap.block.LabelBehavior;
import de.tudarmstadt.ukp.clarin.webanno.codebook.model.Codebook;
import de.tudarmstadt.ukp.clarin.webanno.codebook.model.CodebookFeature;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.codebook.model.CodebookTag;
import de.tudarmstadt.ukp.clarin.webanno.codebook.service.CodebookSchemaService;

import java.util.ArrayList;
import java.util.List;

public class CodebookNodePanel
    extends Panel
{
    private static final long serialVersionUID = 5875644822389693657L;

    private DropDownChoice<CodebookTag> parentSelection;
    private @SpringBean CodebookSchemaService codebookService;

    public CodebookNodePanel(String id, IModel<CodebookNode> node)
    {
        super(id, new CompoundPropertyModel<>(node));

        add(new Label("codebookNameLabel", node.getObject().getUiName()));

        IModel<CodebookTag> selectedTag = Model.of();
        Form<CodebookTag> tagSelectionForm = new Form<>("codebookTagSelectionForm", CompoundPropertyModel.of(selectedTag));
        tagSelectionForm.setOutputMarkupId(true);

        Codebook book = node.getObject().getCodebook();
        List<CodebookTag> tags = this.getTags(book);
        parentSelection = new DropDownChoice<>("codebookTag", tags, new ChoiceRenderer<>("name"));

        tagSelectionForm.add(parentSelection);
        tagSelectionForm.add(new Label("codebookTagLabel", "Annotation"));
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