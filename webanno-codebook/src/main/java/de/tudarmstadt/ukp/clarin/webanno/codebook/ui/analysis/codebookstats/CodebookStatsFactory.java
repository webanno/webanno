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
package de.tudarmstadt.ukp.clarin.webanno.codebook.ui.analysis.codebookstats;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;

import de.tudarmstadt.ukp.clarin.webanno.codebook.model.Codebook;
import de.tudarmstadt.ukp.clarin.webanno.codebook.model.CodebookTag;
import de.tudarmstadt.ukp.clarin.webanno.codebook.ui.analysis.Stats;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;

public interface CodebookStatsFactory
{

    CodebookStats create(Project project);

    CodebookStats create(SourceDocument... docs);

    CodebookStats create(List<SourceDocument> docs);

    class CodebookStats
        extends Stats<Codebook, CodebookTag>
    {

        private static final long serialVersionUID = 1627935552528272517L;

        public CodebookStats(Map<Codebook, List<Pair<CodebookTag, Integer>>> sortedFrequencies)
        {
            super(sortedFrequencies);
        }
    }

}
