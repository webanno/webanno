/*
 * Copyright 2015
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
 * Technische Universität Darmstadt
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.clarin.webanno.ui.curation.util;

import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.SPAN_TYPE;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.util.CasUtil;
import org.apache.uima.jcas.JCas;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.AnnotationException;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff2;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.DiffUtils;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.LinkMode;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.Dependency;
import mockit.Mock;
import mockit.MockUp;

public class CopyAnnotationTest
{
    private AnnotationSchemaService annotationSchemaService;
    private Project project;
    private AnnotationLayer tokenLayer;
    private AnnotationFeature tokenPosFeature;
    private AnnotationLayer posLayer;
    private AnnotationFeature posFeature;
    private AnnotationLayer slotLayer;
    private AnnotationFeature slotFeature;
    private AnnotationFeature stringFeature;
    
    @Before
    public void setup()
    {
        project = new Project();
        
        tokenLayer = new AnnotationLayer(Token.class.getName(), "Token",
                SPAN_TYPE, null, true);
        
        tokenPosFeature = new AnnotationFeature();
        tokenPosFeature.setName("pos");
        tokenPosFeature.setEnabled(true);
        tokenPosFeature.setType(POS.class.getName());
        tokenPosFeature.setUiName("pos");
        tokenPosFeature.setLayer(tokenLayer);
        tokenPosFeature.setProject(project);
        tokenPosFeature.setVisible(true);
        
        posLayer = new AnnotationLayer(POS.class.getName(), "POS",
                SPAN_TYPE, project, true);
        posLayer.setAttachType(tokenLayer);
        posLayer.setAttachFeature(tokenPosFeature);
        
        posFeature = new AnnotationFeature();
        posFeature.setName("PosValue");
        posFeature.setEnabled(true);
        posFeature.setType(CAS.TYPE_NAME_STRING);
        posFeature.setUiName("PosValue");
        posFeature.setLayer(posLayer);
        posFeature.setProject(project);
        posFeature.setVisible(true);
        
        slotLayer = new AnnotationLayer(DiffUtils.HOST_TYPE, DiffUtils.HOST_TYPE,
                SPAN_TYPE, project, false);
        slotFeature = new AnnotationFeature();
        slotFeature.setName("links");
        slotFeature.setEnabled(true);
        slotFeature.setType(Token.class.getName());
        slotFeature.setLinkMode(LinkMode.WITH_ROLE);
        slotFeature.setUiName("f1");
        slotFeature.setLayer(slotLayer);
        slotFeature.setProject(project);
        slotFeature.setVisible(true);
        stringFeature = new AnnotationFeature();
        stringFeature.setName("f1");
        stringFeature.setEnabled(true);
        stringFeature.setType(CAS.TYPE_NAME_STRING);
        stringFeature.setUiName("f1");
        stringFeature.setLayer(slotLayer);
        stringFeature.setProject(project);
        stringFeature.setVisible(true);
        
        annotationSchemaService = new MockUp<AnnotationSchemaService>()
        {
            @Mock
            List<AnnotationFeature> listAnnotationFeature(AnnotationLayer type)
            {
                if (type.getName().equals(POS.class.getName())) {
                    return asList(posFeature);
                }
                if (type.getName().equals(DiffUtils.HOST_TYPE)) {
                    return asList(slotFeature, stringFeature);
                }
                throw new IllegalStateException("Unknown layer type: " + type.getName());
            }
        }.getMockInstance();
    }
    
    @Test
    public void simpleCopyToEmptyTest()
        throws Exception
    {
        
        JCas jcas = JCasFactory.createJCas();
        Type type = jcas.getTypeSystem().getType(POS.class.getTypeName());
        AnnotationFS clickedFs = createPOSAnno(jcas, type, "NN", 0, 0);

        JCas mergeCAs = JCasFactory.createJCas();
        createTokenAnno(mergeCAs, 0, 0);

        MergeCas.addSpanAnnotation(annotationSchemaService, posLayer, mergeCAs, clickedFs, false);

        assertEquals(1, CasUtil.selectCovered(mergeCAs.getCas(), type, 0, 0).size());
    }

    private AnnotationFS createPOSAnno(JCas aJCas, Type aType, String aValue, int aBegin, int aEnd)
    {
        AnnotationFS clickedFs = aJCas.getCas().createAnnotation(aType, aBegin, aEnd);
        Feature posValue = aType.getFeatureByBaseName("PosValue");
        clickedFs.setStringValue(posValue, aValue);
        aJCas.addFsToIndexes(clickedFs);
        return clickedFs;
    }

    private AnnotationFS createTokenAnno(JCas aJCas, int aBegin, int aEnd)
    {
        Type type = aJCas.getTypeSystem().getType(Token.class.getTypeName());
        AnnotationFS token = aJCas.getCas().createAnnotation(type, aBegin, aEnd);
        aJCas.addFsToIndexes(token);
        return token;
    }

    @Test
    public void simpleCopyToSameExistingAnnoTest()
        throws Exception
    {
        JCas jcas = JCasFactory.createJCas();
        Type type = jcas.getTypeSystem().getType(POS.class.getTypeName());
        AnnotationFS clickedFs = createPOSAnno(jcas, type, "NN", 0, 0);

        JCas mergeCAs = JCasFactory.createJCas();
        AnnotationFS existingFs = mergeCAs.getCas().createAnnotation(type, 0, 0);
        Feature posValue = type.getFeatureByBaseName("PosValue");
        existingFs.setStringValue(posValue, "NN");
        mergeCAs.addFsToIndexes(existingFs);

        exception.expect(AnnotationException.class);
        MergeCas.addSpanAnnotation(annotationSchemaService, posLayer, mergeCAs, clickedFs, false);
    }

    @Test
    public void simpleCopyToDiffExistingAnnoWithNoStackingTest()
        throws Exception
    {
        JCas jcas = JCasFactory.createJCas();
        Type type = jcas.getTypeSystem().getType(POS.class.getTypeName());
        AnnotationFS clickedFs = createPOSAnno(jcas, type, "NN", 0, 0);

        JCas mergeCAs = JCasFactory.createJCas();
        AnnotationFS existingFs = mergeCAs.getCas().createAnnotation(type, 0, 0);
        Feature posValue = type.getFeatureByBaseName("PosValue");
        existingFs.setStringValue(posValue, "NE");
        mergeCAs.addFsToIndexes(existingFs);

        MergeCas.addSpanAnnotation(annotationSchemaService, posLayer, mergeCAs, clickedFs, false);

        assertEquals(1, CasUtil.selectCovered(mergeCAs.getCas(), type, 0, 0).size());
    }

    @Test
    public void simpleCopyToDiffExistingAnnoWithStackingTest()
        throws Exception
    {
        posLayer.setAllowStacking(true);

        JCas jcas = JCasFactory.createJCas();
        Type type = jcas.getTypeSystem().getType(POS.class.getTypeName());
        AnnotationFS clickedFs = createPOSAnno(jcas, type, "NN", 0, 0);

        JCas mergeCAs = JCasFactory.createJCas();
        createTokenAnno(mergeCAs, 0, 0);
        AnnotationFS existingFs = mergeCAs.getCas().createAnnotation(type, 0, 0);
        Feature posValue = type.getFeatureByBaseName("PosValue");
        existingFs.setStringValue(posValue, "NE");
        mergeCAs.addFsToIndexes(existingFs);

        MergeCas.addSpanAnnotation(annotationSchemaService, posLayer, mergeCAs, clickedFs, true);

        assertEquals(2, CasUtil.selectCovered(mergeCAs.getCas(), type, 0, 0).size());
    }

    @Test
    public void copySpanWithSlotNoStackingTest()
        throws Exception
    {
        slotLayer.setAllowStacking(false);
        
        JCas jcasA = JCasFactory.createJCas(DiffUtils.createMultiLinkWithRoleTestTypeSytem("f1"));
        Type type = jcasA.getTypeSystem().getType(DiffUtils.HOST_TYPE);
        Feature feature = type.getFeatureByBaseName("f1");

        AnnotationFS clickedFs = DiffUtils.makeLinkHostMultiSPanFeatureFS(jcasA, 0, 0, feature, "A",
                DiffUtils.makeLinkFS(jcasA, "slot1", 0, 0));

        JCas mergeCAs = JCasFactory
                .createJCas(DiffUtils.createMultiLinkWithRoleTestTypeSytem("f1"));

        DiffUtils.makeLinkHostMultiSPanFeatureFS(mergeCAs, 0, 0, feature, "C",
                DiffUtils.makeLinkFS(mergeCAs, "slot1", 0, 0));

        MergeCas.addSpanAnnotation(annotationSchemaService, slotLayer, mergeCAs, clickedFs, false);

        assertEquals(1, CasUtil.selectCovered(mergeCAs.getCas(), type, 0, 0).size());
    }

    @Test
    public void copySpanWithSlotWithStackingTest()
        throws Exception
    {
        slotLayer.setAllowStacking(true);
        
        JCas jcasA = JCasFactory.createJCas(DiffUtils.createMultiLinkWithRoleTestTypeSytem("f1"));
        Type type = jcasA.getTypeSystem().getType(DiffUtils.HOST_TYPE);
        Feature feature = type.getFeatureByBaseName("f1");

        AnnotationFS clickedFs = DiffUtils.makeLinkHostMultiSPanFeatureFS(jcasA, 0, 0, feature, "A",
                DiffUtils.makeLinkFS(jcasA, "slot1", 0, 0));

        JCas mergeCAs = JCasFactory
                .createJCas(DiffUtils.createMultiLinkWithRoleTestTypeSytem("f1"));

        DiffUtils.makeLinkHostMultiSPanFeatureFS(mergeCAs, 0, 0, feature, "C",
                DiffUtils.makeLinkFS(mergeCAs, "slot1", 0, 0));

        MergeCas.addSpanAnnotation(annotationSchemaService, slotLayer, mergeCAs, clickedFs, true);

        assertEquals(2, CasUtil.selectCovered(mergeCAs.getCas(), type, 0, 0).size());
    }

    @Test
    public void copyLinkToEmptyTest()
        throws Exception
    {

        JCas mergeCAs = JCasFactory
                .createJCas(DiffUtils.createMultiLinkWithRoleTestTypeSytem("f1"));
        Type type = mergeCAs.getTypeSystem().getType(DiffUtils.HOST_TYPE);
        Feature feature = type.getFeatureByBaseName("f1");

        AnnotationFS mergeFs = DiffUtils.makeLinkHostMultiSPanFeatureFS(mergeCAs, 0, 0, feature,
                "A");

        FeatureStructure copyFS = DiffUtils.makeLinkFS(mergeCAs, "slot1", 0, 0);

        List<FeatureStructure> linkFs = new ArrayList<>();
        linkFs.add(copyFS);
        WebAnnoCasUtil.setLinkFeatureValue(mergeFs, type.getFeatureByBaseName("links"), linkFs);

        JCas jcasA = JCasFactory.createJCas(DiffUtils.createMultiLinkWithRoleTestTypeSytem("f1"));
        DiffUtils.makeLinkHostMultiSPanFeatureFS(jcasA, 0, 0, feature, "A",
                DiffUtils.makeLinkFS(jcasA, "slot1", 0, 0));

        Map<String, List<JCas>> casByUser = new LinkedHashMap<>();
        casByUser.put("user1", asList(mergeCAs));
        casByUser.put("user2", asList(jcasA));

        List<String> entryTypes = asList(DiffUtils.HOST_TYPE);

        CasDiff2.SpanDiffAdapter adapter = new CasDiff2.SpanDiffAdapter(DiffUtils.HOST_TYPE);
        adapter.addLinkFeature("links", "role", "target");
        List<? extends CasDiff2.DiffAdapter> diffAdapters = asList(adapter);

        CasDiff2.DiffResult diff = CasDiff2.doDiff(entryTypes, diffAdapters,
                CasDiff2.LinkCompareBehavior.LINK_TARGET_AS_LABEL, casByUser);

        assertEquals(0, diff.getDifferingConfigurationSets().size());
        assertEquals(0, diff.getIncompleteConfigurationSets().size());
    }

    @Test
    public void copyLinkToExistingButDiffLinkTest()
        throws Exception
    {

        JCas mergeCAs = JCasFactory
                .createJCas(DiffUtils.createMultiLinkWithRoleTestTypeSytem("f1"));
        Type type = mergeCAs.getTypeSystem().getType(DiffUtils.HOST_TYPE);
        Feature feature = type.getFeatureByBaseName("f1");

        AnnotationFS mergeFs = DiffUtils.makeLinkHostMultiSPanFeatureFS(mergeCAs, 0, 0, feature,
                "A", DiffUtils.makeLinkFS(mergeCAs, "slot1", 0, 0));

        FeatureStructure copyFS = DiffUtils.makeLinkFS(mergeCAs, "slot2", 0, 0);

        List<FeatureStructure> linkFs = new ArrayList<>();
        linkFs.add(copyFS);
        WebAnnoCasUtil.setLinkFeatureValue(mergeFs, type.getFeatureByBaseName("links"), linkFs);

        JCas jcasA = JCasFactory.createJCas(DiffUtils.createMultiLinkWithRoleTestTypeSytem("f1"));
        DiffUtils.makeLinkHostMultiSPanFeatureFS(jcasA, 0, 0, feature, "A",
                DiffUtils.makeLinkFS(jcasA, "slot1", 0, 0));

        Map<String, List<JCas>> casByUser = new LinkedHashMap<>();
        casByUser.put("user1", asList(mergeCAs));
        casByUser.put("user2", asList(jcasA));

        List<String> entryTypes = asList(DiffUtils.HOST_TYPE);

        CasDiff2.SpanDiffAdapter adapter = new CasDiff2.SpanDiffAdapter(DiffUtils.HOST_TYPE);
        adapter.addLinkFeature("links", "role", "target");
        List<? extends CasDiff2.DiffAdapter> diffAdapters = asList(adapter);

        CasDiff2.DiffResult diff = CasDiff2.doDiff(entryTypes, diffAdapters,
                CasDiff2.LinkCompareBehavior.LINK_TARGET_AS_LABEL, casByUser);

        assertEquals(0, diff.getDifferingConfigurationSets().size());
        assertEquals(2, diff.getIncompleteConfigurationSets().size());
    }

    @Test
    public void simpleCopyRelationToEmptyAnnoTest()
        throws Exception
    {
        JCas jcas = JCasFactory.createJCas();
        Type type = jcas.getTypeSystem().getType(Dependency.class.getTypeName());
        Type posType = jcas.getTypeSystem().getType(POS.class.getTypeName());

        AnnotationFS originClickedToken = createTokenAnno(jcas, 0, 0);
        AnnotationFS targetClickedToken = createTokenAnno(jcas, 1, 1);

        AnnotationFS originClicked = createPOSAnno(jcas, posType, "NN", 0, 0);
        AnnotationFS targetClicked = createPOSAnno(jcas, posType, "NN", 1, 1);

        jcas.addFsToIndexes(originClicked);
        jcas.addFsToIndexes(targetClicked);

        originClickedToken.setFeatureValue(originClickedToken.getType().getFeatureByBaseName("pos"),
                originClicked);
        targetClickedToken.setFeatureValue(targetClickedToken.getType().getFeatureByBaseName("pos"),
                targetClicked);

        Feature sourceFeature = type.getFeatureByBaseName(WebAnnoConst.FEAT_REL_SOURCE);
        Feature targetFeature = type.getFeatureByBaseName(WebAnnoConst.FEAT_REL_TARGET);

        AnnotationFS clickedFs = jcas.getCas().createAnnotation(type, 0, 1);
        clickedFs.setFeatureValue(sourceFeature, originClickedToken);
        clickedFs.setFeatureValue(targetFeature, targetClickedToken);
        jcas.addFsToIndexes(clickedFs);

        JCas mergeCAs = JCasFactory.createJCas();
        AnnotationFS origin = createPOSAnno(mergeCAs, posType, "NN", 0, 0);
        AnnotationFS target = createPOSAnno(mergeCAs, posType, "NN", 1, 1);

        mergeCAs.addFsToIndexes(origin);
        mergeCAs.addFsToIndexes(target);

        AnnotationFS originToken = createTokenAnno(mergeCAs, 0, 0);
        AnnotationFS targetToken = createTokenAnno(mergeCAs, 1, 1);
        originToken.setFeatureValue(originToken.getType().getFeatureByBaseName("pos"), origin);
        targetToken.setFeatureValue(targetToken.getType().getFeatureByBaseName("pos"), target);

        mergeCAs.addFsToIndexes(originToken);
        mergeCAs.addFsToIndexes(targetToken);

        MergeCas.addRelationArcAnnotation(mergeCAs, clickedFs, true, false, originToken,
                targetToken);
        assertEquals(1, CasUtil.selectCovered(mergeCAs.getCas(), type, 0, 1).size());
    }

    @Test
    public void simpleCopyRelationToStackedTargetsTest()
        throws Exception
    {
        JCas jcas = JCasFactory.createJCas();
        Type type = jcas.getTypeSystem().getType(Dependency.class.getTypeName());
        Type posType = jcas.getTypeSystem().getType(POS.class.getTypeName());

        AnnotationFS originClickedToken = createTokenAnno(jcas, 0, 0);
        AnnotationFS targetClickedToken = createTokenAnno(jcas, 1, 1);

        AnnotationFS originClicked = createPOSAnno(jcas, posType, "NN", 0, 0);
        AnnotationFS targetClicked = createPOSAnno(jcas, posType, "NN", 1, 1);

        jcas.addFsToIndexes(originClicked);
        jcas.addFsToIndexes(targetClicked);

        originClickedToken.setFeatureValue(originClickedToken.getType().getFeatureByBaseName("pos"),
                originClicked);
        targetClickedToken.setFeatureValue(targetClickedToken.getType().getFeatureByBaseName("pos"),
                targetClicked);

        Feature sourceFeature = type.getFeatureByBaseName(WebAnnoConst.FEAT_REL_SOURCE);
        Feature targetFeature = type.getFeatureByBaseName(WebAnnoConst.FEAT_REL_TARGET);

        AnnotationFS clickedFs = jcas.getCas().createAnnotation(type, 0, 1);
        clickedFs.setFeatureValue(sourceFeature, originClickedToken);
        clickedFs.setFeatureValue(targetFeature, targetClickedToken);
        jcas.addFsToIndexes(clickedFs);

        JCas mergeCAs = JCasFactory.createJCas();
        AnnotationFS origin = createPOSAnno(mergeCAs, posType, "NN", 0, 0);
        AnnotationFS target = createPOSAnno(mergeCAs, posType, "NN", 1, 1);

        mergeCAs.addFsToIndexes(origin);
        mergeCAs.addFsToIndexes(target);

        AnnotationFS originToken = createTokenAnno(mergeCAs, 0, 0);
        AnnotationFS targetToken = createTokenAnno(mergeCAs, 1, 1);
        originToken.setFeatureValue(originToken.getType().getFeatureByBaseName("pos"), origin);
        targetToken.setFeatureValue(targetToken.getType().getFeatureByBaseName("pos"), target);

        mergeCAs.addFsToIndexes(originToken);
        mergeCAs.addFsToIndexes(targetToken);

        AnnotationFS origin2 = createPOSAnno(mergeCAs, posType, "NN", 0, 0);
        AnnotationFS target2 = createPOSAnno(mergeCAs, posType, "NN", 1, 1);

        mergeCAs.addFsToIndexes(origin2);
        mergeCAs.addFsToIndexes(target2);

        AnnotationFS originToken2 = createTokenAnno(mergeCAs, 0, 0);
        AnnotationFS targetToken2 = createTokenAnno(mergeCAs, 1, 1);
        originToken2.setFeatureValue(originToken.getType().getFeatureByBaseName("pos"), origin2);
        targetToken2.setFeatureValue(targetToken.getType().getFeatureByBaseName("pos"), target2);

        mergeCAs.addFsToIndexes(originToken2);
        mergeCAs.addFsToIndexes(targetToken2);

        exception.expect(AnnotationException.class);
        MergeCas.addRelationArcAnnotation(mergeCAs, clickedFs, true, false, originToken,
                targetToken);

    }

    @Test
    public void simpleCopyStackedRelationTest()
        throws Exception
    {
        JCas jcas = JCasFactory.createJCas();
        Type type = jcas.getTypeSystem().getType(Dependency.class.getTypeName());
        Type posType = jcas.getTypeSystem().getType(POS.class.getTypeName());

        AnnotationFS originClickedToken = createTokenAnno(jcas, 0, 0);
        AnnotationFS targetClickedToken = createTokenAnno(jcas, 1, 1);

        AnnotationFS originClicked = createPOSAnno(jcas, posType, "NN", 0, 0);
        AnnotationFS targetClicked = createPOSAnno(jcas, posType, "NN", 1, 1);

        jcas.addFsToIndexes(originClicked);
        jcas.addFsToIndexes(targetClicked);

        originClickedToken.setFeatureValue(originClickedToken.getType().getFeatureByBaseName("pos"),
                originClicked);
        targetClickedToken.setFeatureValue(targetClickedToken.getType().getFeatureByBaseName("pos"),
                targetClicked);

        Feature sourceFeature = type.getFeatureByBaseName(WebAnnoConst.FEAT_REL_SOURCE);
        Feature targetFeature = type.getFeatureByBaseName(WebAnnoConst.FEAT_REL_TARGET);

        AnnotationFS clickedFs = jcas.getCas().createAnnotation(type, 0, 1);
        clickedFs.setFeatureValue(sourceFeature, originClickedToken);
        clickedFs.setFeatureValue(targetFeature, targetClickedToken);
        jcas.addFsToIndexes(clickedFs);

        JCas mergeCAs = JCasFactory.createJCas();
        AnnotationFS origin = createPOSAnno(mergeCAs, posType, "NN", 0, 0);
        AnnotationFS target = createPOSAnno(mergeCAs, posType, "NN", 1, 1);

        mergeCAs.addFsToIndexes(origin);
        mergeCAs.addFsToIndexes(target);

        AnnotationFS originToken = createTokenAnno(mergeCAs, 0, 0);
        AnnotationFS targetToken = createTokenAnno(mergeCAs, 1, 1);
        originToken.setFeatureValue(originToken.getType().getFeatureByBaseName("pos"), origin);
        targetToken.setFeatureValue(targetToken.getType().getFeatureByBaseName("pos"), target);

        mergeCAs.addFsToIndexes(originToken);
        mergeCAs.addFsToIndexes(targetToken);

        AnnotationFS existing = mergeCAs.getCas().createAnnotation(type, 0, 1);
        existing.setFeatureValue(sourceFeature, originToken);
        existing.setFeatureValue(targetFeature, targetToken);
        mergeCAs.addFsToIndexes(clickedFs);

        exception.expect(AnnotationException.class);
        MergeCas.addRelationArcAnnotation(mergeCAs, clickedFs, true, false, originToken,
                targetToken);
    }

    @Rule
    public final ExpectedException exception = ExpectedException.none();

}
