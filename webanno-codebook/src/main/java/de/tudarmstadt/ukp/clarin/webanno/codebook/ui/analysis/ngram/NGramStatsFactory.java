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
package de.tudarmstadt.ukp.clarin.webanno.codebook.ui.analysis.ngram;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;

import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

public interface NGramStatsFactory
{
    public static final Integer MAX_N_GRAM = 3;
    public static final Integer TSV_RECORD_SIZE = 2;
    public static final Character TSV_DELIMITER = '\t';
    public static final String NGRAM_STATS_PARENT_DIR = "stats";
    public static final String NGRAM_STATS_FILE = "ngrams.tsv";

    NGramStats create(Collection<Token> tokens);

    NGramStats load(File csvFile) throws IOException;

    NGramStats create(CAS cas) throws CASException;

    NGramStats create(SourceDocument document) throws IOException, CASException;

    NGramStats createOrLoad(SourceDocument document) throws IOException, CASException;

    NGramStats merge(List<NGramStats> stats);
}
