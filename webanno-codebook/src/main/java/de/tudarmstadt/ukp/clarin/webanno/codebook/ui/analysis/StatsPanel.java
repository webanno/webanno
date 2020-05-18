/*
 * Copyright 2020
 * Ubiquitous Knowledge Processing (UKP) Lab Technische Universität Darmstadt
 * and  Language Technology Universität Hamburg
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
package de.tudarmstadt.ukp.clarin.webanno.codebook.ui.analysis;

import org.apache.wicket.markup.html.panel.Panel;

public abstract class StatsPanel<T>
    extends Panel
{
    private static final long serialVersionUID = -6863640617466494681L;

    protected T analysisTarget;

    public StatsPanel(String id)
    {
        super(id);
        analysisTarget = null;
    }

    public StatsPanel(String id, T analysisTarget)
    {
        super(id);
        this.analysisTarget = analysisTarget;
    }

    public abstract void update(T analysisTarget);

    public T getAnalysisTarget()
    {
        return analysisTarget;
    }

    public void setAnalysisTarget(T analysisTarget)
    {
        this.analysisTarget = analysisTarget;
    }
}
