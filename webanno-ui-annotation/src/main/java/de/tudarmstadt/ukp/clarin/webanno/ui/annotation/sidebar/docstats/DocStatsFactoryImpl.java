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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.fit.util.JCasUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.event.DocumentOpenedEvent;
import de.tudarmstadt.ukp.clarin.webanno.api.event.AfterDocumentCreatedEvent;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

@Component
public class DocStatsFactoryImpl
    implements DocStatsFactory
{
    private final DocumentService documentService;

    @Autowired
    public DocStatsFactoryImpl(DocumentService documentService)
    {
        this.documentService = documentService;
    }

    private DocStats create(List<String> tokens)
    {
        List<Map<DocStats.NGram, Integer>> nGramFrequencies = new ArrayList<>();
        // init one map per nGram
        for (int n = 0; n < DOC_STATS_MAX_N_GRAM; n++)
            nGramFrequencies.add(new HashMap<>());

        for (int i = 0; i < tokens.size(); i++) {
            for (int n = 0; n < DOC_STATS_MAX_N_GRAM; n++) {
                if (i + n < tokens.size()) {
                    DocStats.NGram nGram = new DocStats.NGram(tokens.subList(i, i+n+1));
                    nGramFrequencies.get(n).computeIfPresent(nGram, (s, count) -> ++count);
                    nGramFrequencies.get(n).putIfAbsent(nGram, 1);
                }
            }
        }

        // sort the nGrams by frequency and convert them to a single list
        List<List<Pair<DocStats.NGram, Integer>>> sortedNGramFreqs = new ArrayList<>();
        for (int n = 0; n < DOC_STATS_MAX_N_GRAM; n++) {
            sortedNGramFreqs.add(nGramFrequencies.get(n).entrySet().stream()
                    .map(e -> Pair.of(e.getKey(), e.getValue()))
                    .sorted((o1, o2) -> o2.getRight().compareTo(o1.getRight()))
                    .collect(Collectors.toList()));
        }

        return new DocStats(sortedNGramFreqs);
    }

    @Override
    public DocStats create(Collection<Token> tokens)
    {
        return this.create(tokens.stream().map(Token::getText).collect(Collectors.toList()));
    }

    @Override
    public DocStats create(CAS cas) throws CASException
    {
        return this.create(JCasUtil.select(cas.getJCas(), Token.class));
    }

    @Override
    public DocStats create(SourceDocument document) throws IOException, CASException
    {
        return this.create(this.documentService.createOrReadInitialCas(document));
    }

    @Override
    public DocStats createOrLoad(SourceDocument document) throws IOException, CASException
    {
        File docRoot = documentService.getDocumentFolder(document).getParentFile();
        File statsRoot = new File(docRoot, DOC_STATS_PARENT_DIR);
        FileUtils.forceMkdir(statsRoot);
        File statsFile = new File(statsRoot, DOC_STATS_FILE);

        if (statsFile.exists()) return load(statsFile);
        else {
            DocStats stats = this.create(document);

            try (FileWriter writer = new FileWriter(statsFile);
                    CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT
                            .withDelimiter(DOC_STATS_CSV_DELIMITER).withSkipHeaderRecord())) {

                for (int n = 0; n < DOC_STATS_MAX_N_GRAM; n++)
                    for (Pair<DocStats.NGram, Integer> tokenFreq : stats.getSortedFrequencies(n))
                        csvPrinter.printRecord(tokenFreq.getKey().toString(), tokenFreq.getValue());
                csvPrinter.flush();
            }

            return stats;
        }
    }

    @Override
    public DocStats load(File csvFile) throws IOException
    {
        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(csvFile));
        Iterable<CSVRecord> records = CSVFormat.DEFAULT.withDelimiter(DOC_STATS_CSV_DELIMITER)
                .withIgnoreEmptyLines().withSkipHeaderRecord()
                .parse(new InputStreamReader(bis, StandardCharsets.UTF_8));

        List<List<Pair<DocStats.NGram, Integer>>> sortedNGramFreqs = new ArrayList<>();
        for (int n = 0; n < DOC_STATS_MAX_N_GRAM; n++)
            sortedNGramFreqs.add(new ArrayList<>());

        for (CSVRecord nGramFreq : records) {
            if (nGramFreq.size() != DOC_STATS_CSV_RECORD_SIZE)
                throw new IOException("Cannot parse ");
            String[] tokens = nGramFreq.get(0).split(" ");
            sortedNGramFreqs.get(tokens.length - 1)
                    .add(Pair.of(new DocStats.NGram(), Integer.parseInt(nGramFreq.get(1))));
        }

        return new DocStats(sortedNGramFreqs);
    }

    @EventListener
    public void onAfterDocumentCreatedEvent(AfterDocumentCreatedEvent event)
    {
        try {
            this.createOrLoad(event.getDocument());
        }
        catch (IOException | CASException e) {
            // TODO what to throw or do?!
            e.printStackTrace();
        }
    }

    @EventListener
    public void onDocumentOpenedEvent(DocumentOpenedEvent event)
    {
        try {
            this.createOrLoad(event.getDocument());
        }
        catch (IOException | CASException e) {
            // TODO what to throw or do?!
            e.printStackTrace();
        }
    }
}
