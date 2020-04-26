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
package de.tudarmstadt.ukp.clarin.webanno.ui.annotation.sidebar.docstats;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;

import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

public interface DocStatsFactory
{
    public static final Integer DOC_STATS_MAX_N_GRAM = 3;
    public static final Integer DOC_STATS_CSV_RECORD_SIZE = 2;
    public static final Character DOC_STATS_CSV_DELIMITER = '\t';
    public static final String DOC_STATS_PARENT_DIR = "stats";
    public static final String DOC_STATS_FILE = "stats.tsv";

    DocStats create(Collection<Token> tokens);

    DocStats load(File csvFile) throws IOException;

    DocStats create(CAS cas) throws CASException;

    DocStats create(SourceDocument document) throws IOException, CASException;

    DocStats createOrLoad(SourceDocument document) throws IOException, CASException;
}
