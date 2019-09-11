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
        this.updateParents();
    }

    public void removeParent(Codebook codebook)
    {
        this.allParents.remove(codebook);
        this.updateParents();
    }

    private void updateParents()
    {
        this.parentSelection.setChoices(new ArrayList<>(this.allParents));
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
