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
public class NGramStatsFactoryImpl
    implements NGramStatsFactory
{
    private final DocumentService documentService;

    @Autowired
    public NGramStatsFactoryImpl(DocumentService documentService)
    {
        this.documentService = documentService;
    }

    private NGramStats create(List<String> tokens)
    {
        List<Map<NGramStats.NGram, Integer>> nGramFrequencies = this
                .createFrequencyMapsFromTokens(tokens);
        List<List<Pair<NGramStats.NGram, Integer>>> sortedNGramFreqs = createSortedList(
                nGramFrequencies);

        return new NGramStats(sortedNGramFreqs);
    }

    private List<Map<NGramStats.NGram, Integer>> createFrequencyMapsFromTokens(List<String> tokens)
    {
        List<Map<NGramStats.NGram, Integer>> nGramFrequencies = new ArrayList<>();
        // init one map per nGram
        for (int n = 0; n < MAX_N_GRAM; n++)
            nGramFrequencies.add(new HashMap<>());

        for (int i = 0; i < tokens.size(); i++) {
            for (int n = 0; n < MAX_N_GRAM; n++) {
                if (i + n < tokens.size()) {
                    NGramStats.NGram nGram = new NGramStats.NGram(
                            // wrap in ArrayList to avoid SerializationException
                            new ArrayList<>(tokens.subList(i, i + n + 1)));
                    nGramFrequencies.get(n).computeIfPresent(nGram, (s, count) -> ++count);
                    nGramFrequencies.get(n).putIfAbsent(nGram, 1);
                }
            }
        }
        return nGramFrequencies;
    }

    private List<Map<NGramStats.NGram, Integer>> createFrequencyMapsFromSortedNGramFrequencies(
            List<List<Pair<NGramStats.NGram, Integer>>> sortedFreqs)
    {
        List<Map<NGramStats.NGram, Integer>> nGramFrequencies = new ArrayList<>();
        // init one map per nGram
        for (int n = 0; n < MAX_N_GRAM; n++)
            nGramFrequencies.add(new HashMap<>());

        for (int i = 0; i < sortedFreqs.size(); i++) {
            for (Pair<NGramStats.NGram, Integer> nGram : sortedFreqs.get(i)) {
                Map<NGramStats.NGram, Integer> nGramMap = nGramFrequencies.get(i);
                nGramMap.computeIfPresent(nGram.getLeft(), (ng, num) -> num += nGram.getRight());
                nGramMap.putIfAbsent(nGram.getLeft(), nGram.getRight());
            }
        }

        return nGramFrequencies;
    }

    private List<List<Pair<NGramStats.NGram, Integer>>> createSortedList(
            List<Map<NGramStats.NGram, Integer>> nGramFrequencies)
    {
        // sort the nGrams by frequency and convert them to a single list
        List<List<Pair<NGramStats.NGram, Integer>>> sortedNGramFreqs = new ArrayList<>();
        for (int n = 0; n < MAX_N_GRAM; n++) {
            sortedNGramFreqs.add(nGramFrequencies.get(n).entrySet().stream()
                    .map(e -> Pair.of(e.getKey(), e.getValue()))
                    .sorted((o1, o2) -> o2.getRight().compareTo(o1.getRight()))
                    .collect(Collectors.toList()));
        }
        return sortedNGramFreqs;
    }

    @Override
    public NGramStats create(Collection<Token> tokens)
    {
        return this.create(tokens.stream().map(Token::getText).collect(Collectors.toList()));
    }

    @Override
    public NGramStats create(CAS cas) throws CASException
    {
        return this.create(JCasUtil.select(cas.getJCas(), Token.class));
    }

    @Override
    public NGramStats create(SourceDocument document) throws IOException, CASException
    {
        return this.create(this.documentService.createOrReadInitialCas(document));
    }

    @Override
    public NGramStats createOrLoad(SourceDocument document) throws IOException, CASException
    {
        File docRoot = documentService.getDocumentFolder(document).getParentFile();
        File statsRoot = new File(docRoot, NGRAM_STATS_PARENT_DIR);
        FileUtils.forceMkdir(statsRoot);
        File statsFile = new File(statsRoot, NGRAM_STATS_FILE);

        if (statsFile.exists())
            return load(statsFile);
        else {
            NGramStats stats = this.create(document);
            // persist stats
            try (FileWriter writer = new FileWriter(statsFile);
                    CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT
                            .withDelimiter(TSV_DELIMITER).withSkipHeaderRecord())) {

                for (int n = 0; n < MAX_N_GRAM; n++)
                    for (Pair<NGramStats.NGram, Integer> tokenFreq : stats.getSortedFrequencies(n))
                        csvPrinter.printRecord(tokenFreq.getKey().toString(), tokenFreq.getValue());
                csvPrinter.flush();
            }

            return stats;
        }
    }

    @Override
    public NGramStats merge(List<NGramStats> toMerge)
    {
        if (toMerge.size() == 1)
            return toMerge.get(0);

        List<Map<NGramStats.NGram, Integer>> mergedFrequencies = new ArrayList<>();
        for (int i = 0; i < MAX_N_GRAM; i++)
            mergedFrequencies.add(new HashMap<>());

        // merge all input NGramStats
        for (NGramStats ngStats : toMerge) {
            // create the freq maps per input
            List<Map<NGramStats.NGram, Integer>> nGramFreqs = this
                    .createFrequencyMapsFromSortedNGramFrequencies(ngStats.getSortedFrequencies());

            // join the maps with the mergedFrequencies
            for (int i = 0; i < MAX_N_GRAM; i++) {
                int finalI = i;
                nGramFreqs.get(i).forEach((nG, cnt) -> {
                    mergedFrequencies.get(finalI).computeIfPresent(nG, (nGram, c) -> c += cnt);
                    mergedFrequencies.get(finalI).putIfAbsent(nG, cnt);
                });
            }
        }

        return new NGramStats(createSortedList(mergedFrequencies));
    }

    @Override
    public NGramStats load(File csvFile) throws IOException
    {
        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(csvFile));
        Iterable<CSVRecord> records = CSVFormat.DEFAULT.withDelimiter(TSV_DELIMITER)
                .withIgnoreEmptyLines().withSkipHeaderRecord()
                .parse(new InputStreamReader(bis, StandardCharsets.UTF_8));

        List<List<Pair<NGramStats.NGram, Integer>>> sortedNGramFreqs = new ArrayList<>();
        for (int n = 0; n < MAX_N_GRAM; n++)
            sortedNGramFreqs.add(new ArrayList<>());

        for (CSVRecord nGramFreq : records) {
            if (nGramFreq.size() != TSV_RECORD_SIZE)
                throw new IOException("Cannot parse ");
            String[] tokens = nGramFreq.get(0).split(" ");
            sortedNGramFreqs.get(tokens.length - 1)
                    .add(Pair.of(new NGramStats.NGram(tokens), Integer.parseInt(nGramFreq.get(1))));
        }

        return new NGramStats(sortedNGramFreqs);
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
