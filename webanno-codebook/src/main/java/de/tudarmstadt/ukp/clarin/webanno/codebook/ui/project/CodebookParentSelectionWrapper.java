package de.tudarmstadt.ukp.clarin.webanno.codebook.ui.project;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;

import de.tudarmstadt.ukp.clarin.webanno.codebook.model.Codebook;

public class CodebookParentSelectionWrapper
{

    private DropDownChoice<Codebook> parentSelection;
    private Set<Codebook> allParents;

    public CodebookParentSelectionWrapper(String aId, List<Codebook> aParentChoices)
    {
        this.allParents = new HashSet<>(aParentChoices);
        this.parentSelection = new DropDownChoice<>(aId, new ArrayList<>(aParentChoices),
                new ChoiceRenderer<>("uiName"));
    }

    public void addParent(Codebook codebook)
    {
        this.allParents.add(codebook);
        this.updateParentChoicesForCodebook(codebook);
    }

    public void removeParent(Codebook codebook)
    {
        this.allParents.remove(codebook);
        this.updateParentChoicesForCodebook(codebook);
    }

    /* package private */ void updateParentChoicesForCodebook(Codebook currentCodebook)
    {
        List<Codebook> parentChoices = new ArrayList<>(this.allParents);
        parentChoices.remove(currentCodebook);
        this.parentSelection.setChoices(parentChoices);
    }

    public DropDownChoice get()
    {
        return this.parentSelection;
    }
}
