package de.tudarmstadt.ukp.clarin.webanno.codebook.ui.analysis.codebookstats;

import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.CORRECTION_USER;
import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.CURATION_USER;
import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.INITIAL_CAS_PSEUDO_USER;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.uima.cas.CAS;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.codebook.adapter.CodebookAdapter;
import de.tudarmstadt.ukp.clarin.webanno.codebook.model.Codebook;
import de.tudarmstadt.ukp.clarin.webanno.codebook.model.CodebookFeature;
import de.tudarmstadt.ukp.clarin.webanno.codebook.model.CodebookTag;
import de.tudarmstadt.ukp.clarin.webanno.codebook.service.CodebookSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;

@Component
public class CodebookStatsFactoryImpl
    implements CodebookStatsFactory
{

    private final CodebookSchemaService codebookSchemaService;
    private final DocumentService documentService;

    @Autowired
    public CodebookStatsFactoryImpl(CodebookSchemaService codebookSchemaService,
            DocumentService documentService)
    {
        this.codebookSchemaService = codebookSchemaService;
        this.documentService = documentService;
    }

    @Override
    public CodebookStats create(Project project)
    {
        return this.create(documentService.listSourceDocuments(project));
    }

    @Override
    public CodebookStats create(List<SourceDocument> docs)
    {
        // make sure that each of the provided docs is from the same project (i.e. shares the same
        // codebooks)
        Project project = docs.get(0).getProject();
        docs.forEach(doc -> {
            assert doc.getProject().equals(project);
        });

        // get all cases (of all users) of all SourceDocuments
        List<CAS> CASes = this.loadCASes(docs);

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

    @Override
    public CodebookStats create(SourceDocument... inputDocs)
    {
        return this.create(Arrays.asList(inputDocs));
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

    private List<CAS> loadCASes(List<SourceDocument> docs)
    {
        List<CAS> CASes = new ArrayList<>();
        for (SourceDocument doc : docs)
            // get all annotation documents of all users of the current doc
            documentService.listAnnotationDocuments(doc).forEach(annotationDocument -> {
                try {
                    switch (annotationDocument.getUser()) {
                    case CURATION_USER:
                    case CORRECTION_USER:
                    case INITIAL_CAS_PSEUDO_USER:
                        break;
                    default:
                        CASes.add(documentService.readAnnotationCas(annotationDocument));
                    }

                }
                catch (IOException e) {
                    // TODO what to do?!
                    e.printStackTrace();
                }
            });

        return CASes;
    }
}
