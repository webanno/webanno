/*
 * Copyright 2012
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
package de.tudarmstadt.ukp.clarin.webanno.ui.curation.component.model;

import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.TypeUtil.getAdapter;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.getAddr;
import static org.apache.uima.fit.util.CasUtil.selectCovered;
import static org.apache.uima.fit.util.JCasUtil.select;
import static org.apache.uima.fit.util.JCasUtil.selectCovered;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.uima.UIMAException;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.jcas.JCas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.CorrectionDocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.AnnotationException;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff2;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff2.Configuration;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff2.ConfigurationSet;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff2.DiffResult;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff2.LinkCompareBehavior;
import de.tudarmstadt.ukp.clarin.webanno.curation.storage.CurationDocumentService;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.ui.curation.util.MergeCas;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

/**
 * This class is responsible for two things. Firstly, it creates a pre-merged cas, which contains
 * all annotations, where all annotators agree on. This is done by copying a random cas and removing
 * all differing annotations.
 *
 * Secondly, the class creates an instance of {@link CurationContainer}, which is the wicket model
 * for the curation panel. The {@link CurationContainer} contains the text for all sentences, which
 * are displayed at a specific page.
 */
public class SuggestionBuilder
{
    private final Logger log = LoggerFactory.getLogger(getClass());    
    
    private final AnnotationSchemaService annotationService;
    private final DocumentService documentService;
    private final CorrectionDocumentService correctionDocumentService;
    private final CurationDocumentService curationDocumentService;
    private final UserDao userRepository;

    int diffRangeBegin, diffRangeEnd;
    boolean firstload = true;
    public static Map<Integer, Set<Integer>> crossSentenceLists;
    //
    Map<Integer, Integer> segmentBeginEnd = new HashMap<>();

    public SuggestionBuilder(DocumentService aDocumentService,
            CorrectionDocumentService aCorrectionDocumentService,
            CurationDocumentService aCurationDocumentService, AnnotationSchemaService aAnnotationService,
            UserDao aUserDao)
    {
        documentService = aDocumentService;
        correctionDocumentService = aCorrectionDocumentService;
        curationDocumentService = aCurationDocumentService;
        annotationService = aAnnotationService;
        userRepository = aUserDao;
    }

    public CurationContainer buildCurationContainer(AnnotatorState aBModel)
        throws UIMAException, ClassNotFoundException, IOException, AnnotationException
    {
        CurationContainer curationContainer = new CurationContainer();
        // initialize Variables
        SourceDocument sourceDocument = aBModel.getDocument();
        Map<Integer, Integer> segmentBeginEnd = new HashMap<>();
        Map<Integer, Integer> segmentNumber = new HashMap<>();
        Map<String, Map<Integer, Integer>> segmentAdress = new HashMap<>();
        // get annotation documents

        List<AnnotationDocument> finishedAnnotationDocuments = new ArrayList<>();

        for (AnnotationDocument annotationDocument : documentService.listAnnotationDocuments(aBModel
                .getDocument())) {
            if (annotationDocument.getState().equals(AnnotationDocumentState.FINISHED)) {
                finishedAnnotationDocuments.add(annotationDocument);
            }
        }

        Map<String, JCas> jCases = new HashMap<>();

        AnnotationDocument randomAnnotationDocument = null;
        JCas mergeJCas;

        // get the correction/automation JCas for the logged in user
        if (aBModel.getMode().equals(Mode.AUTOMATION) || aBModel.getMode().equals(Mode.CORRECTION)) {
            jCases = listJcasesforCorrection(randomAnnotationDocument, sourceDocument,
                    aBModel.getMode());
            mergeJCas = getMergeCas(aBModel, sourceDocument, jCases, randomAnnotationDocument, false);
            String username = jCases.keySet().iterator().next();
            updateSegment(aBModel, segmentBeginEnd, segmentNumber, segmentAdress,
                    jCases.get(username), username, aBModel.getWindowBeginOffset(),
                    aBModel.getWindowEndOffset());
        }
        else {
            jCases = listJcasesforCuration(finishedAnnotationDocuments, randomAnnotationDocument,
                    aBModel.getMode());
            mergeJCas = getMergeCas(aBModel, sourceDocument, jCases, randomAnnotationDocument, false);
            updateSegment(aBModel, segmentBeginEnd, segmentNumber, segmentAdress, mergeJCas,
                    WebAnnoConst.CURATION_USER,
                    WebAnnoCasUtil.getFirstSentence(mergeJCas).getBegin(),
                    mergeJCas.getDocumentText().length());

        }

        List<Type> entryTypes = null;

        segmentAdress.put(WebAnnoConst.CURATION_USER, new HashMap<>());
        for (Sentence sentence : selectCovered(mergeJCas, Sentence.class, diffRangeBegin, diffRangeEnd)) {
            segmentAdress.get(WebAnnoConst.CURATION_USER).put(sentence.getBegin(),
                    getAddr(sentence));
        }

        if (entryTypes == null) {
            entryTypes = getEntryTypes(mergeJCas, aBModel.getAnnotationLayers(), annotationService);
        }

        // for cross-sentences annotation, update the end of the segment
        if (firstload) {
            long start = System.currentTimeMillis();
            log.debug("Updating cross sentence annotation list...");
            updateCrossSentAnnoList(segmentBeginEnd, segmentNumber, jCases, entryTypes);
            firstload = false;
            log.debug("Cross sentence annotation list complete in {}ms",
                    (System.currentTimeMillis() - start));
        }

        long diffStart = System.currentTimeMillis();
        log.debug("Calculating differences...");
        int count = 0;
        for (Integer begin : segmentBeginEnd.keySet()) {
            Integer end = segmentBeginEnd.get(begin);

            count ++;
            if (count % 100 == 0) {
                log.debug("Processing differences: {} of {} sentences...", count,
                        segmentBeginEnd.size());
            }

            DiffResult diff = CasDiff2.doDiffSingle(annotationService, aBModel.getProject(),
                    entryTypes, LinkCompareBehavior.LINK_ROLE_AS_LABEL, jCases, begin, end);
            
            SourceListView curationSegment = new SourceListView();
            curationSegment.setBegin(begin);
            curationSegment.setEnd(end);
            curationSegment.setSentenceNumber(segmentNumber.get(begin));
            if (diff.hasDifferences() || !diff.getIncompleteConfigurationSets().isEmpty()) {
                // Is this confSet a diff due to stacked annotations (with same configuration)?
                boolean stackedDiff = false;

                stackedDiffSet: for (ConfigurationSet d : diff.getDifferingConfigurationSets()
                        .values()) {
                    for (Configuration c : d.getConfigurations()) {
                        if (c.getCasGroupIds().size() != d.getCasGroupIds().size()) {
                            stackedDiff = true;
                            break stackedDiffSet;
                        }
                    }
                }

                if (stackedDiff) {
                    curationSegment.setSentenceState(SentenceState.DISAGREE);
                }
                else if (!diff.getIncompleteConfigurationSets().isEmpty()) {
                    curationSegment.setSentenceState(SentenceState.DISAGREE);
                }
                else {
                    curationSegment.setSentenceState(SentenceState.AGREE);
                }
            }
            else {
                curationSegment.setSentenceState(SentenceState.AGREE);
            }

			for (String username : segmentAdress.keySet()) {
				curationSegment.getSentenceAddress().put(username,
                        segmentAdress.get(username).get(begin));
            }
            curationContainer.getCurationViewByBegin().put(begin, curationSegment);
        }
        log.debug("Difference calculation completed in {}ms",
                (System.currentTimeMillis() - diffStart));
        
        return curationContainer;
    }

    private void updateCrossSentAnnoList(Map<Integer, Integer> aSegmentBeginEnd,
            Map<Integer, Integer> aSegmentNumber, Map<String, JCas> aJCases, List<Type> aEntryTypes)
    {
        // FIXME Remove this side-effect and instead return this hashmap
        crossSentenceLists = new HashMap<>();
        
        // Extract the sentences for all the CASes
        Map<JCas, List<Sentence>> idxSentences = new HashMap<>();
        for (JCas c : aJCases.values()) {
            idxSentences.put(c, new ArrayList<>(select(c, Sentence.class)));
        }
        
        Set<Integer> sentenceBegins = aSegmentBeginEnd.keySet();
        int count = 0;
        for (int sentBegin : sentenceBegins) {
            count ++;

            if (count % 100 == 0) {
                log.debug("Updating cross-sentence annoations: {} of {} sentences...", count,
                        sentenceBegins.size());
            }

            int sentEnd = aSegmentBeginEnd.get(sentBegin);
            int currentSentenceNumber = -1;
            
            Set<Integer> crossSents = new HashSet<>();
            
            for (Type t : aEntryTypes) {
                for (JCas c : aJCases.values()) {
                    // Determine sentence number for the current segment begin. This takes quite
                    // a while, so we only do it for the first CAS in the batch. Will be the
                    // same for all others anyway.
                    if (currentSentenceNumber == -1) {
                        currentSentenceNumber = aSegmentNumber.get(sentBegin);
                    }
                    
                    // update cross-sentence annotation lists
                    for (AnnotationFS fs : selectCovered(c.getCas(), t, diffRangeBegin,
                            diffRangeEnd)) {
                        // CASE 1. annotation begins here
                        if (sentBegin <= fs.getBegin() && fs.getBegin() <= sentEnd) {
                            if (fs.getEnd() < sentBegin || sentEnd < fs.getEnd()) {
                                Sentence s = getSentenceByAnnoEnd(idxSentences.get(c), fs.getEnd());
                                int thatSent = idxSentences.get(c).indexOf(s) + 1;
                                crossSents.add(thatSent);
                            }
                        }
                        // CASE 2. Annotation ends here
                        else if (sentBegin <= fs.getEnd() && fs.getEnd() <= sentEnd) {
                            if (fs.getBegin() < sentBegin || sentEnd < fs.getBegin()) {
                                int thatSent = WebAnnoCasUtil.getSentenceNumber(c, fs.getBegin());
                                crossSents.add(thatSent);
                            }
                        }
                    }

                    for (AnnotationFS fs : selectCovered(c.getCas(), t, sentBegin, diffRangeEnd)) {
                        if (fs.getBegin() <= sentEnd && fs.getEnd() > sentEnd) {
                            Sentence s = getSentenceByAnnoEnd(idxSentences.get(c), fs.getEnd());
                            aSegmentBeginEnd.put(sentBegin, s.getEnd());
                        }
                    }
                }
            }
            crossSentenceLists.put(currentSentenceNumber, crossSents);
        }
    }
    
    /**
     * Get a sentence at the end of an annotation
     */
    private static Sentence getSentenceByAnnoEnd(List<Sentence> aSentences, int aEnd)
    {
        int prevEnd = 0;
        Sentence sent = null;
        for (Sentence sentence : aSentences) {
            if (prevEnd >= aEnd) {
                return sent;
            }
            sent = sentence;
            prevEnd = sent.getEnd();
        }
        return sent;
    }


    private Map<String, JCas> listJcasesforCorrection(AnnotationDocument randomAnnotationDocument,
            SourceDocument aDocument, Mode aMode)
        throws UIMAException, ClassNotFoundException, IOException
    {
        Map<String, JCas> jCases = new HashMap<>();
        User user = userRepository.get(SecurityContextHolder.getContext().getAuthentication()
                .getName());
        randomAnnotationDocument = documentService.getAnnotationDocument(aDocument, user);

        // Upgrading should be an explicit action during the opening of a document at the end
        // of the open dialog - it must not happen during editing because the CAS addresses
        // are used as IDs in the UI
        // repository.upgradeCasAndSave(aDocument, aMode, user.getUsername());
        JCas jCas = documentService.readAnnotationCas(randomAnnotationDocument);
        jCases.put(user.getUsername(), jCas);
        return jCases;
    }

    public Map<String, JCas> listJcasesforCuration(List<AnnotationDocument> annotationDocuments,
            AnnotationDocument randomAnnotationDocument, Mode aMode)
        throws UIMAException, ClassNotFoundException, IOException
    {
        Map<String, JCas> jCases = new HashMap<>();
        for (AnnotationDocument annotationDocument : annotationDocuments) {
            String username = annotationDocument.getUser();

            if (!annotationDocument.getState().equals(AnnotationDocumentState.FINISHED)) {
                continue;
            }

            if (randomAnnotationDocument == null) {
                randomAnnotationDocument = annotationDocument;
            }

            // Upgrading should be an explicit action during the opening of a document at the end
            // of the open dialog - it must not happen during editing because the CAS addresses
            // are used as IDs in the UI
            // repository.upgradeCasAndSave(annotationDocument.getDocument(), aMode, username);
            JCas jCas = documentService.readAnnotationCas(annotationDocument);
            jCases.put(username, jCas);
        }
        return jCases;
    }

    /**
     * Fetches the CAS that the user will be able to edit. In AUTOMATION/CORRECTION mode, this is
     * the CAS for the CORRECTION_USER and in CURATION mode it is the CAS for the CURATION user.
     *
     * @param aBratAnnotatorModel
     *            the model.
     * @param aDocument
     *            the source document.
     * @param jCases
     *            the JCases.
     * @param randomAnnotationDocument
     *            an annotation document.
     * @return the JCas.
     * @throws UIMAException
     *             hum?
     * @throws ClassNotFoundException
     *             hum?
     * @throws IOException
     *             if an I/O error occurs.
     * @throws AnnotationException
     *             hum?
     */
    public JCas getMergeCas(AnnotatorState aBratAnnotatorModel, SourceDocument aDocument,
            Map<String, JCas> jCases, AnnotationDocument randomAnnotationDocument, boolean aUpgrade)
        throws UIMAException, ClassNotFoundException, IOException, AnnotationException
    {
        JCas mergeJCas = null;
        try {
            if (aBratAnnotatorModel.getMode().equals(Mode.AUTOMATION)
                    || aBratAnnotatorModel.getMode().equals(Mode.CORRECTION)) {
                // Upgrading should be an explicit action during the opening of a document at the
                // end
                // of the open dialog - it must not happen during editing because the CAS addresses
                // are used as IDs in the UI
                // repository.upgradeCasAndSave(aDocument, aBratAnnotatorModel.getMode(),
                // aBratAnnotatorModel.getUser().getUsername());
                mergeJCas = correctionDocumentService.readCorrectionCas(aDocument);
                if (aUpgrade) {
                    correctionDocumentService.upgradeCorrectionCas(mergeJCas.getCas(), aDocument);
                    correctionDocumentService.writeCorrectionCas(mergeJCas, aDocument);
                }
            }
            else {
                // Upgrading should be an explicit action during the opening of a document at the
                // end
                // of the open dialog - it must not happen during editing because the CAS addresses
                // are used as IDs in the UI
                // repository.upgradeCasAndSave(aDocument, aBratAnnotatorModel.getMode(),
                // aBratAnnotatorModel.getUser().getUsername());
                mergeJCas = curationDocumentService.readCurationCas(aDocument);
                if (aUpgrade) {
                    curationDocumentService.upgradeCurationCas(mergeJCas.getCas(), aDocument);
                    curationDocumentService.writeCurationCas(mergeJCas, aDocument, true);
                }
            }
        }
        // Create jcas, if it could not be loaded from the file system
        catch (Exception e) {

            if (aBratAnnotatorModel.getMode().equals(Mode.AUTOMATION)
                    || aBratAnnotatorModel.getMode().equals(Mode.CORRECTION)) {
                mergeJCas = createCorrectionCas(mergeJCas, aBratAnnotatorModel,
                        randomAnnotationDocument);
            }
            else {
                mergeJCas = createCurationCas(aBratAnnotatorModel.getProject(),
                        randomAnnotationDocument, jCases, aBratAnnotatorModel.getAnnotationLayers());
            }
        }
        return mergeJCas;
    }

    /**
     * Puts JCases into a list and get a random annotation document that will be used as a base for
     * the diff.
     */
    private void updateSegment(AnnotatorState aBratAnnotatorModel,
            Map<Integer, Integer> aIdxSentenceBeginEnd,
            Map<Integer, Integer> aIdxSentenceBeginNumber,
            Map<String, Map<Integer, Integer>> aSegmentAdress, JCas aJCas, String aUsername,
            int aWindowStart, int aWindowEnd)
    {
        diffRangeBegin = aWindowStart;
        diffRangeEnd = aWindowEnd;

        // Get the number of the first sentence - instead of fetching the number over and over
        // we can just increment this one.
        int sentenceNumber = WebAnnoCasUtil.getSentenceNumber(aJCas, diffRangeBegin);

        aSegmentAdress.put(aUsername, new HashMap<>());
        for (Sentence sentence : selectCovered(aJCas, Sentence.class, diffRangeBegin,
                diffRangeEnd)) {
            aIdxSentenceBeginEnd.put(sentence.getBegin(), sentence.getEnd());
            aIdxSentenceBeginNumber.put(sentence.getBegin(), sentenceNumber);
            aSegmentAdress.get(aUsername).put(sentence.getBegin(), getAddr(sentence));
            sentenceNumber += 1;
        }
    }

    public static List<Type> getEntryTypes(JCas mergeJCas, List<AnnotationLayer> aLayers,
            AnnotationSchemaService aAnnotationService)
    {
        List<Type> entryTypes = new LinkedList<>();

        for (AnnotationLayer layer : aLayers) {
            if (layer.getName().equals(Token.class.getName())) {
                continue;
            }
            if (layer.getType().equals(WebAnnoConst.CHAIN_TYPE)) {
                continue;
            }
            entryTypes.add(getAdapter(aAnnotationService, layer).getAnnotationType(
                    mergeJCas.getCas()));
        }
        return entryTypes;
    }

    
    /**
     * For the first time a curation page is opened, create a MergeCas that contains only agreeing
     * annotations Using the CAS of the curator user.
     *
     * @param aProject
     *            the project
     * @param randomAnnotationDocument
     *            an annotation document.
     * @param jCases
     *            the JCases
     * @param aAnnotationLayers
     *            the layers.
     * @return the JCas.
     * @throws IOException
     *             if an I/O error occurs.
     */
    public JCas createCurationCas(Project aProject, AnnotationDocument randomAnnotationDocument,
            Map<String, JCas> jCases, List<AnnotationLayer> aAnnotationLayers)
                throws IOException, UIMAException
    {
        User userLoggedIn = userRepository
                .get(SecurityContextHolder.getContext().getAuthentication().getName());

        JCas mergeJCas = documentService.readAnnotationCas(randomAnnotationDocument);
        jCases.put(WebAnnoConst.CURATION_USER, mergeJCas);

        List<Type> entryTypes = getEntryTypes(mergeJCas, aAnnotationLayers, annotationService);

        DiffResult diff = CasDiff2.doDiffSingle(annotationService, aProject, entryTypes,
                LinkCompareBehavior.LINK_ROLE_AS_LABEL, jCases, 0,
                mergeJCas.getDocumentText().length());

        mergeJCas = MergeCas.geMergeCas(diff, jCases);

        curationDocumentService.writeCurationCas(mergeJCas, randomAnnotationDocument.getDocument(),
                false);
        return mergeJCas;
    }
    
    private JCas createCorrectionCas(JCas mergeJCas, AnnotatorState aBratAnnotatorModel,
            AnnotationDocument randomAnnotationDocument)
        throws UIMAException, ClassNotFoundException, IOException
    {
        User userLoggedIn = userRepository.get(SecurityContextHolder.getContext()
                .getAuthentication().getName());
        mergeJCas = documentService.readAnnotationCas(aBratAnnotatorModel.getDocument(), userLoggedIn);
        correctionDocumentService.writeCorrectionCas(mergeJCas, randomAnnotationDocument.getDocument());
        return mergeJCas;
    }
}
