/*
 * Copyright 2019
 * Ubiquitous Knowledge Processing (UKP) Lab Technische Universität Darmstadt  
 *  and Language Technology Group  Universität Hamburg 
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
package de.tudarmstadt.ukp.clarin.webanno.codebook.service;

import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.copyDocumentMetadata;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.createCas;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.createSentence;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.createToken;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.exists;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.getAddr;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.selectAt;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.selectSentences;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.selectTokens;
import static org.apache.uima.cas.impl.Serialization.deserializeCASComplete;
import static org.apache.uima.cas.impl.Serialization.serializeCASComplete;
import static org.apache.uima.fit.util.CasUtil.getType;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.uima.UIMAException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.impl.CASCompleteSerializer;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.text.AnnotationFS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.event.BulkAnnotationEvent;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.AnnotationException;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil;
import de.tudarmstadt.ukp.clarin.webanno.codebook.adapter.CodebookAdapter;
import de.tudarmstadt.ukp.clarin.webanno.codebook.model.Codebook;
import de.tudarmstadt.ukp.clarin.webanno.codebook.model.CodebookFeature;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff.Configuration;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff.ConfigurationSet;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff.DiffResult;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.api.Position;
import de.tudarmstadt.ukp.clarin.webanno.curation.casmerge.AlreadyMergedException;
import de.tudarmstadt.ukp.clarin.webanno.curation.casmerge.CasMergeOpertationResult;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.support.logging.LogMessage;
import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData;

/**
 * Do a merge CAS out of multiple user annotations
 */
public class CodebookCasMerge
{
    private static final Logger LOG = LoggerFactory.getLogger(CodebookCasMerge.class);
    
    private final CodebookSchemaService schemaService;
    private final ApplicationEventPublisher eventPublisher;
    
    private boolean mergeIncompleteAnnotations = false;
    public CodebookCasMerge(CodebookSchemaService  aSchemaService)
    {
        this(aSchemaService, null);
    }
    
    public CodebookCasMerge(CodebookSchemaService aSchemaService,
            ApplicationEventPublisher aEventPublisher)
    {
        schemaService = aSchemaService;
        eventPublisher = aEventPublisher;
    }
    
    public void setMergeIncompleteAnnotations(boolean aMergeIncompleteAnnotations)
    {
        mergeIncompleteAnnotations = aMergeIncompleteAnnotations;
    }
    
    public boolean isMergeIncompleteAnnotations()
    {
        return mergeIncompleteAnnotations;
    }

    private boolean shouldMerge(DiffResult aDiff, ConfigurationSet cfgs)
    {
        boolean stacked = cfgs.getConfigurations().stream()
                .filter(Configuration::isStacked)
                .findAny()
                .isPresent();
        if (stacked) {
            LOG.trace(" `-> Not merging stacked annotation");
            return false;
        }
        
        if (!aDiff.isComplete(cfgs) && !isMergeIncompleteAnnotations()) {
            LOG.trace(" `-> Not merging incomplete annotation");
            return false;
        }
        
        if (!aDiff.isAgreement(cfgs)) {
            LOG.trace(" `-> Not merging annotation with disagreement");
            return false;
        }
        
        return true;
    }
    
    /**
     * Using {@code DiffResult}, determine the annotations to be deleted from the randomly generated
     * MergeCase. The initial Merge CAs is stored under a name {@code CurationPanel#CURATION_USER}.
     * <p>
     * Any similar annotations stacked in a {@code CasDiff2.Position} will be assumed a difference
     * <p>
     * Any two annotation with different value will be assumed a difference
     * @param aDiff
     *            the {@link DiffResult}
     * @param aCases
     *            a map of {@code CAS}s for each users and the random merge
     */
    public void reMergeCas(DiffResult aDiff, SourceDocument aTargetDocument, String aTargetUsername,
            CAS aTargetCas, Map<String, CAS> aCases)
        throws AnnotationException, UIMAException
    {
        
        List<LogMessage> messages = new ArrayList<>();
        
        // Remove any annotations from the target CAS - keep type system, sentences and tokens
        clearAnnotations(aTargetCas);
        
        // If there is nothing to merge, bail out
        if (aCases.isEmpty()) {
            return;
        }
                
        // Set up a cache for resolving type to layer to avoid hammering the DB as we process each
        // position
 
        Map<String, Codebook> type2code = aDiff.getPositions().stream()
                .map(Position::getType)
                .distinct()
                .map(type -> schemaService.getCodeBook(type, aTargetDocument.getProject()))
                .collect(Collectors.toMap(Codebook::getName, Function.identity()));

        List<String> codeNames = new ArrayList<>(type2code.keySet());

        for (String codeName : codeNames) {
            List<CodebookPosition> positions = aDiff.getPositions().stream()
                    .filter(pos -> codeName.equals(pos.getType()))
                    .filter(pos -> pos instanceof CodebookPosition)
                    .map(pos -> (CodebookPosition) pos)
                    .filter(pos -> pos.getFeature() == null)
                    .collect(Collectors.toList());
            
            if (positions.isEmpty()) {
                continue;
            }

            LOG.trace("Processing {} codebook positions on layer {}", positions.size(), codeName);
            for (CodebookPosition position : positions) {
                LOG.trace(" |   processing {}", position);
                ConfigurationSet cfgs = aDiff.getConfigurtionSet(position);
                
                if (!shouldMerge(aDiff, cfgs)) {
                    continue;
                }
                
                try {
                    AnnotationFS sourceFS = (AnnotationFS) cfgs.getConfigurations().get(0)
                            .getRepresentative();
                    mergeCodebookAnnotation(aTargetDocument, aTargetUsername,
                            type2code.get(position.getType()), aTargetCas, sourceFS, false);
                    LOG.trace(" `-> merged annotation with agreement");
                }
                catch (AnnotationException e) {
                    LOG.trace(" `-> not merged annotation: {}", e.getMessage());
                    messages.add(LogMessage.error(this, "%s", e.getMessage()));
                }
            }
        }
        

        if (eventPublisher != null) {
            eventPublisher.publishEvent(
                    new BulkAnnotationEvent(this, aTargetDocument, aTargetUsername, null));
        }
        
    }

    private static void clearAnnotations(CAS aCas)
        throws UIMAException
    {
        CAS backup = createCas();
        
        // Copy the CAS - basically we do this just to keep the full type system information
        CASCompleteSerializer serializer = serializeCASComplete((CASImpl) aCas);
        deserializeCASComplete(serializer, (CASImpl) backup);

        // Remove all annotations from the target CAS but we keep the type system!
        aCas.reset();
        
        // Copy over essential information
        if (exists(backup, getType(backup, DocumentMetaData.class))) {
            copyDocumentMetadata(backup, aCas);
        }
        else {
            WebAnnoCasUtil.createDocumentMetadata(aCas);
        }
        aCas.setDocumentLanguage(backup.getDocumentLanguage()); // DKPro Core Issue 435
        aCas.setDocumentText(backup.getDocumentText());
        
        // Transfer token boundaries
        for (AnnotationFS t : selectTokens(backup)) {
            aCas.addFsToIndexes(createToken(aCas, t.getBegin(), t.getEnd()));
        }

        // Transfer sentence boundaries
        for (AnnotationFS s : selectSentences(backup)) {
            aCas.addFsToIndexes(createSentence(aCas, s.getBegin(), s.getEnd()));
        }
    }
    /**
     * Do not check on agreement on Position and SOfa feature - already checked
     */
    private static boolean isBasicFeature(Feature aFeature)
    {
        // FIXME The two parts of this OR statement seem to be redundant. Also the order
        // of the check should be changes such that equals is called on the constant.
        return aFeature.getName().equals(CAS.FEATURE_FULL_NAME_SOFA)
                || aFeature.toString().equals("uima.cas.AnnotationBase:sofa");
    }

    /**
     * Return true if these two annotations agree on every non slot features
     */
    private static boolean isSameAnno(AnnotationFS aFs1, AnnotationFS aFs2)
    {
        // Check offsets (because they are excluded by shouldIgnoreFeatureOnMerge())
        if (aFs1.getBegin() != aFs2.getBegin() || aFs1.getEnd() != aFs2.getEnd()) {
            return false;
        }
        
        // Check the features (basically limiting to the primitive features)
        for (Feature f : aFs1.getType().getFeatures()) {
            if (shouldIgnoreFeatureOnMerge(aFs1, f)) {
                continue;
            }

            Object value1 = getFeatureValue(aFs1, f);
            Object value2 = getFeatureValue(aFs2, f);
            
            if (!Objects.equals(value1, value2)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Get the feature value of this {@code Feature} on this annotation
     */
    private static Object getFeatureValue(FeatureStructure aFS, Feature aFeature)
    {
        switch (aFeature.getRange().getName()) {
        case CAS.TYPE_NAME_STRING:
            return aFS.getFeatureValueAsString(aFeature);
        case CAS.TYPE_NAME_BOOLEAN:
            return aFS.getBooleanValue(aFeature);
        case CAS.TYPE_NAME_FLOAT:
            return aFS.getFloatValue(aFeature);
        case CAS.TYPE_NAME_INTEGER:
            return aFS.getIntValue(aFeature);
        case CAS.TYPE_NAME_BYTE:
            return aFS.getByteValue(aFeature);
        case CAS.TYPE_NAME_DOUBLE:
            return aFS.getDoubleValue(aFeature);
        case CAS.TYPE_NAME_LONG:
            aFS.getLongValue(aFeature);
        case CAS.TYPE_NAME_SHORT:
            aFS.getShortValue(aFeature);
        default:
            return null;
        // return aFS.getFeatureValue(aFeature);
        }
    }

    private static boolean existsSameAt(CAS aCas, AnnotationFS aFs)
    {
        return selectAt(aCas, aFs.getType(), aFs.getBegin(), aFs.getEnd()).stream()
                .filter(cand -> isSameAnno(aFs, cand))
                .findAny()
                .isPresent();
    }



    private void copyFeatures(SourceDocument aDocument, String aUsername, CodebookAdapter aAdapter,
            Codebook aCodebook, FeatureStructure aTargetFS, FeatureStructure aSourceFs)
    {

        CodebookFeature feature = schemaService.listCodebookFeature(aCodebook).get(0);

        Type sourceFsType = aAdapter.getAnnotationType(aSourceFs.getCAS());
        Feature sourceFeature = sourceFsType.getFeatureByBaseName(feature.getName());

        if (sourceFeature == null) {
            throw new IllegalStateException("Target CAS type [" + sourceFsType.getName()
                    + "] does not define a feature named [" + feature.getName() + "]");
        }

        if (shouldIgnoreFeatureOnMerge(aSourceFs, sourceFeature)) {
            return;
        }

        Object value = aAdapter.getExistingCodeValue(aSourceFs.getCAS(), feature);
        aAdapter.setFeatureValue(aTargetFS.getCAS(), feature, getAddr(aTargetFS), value);

    }

    private static boolean shouldIgnoreFeatureOnMerge(FeatureStructure aFS, Feature aFeature)
    {
        return !WebAnnoCasUtil.isPrimitiveType(aFeature.getRange()) || 
                isBasicFeature(aFeature) ||
                aFeature.getName().equals(CAS.FEATURE_FULL_NAME_BEGIN) ||
                aFeature.getName().equals(CAS.FEATURE_FULL_NAME_END);
    }

    public CasMergeOpertationResult mergeCodebookAnnotation(SourceDocument aDocument, String aUsername,
            Codebook aCodebook, CAS aTargetCas, AnnotationFS aSourceFs,
            boolean aAllowStacking)
        throws AnnotationException
    {
        if (existsSameAt(aTargetCas, aSourceFs)) {
            throw new AlreadyMergedException(
                    "The annotation already exists in the target document.");
        }

        CodebookAdapter adapter = new CodebookAdapter(aCodebook);
        List<AnnotationFS> existingAnnos = selectAt(aTargetCas, aSourceFs.getType(),
                aSourceFs.getBegin(), aSourceFs.getEnd());
        if (existingAnnos.isEmpty() ) {
            adapter.add(aTargetCas);
                
            AnnotationFS mergedCode = adapter.getExistingFs(aTargetCas);
            copyFeatures(aDocument, aUsername,adapter, aCodebook, mergedCode, aSourceFs);
            return CasMergeOpertationResult.CREATED;
        }

        else {
            copyFeatures(aDocument, aUsername, adapter, aCodebook, existingAnnos.get(0), aSourceFs);
        }
        return CasMergeOpertationResult.UPDATED;

    }
}
