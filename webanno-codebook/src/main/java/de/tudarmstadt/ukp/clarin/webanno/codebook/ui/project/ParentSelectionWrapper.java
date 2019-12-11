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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;

public class ParentSelectionWrapper<T>
    implements Serializable
{

    private static final long serialVersionUID = 7597507351396638287L;
    private DropDownChoice<T> parentSelection;
    private Set<T> allParentTags;

    /**
     * @param aId
     *            wicked markup Id for the dropdown
     * @param displayExpression
     *            name of field that's displayed in the dropdown (e.g. name or uiName)
     * @param aParentChoices
     *            the list of possible parents
     */
    public ParentSelectionWrapper(String aId, String displayExpression,
            Collection<T> aParentChoices)
    {
        this.allParentTags = new HashSet<>(aParentChoices);
        this.parentSelection = new DropDownChoice<>(aId, new ArrayList<>(aParentChoices),
                new ChoiceRenderer<>(displayExpression));
        this.parentSelection.setNullValid(true);
    }

    public void addParent(T parent)
    {
        this.allParentTags.add(parent);
        this.updateParents();
    }

    public void removeParent(T parent)
    {
        this.allParentTags.remove(parent);
        this.updateParents();
    }

    private void updateParents()
    {
        this.parentSelection.setChoices(new ArrayList<>(this.allParentTags));
    }

    public void updateParents(Collection<T> parentChoices)
    {
        this.allParentTags = new HashSet<>(parentChoices);
        this.updateParents();
    }

    /* package private */ void removeFromParentChoices(T parentToRemove)
    {
        List<T> parentChoices = new ArrayList<>(this.allParentTags);
        parentChoices.remove(parentToRemove);
        this.parentSelection.setChoices(parentChoices);
    }

    public DropDownChoice getDropdown()
    {
        return this.parentSelection;
    }
}
