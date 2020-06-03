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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.uima.cas.CAS;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.codebook.adapter.CodebookAdapter;
import de.tudarmstadt.ukp.clarin.webanno.codebook.model.Codebook;
import de.tudarmstadt.ukp.clarin.webanno.codebook.model.CodebookFeature;
import de.tudarmstadt.ukp.clarin.webanno.codebook.model.CodebookTag;
import de.tudarmstadt.ukp.clarin.webanno.codebook.service.CodebookSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;

@Component
public class CodebookStatsFactoryImpl
    implements CodebookStatsFactory
{

    private final CodebookSchemaService codebookSchemaService;
    private final DocumentService documentService;
    private final UserDao userRepository;
    private final ProjectService projectService;

    @Autowired
    public CodebookStatsFactoryImpl(CodebookSchemaService codebookSchemaService,
            DocumentService documentService, UserDao userRepository, ProjectService projectService)
    {
        this.codebookSchemaService = codebookSchemaService;
        this.documentService = documentService;
        this.userRepository = userRepository;
        this.projectService = projectService;
    }

    @Override
    public CodebookStats create(Project project, boolean annotators, boolean curators)
    {
        return this.create(documentService.listSourceDocuments(project), annotators, curators);
    }

    @Override
    public CodebookStats create(List<SourceDocument> docs, boolean annotators, boolean curators)
    {
        // make sure that each of the provided docs is from the same project (i.e. shares the same
        // codebooks)
        Project project = docs.get(0).getProject();
        docs.forEach(doc -> {
            assert doc.getProject().equals(project);
        });

        // get all cases (of all users) of all SourceDocuments
        List<CAS> CASes = this.loadCASes(docs, annotators, curators);

        // get all codebooks
        List<Codebook> codebooks = codebookSchemaService.listCodebook(project);

        // get the annotated Tags for each of the codebooks
        Map<Codebook, Map<CodebookTag, Integer>> suggestions = new HashMap<>();
        for (Codebook codebook : codebooks) {
            for (CAS cas : CASes) {
                CodebookAdapter adapter = new CodebookAdapter(codebook);

                // there is only 1 dummy feature at the moment!
                CodebookFeature feature = codebookSchemaService.listCodebookFeature(codebook)
                        .get(0);

                String tagName = (String) adapter.getExistingCodeValue(cas, feature);
                CodebookTag tag = null;
                if (tagName != null && !tagName.isEmpty()
                        && codebookSchemaService.existsCodebookTag(tagName, codebook))
                    tag = codebookSchemaService.getCodebookTag(tagName, codebook);

                suggestions.putIfAbsent(codebook, new HashMap<>());
                suggestions.get(codebook).computeIfPresent(tag, (t, cnt) -> ++cnt);
                suggestions.get(codebook).putIfAbsent(tag, 1);
            }
        }

        return new CodebookStats(this.sort(suggestions));
    }

    private Map<Codebook, List<Pair<CodebookTag, Integer>>> sort(
            Map<Codebook, Map<CodebookTag, Integer>> suggestions)
    {
        Map<Codebook, List<Pair<CodebookTag, Integer>>> sorted = new HashMap<>();

        suggestions.forEach((codebook, tagCountMap) -> {
            sorted.put(codebook,
                    tagCountMap.entrySet().stream().map(e -> Pair.of(e.getKey(), e.getValue()))
                            .sorted((o1, o2) -> o2.getRight().compareTo(o1.getRight()))
                            .collect(Collectors.toList()));
        });
        return sorted;
    }

    private List<CAS> loadCASes(List<SourceDocument> docs, boolean annotators, boolean curators)
    {
        List<CAS> CASes = new ArrayList<>();
        for (SourceDocument doc : docs)
            // get all annotation documents of all users of the current doc
            documentService.listAnnotationDocuments(doc).forEach(annotationDocument -> {
                try {
                    User user = userRepository.get(annotationDocument.getUser());
                    Project project = annotationDocument.getProject();
                    boolean isCurator = projectService.isCurator(project, user);
                    boolean isAnnotator = projectService.isAnnotator(project, user);

                    if ((curators && isCurator) || (annotators && isAnnotator))
                        CASes.add(documentService.readAnnotationCas(annotationDocument));
                }
                catch (IOException e) {
                    // TODO what to do?!
                    e.printStackTrace();
                }
            });

        return CASes;
    }
}
