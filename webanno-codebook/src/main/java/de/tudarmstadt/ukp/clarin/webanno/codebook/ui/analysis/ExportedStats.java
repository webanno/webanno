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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.tuple.Pair;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import de.tudarmstadt.ukp.clarin.webanno.codebook.model.CodebookTag;
import de.tudarmstadt.ukp.clarin.webanno.codebook.ui.analysis.codebookstats.CodebookStatsFactory;
import de.tudarmstadt.ukp.clarin.webanno.codebook.ui.analysis.ngramstats.NGram;
import de.tudarmstadt.ukp.clarin.webanno.codebook.ui.analysis.ngramstats.NGramStatsFactory;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ExportedStats
{
    @JsonProperty("analysisTarget")
    private java.lang.String name;

    @JsonProperty("nGrams")
    private Map<String, List<Pair<NGram, Integer>>> nGrams;

    @JsonProperty("codebooks")
    private Map<String, List<Pair<CodebookTag, Integer>>> codebooks;

    public void setName(String name)
    {
        this.name = name;
    }

    public void setNGrams(NGramStatsFactory.NGramStats... nGramStats)
    {
        this.nGrams = new HashMap<>();
        Arrays.stream(nGramStats).forEach(stats -> {
            stats.getSortedFrequencies().forEach((n, pairs) -> {
                String nGrams = (n + 1) + "-Grams";
                this.nGrams.put(nGrams, pairs);
            });
        });
    }

    public void setCodebooks(CodebookStatsFactory.CodebookStats... codebookStats)
    {
        this.codebooks = new HashMap<>();
        Arrays.stream(codebookStats).forEach(stats -> {
            stats.getSortedFrequencies().forEach((codebook, pairs) -> {
                this.codebooks.put(codebook.getName(), pairs);
            });
        });
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
            return true;

        if (o == null || getClass() != o.getClass())
            return false;

        ExportedStats that = (ExportedStats) o;

        return new EqualsBuilder().append(name, that.name).append(nGrams, that.nGrams)
                .append(codebooks, that.codebooks).isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(17, 37).append(name).append(nGrams).append(codebooks)
                .toHashCode();
    }
}
