/*
 * Copyright 2015
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
package de.tudarmstadt.ukp.clarin.webanno.curation.casdiff;

import static java.util.Arrays.asList;
import static org.apache.uima.fit.util.JCasUtil.select;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.fit.testing.factory.TokenBuilder;
import org.apache.uima.fit.util.FSUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.CasCreationUtils;
import org.junit.Rule;
import org.junit.Test;

import de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst;
import de.tudarmstadt.ukp.clarin.webanno.curation.agreement.AgreementUtils;
import de.tudarmstadt.ukp.clarin.webanno.curation.agreement.AgreementUtils.AgreementResult;
import de.tudarmstadt.ukp.clarin.webanno.curation.agreement.AgreementUtils.ConcreteAgreementMeasure;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff2;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff2.ArcDiffAdapter;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff2.DiffAdapter;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff2.DiffResult;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff2.LinkCompareBehavior;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff2.SpanDiffAdapter;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Lemma;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.Dependency;
import de.tudarmstadt.ukp.dkpro.core.testing.DkproTestContext;
import de.tudarmstadt.ukp.dkpro.statistics.agreement.coding.ICodingAnnotationItem;

public class CasDiff2Test
{
    @Test
    public void noDataTest()
        throws Exception
    {
        List<String> entryTypes = new ArrayList<>();
        
        List<DiffAdapter> diffAdapters = new ArrayList<>();

        Map<String, List<JCas>> casByUser = new LinkedHashMap<>();

        DiffResult result = CasDiff2.doDiff(entryTypes, diffAdapters,
                LinkCompareBehavior.LINK_TARGET_AS_LABEL, casByUser);
        
        result.print(System.out);
        
        assertEquals(0, result.size());
        assertEquals(0, result.getDifferingConfigurationSets().size());
        assertEquals(0, result.getIncompleteConfigurationSets().size());
    }

    @Test
    public void singleEmptyCasTest()
        throws Exception
    {
        String text = "";
        
        JCas user1Cas = JCasFactory.createJCas();
        user1Cas.setDocumentText(text);
        
        Map<String, List<JCas>> casByUser = new LinkedHashMap<>();
        casByUser.put("user1", asList(user1Cas));

        List<String> entryTypes = asList(Token.class.getName());

        List<SpanDiffAdapter> diffAdapters = asList(new SpanDiffAdapter(Token.class.getName()));

        DiffResult result = CasDiff2.doDiff(entryTypes, diffAdapters,
                LinkCompareBehavior.LINK_TARGET_AS_LABEL, casByUser);
        
        result.print(System.out);
        
        assertEquals(0, result.size());
        assertEquals(0, result.getDifferingConfigurationSets().size());
    }

    @Test
    public void twoEmptyCasTest()
        throws Exception
    {
        String text = "";
        
        JCas user1Cas = JCasFactory.createJCas();
        user1Cas.setDocumentText(text);

        JCas user2Cas = JCasFactory.createJCas();
        user2Cas.setDocumentText(text);

        Map<String, List<JCas>> casByUser = new LinkedHashMap<>();
        casByUser.put("user1", asList(user1Cas));
        casByUser.put("user2", asList(user2Cas));

        List<String> entryTypes = asList(Lemma.class.getName());

        List<SpanDiffAdapter> diffAdapters = asList(new SpanDiffAdapter(Lemma.class.getName()));

        DiffResult result = CasDiff2.doDiff(entryTypes, diffAdapters,
                LinkCompareBehavior.LINK_TARGET_AS_LABEL, casByUser);
        
        result.print(System.out);
        
        assertEquals(0, result.size());
        assertEquals(0, result.getDifferingConfigurationSets().size());
        assertEquals(0, result.getIncompleteConfigurationSets().size());

        AgreementResult agreement = AgreementUtils.getCohenKappaAgreement(result, entryTypes.get(0),
                "value", casByUser);
        assertEquals(Double.NaN, agreement.getAgreement(), 0.000001d);
        assertEquals(0, agreement.getIncompleteSetsByPosition().size());
    }

    @Test
    public void multipleEmptyCasWithMissingOnesTest()
        throws Exception
    {
        String text = "";
        
        JCas user1Cas1 = null;

        JCas user1Cas2 = null;

        JCas user1Cas3 = JCasFactory.createJCas();
        user1Cas3.setDocumentText(text);

        JCas user1Cas4 = JCasFactory.createJCas();
        user1Cas4.setDocumentText(text);

        JCas user2Cas1 = JCasFactory.createJCas();
        user2Cas1.setDocumentText(text);

        JCas user2Cas2 = null;

        JCas user2Cas3 = null;

        JCas user2Cas4 = JCasFactory.createJCas();
        user2Cas4.setDocumentText(text);
        
        Map<String, List<JCas>> casByUser = new LinkedHashMap<>();
        casByUser.put("user1", asList(user1Cas1, user1Cas2, user1Cas3, user1Cas4));
        casByUser.put("user2", asList(user2Cas1, user2Cas2, user2Cas3, user2Cas4));

        List<String> entryTypes = asList(Lemma.class.getName());

        List<SpanDiffAdapter> diffAdapters = asList(new SpanDiffAdapter(Lemma.class.getName()));

        DiffResult result = CasDiff2.doDiff(entryTypes, diffAdapters,
                LinkCompareBehavior.LINK_TARGET_AS_LABEL, casByUser);
        
        result.print(System.out);
        
        assertEquals(0, result.size());
        assertEquals(0, result.getDifferingConfigurationSets().size());
        assertEquals(0, result.getIncompleteConfigurationSets().size());

        AgreementResult agreement = AgreementUtils.getCohenKappaAgreement(result, entryTypes.get(0),
                "value", casByUser);
        assertEquals(Double.NaN, agreement.getAgreement(), 0.000001d);
        assertEquals(0, agreement.getIncompleteSetsByPosition().size());
    }
    @Test
    public void noDifferencesPosTest()
        throws Exception
    {
        Map<String, List<JCas>> casByUser = DiffUtils.load(
                "casdiff/noDifferences/data.conll",
                "casdiff/noDifferences/data.conll");

        List<String> entryTypes = asList(POS.class.getName());
        
        List<SpanDiffAdapter> diffAdapters = asList(SpanDiffAdapter.POS);

        DiffResult result = CasDiff2.doDiff(entryTypes, diffAdapters,
                LinkCompareBehavior.LINK_TARGET_AS_LABEL, casByUser);
        
        result.print(System.out);
        
        assertEquals(26, result.size());
        assertEquals(0, result.getDifferingConfigurationSets().size());
        assertEquals(0, result.getIncompleteConfigurationSets().size());

        AgreementResult agreement = AgreementUtils.getCohenKappaAgreement(result, entryTypes.get(0),
                "PosValue", casByUser);
        assertEquals(1.0d, agreement.getAgreement(), 0.000001d);
        assertEquals(0, agreement.getIncompleteSetsByPosition().size());
    }

    @Test
    public void noDifferencesDependencyTest()
        throws Exception
    {
        Map<String, List<JCas>> casByUser = DiffUtils.load(
                "casdiff/noDifferences/data.conll",
                "casdiff/noDifferences/data.conll");

        List<String> entryTypes = asList(Dependency.class.getName());

        List<? extends DiffAdapter> diffAdapters = asList(ArcDiffAdapter.DEPENDENCY);

        DiffResult result = CasDiff2.doDiff(entryTypes, diffAdapters,
                LinkCompareBehavior.LINK_TARGET_AS_LABEL, casByUser);

        result.print(System.out);
        
        assertEquals(26, result.size());
        assertEquals(0, result.getDifferingConfigurationSets().size());
        assertEquals(0, result.getIncompleteConfigurationSets().size());

        AgreementResult agreement = AgreementUtils.getCohenKappaAgreement(result, entryTypes.get(0),
                "DependencyType", casByUser);
        assertEquals(1.0d, agreement.getAgreement(), 0.000001d);
        assertEquals(0, agreement.getIncompleteSetsByPosition().size());
    }

    @Test
    public void noDifferencesPosDependencyTest()
        throws Exception
    {
        Map<String, List<JCas>> casByUser = DiffUtils.load(
                "casdiff/noDifferences/data.conll",
                "casdiff/noDifferences/data.conll");

        List<String> entryTypes = asList(POS.class.getName(), Dependency.class.getName());
        
        List<? extends DiffAdapter> diffAdapters = asList(
                SpanDiffAdapter.POS, 
                ArcDiffAdapter.DEPENDENCY);

        DiffResult result = CasDiff2.doDiff(entryTypes, diffAdapters,
                LinkCompareBehavior.LINK_TARGET_AS_LABEL, casByUser);

        result.print(System.out);
        
        assertEquals(52, result.size());
        assertEquals(26, result.size(POS.class.getName()));
        assertEquals(26, result.size(Dependency.class.getName()));
        assertEquals(0, result.getDifferingConfigurationSets().size());
        assertEquals(0, result.getIncompleteConfigurationSets().size());

        AgreementResult agreement = AgreementUtils.getCohenKappaAgreement(result, entryTypes.get(0),
                "PosValue", casByUser);
        assertEquals(1.0d, agreement.getAgreement(), 0.000001d);
        assertEquals(0, agreement.getIncompleteSetsByPosition().size());
    }

    @Test
    public void singleDifferencesTest()
        throws Exception
    {
        Map<String, List<JCas>> casByUser = DiffUtils.load(
                "casdiff/singleSpanDifference/user1.conll",
                "casdiff/singleSpanDifference/user2.conll");

        List<String> entryTypes = asList(POS.class.getName());

        List<SpanDiffAdapter> diffAdapters = asList(SpanDiffAdapter.POS);

        DiffResult result = CasDiff2.doDiff(entryTypes, diffAdapters,
                LinkCompareBehavior.LINK_TARGET_AS_LABEL, casByUser);

        result.print(System.out);
        
        assertEquals(1, result.size());
        assertEquals(1, result.getDifferingConfigurationSets().size());
        assertEquals(0, result.getIncompleteConfigurationSets().size());

        AgreementResult agreement = AgreementUtils.getCohenKappaAgreement(result, entryTypes.get(0),
                "PosValue", casByUser);
        assertEquals(0.0d, agreement.getAgreement(), 0.000001d);
        assertEquals(0, agreement.getIncompleteSetsByPosition().size());
    }

    @Test
    public void singleNoDifferencesWithAdditionalCas1Test()
        throws Exception
    {
        JCas user1 = JCasFactory.createJCas();
        user1.setDocumentText("test");

        JCas user2 = JCasFactory.createJCas();
        user2.setDocumentText("test");
        
        JCas user3 = JCasFactory.createJCas();
        user3.setDocumentText("test");
        POS pos3 = new POS(user3, 0, 4);
        pos3.setPosValue("test");
        pos3.addToIndexes();
        
        Map<String, List<JCas>> casByUser = new LinkedHashMap<>();
        casByUser.put("user1", asList(user1));
        casByUser.put("user2", asList(user2));
        casByUser.put("user3", asList(user3));
        
        List<String> entryTypes = asList(POS.class.getName());

        List<SpanDiffAdapter> diffAdapters = asList(SpanDiffAdapter.POS);

        DiffResult result = CasDiff2.doDiff(entryTypes, diffAdapters,
                LinkCompareBehavior.LINK_TARGET_AS_LABEL, casByUser);

        result.print(System.out);
        
        casByUser.remove("user3");
        
        AgreementResult agreement = AgreementUtils.getAgreement(
                ConcreteAgreementMeasure.KRIPPENDORFF_ALPHA_NOMINAL_AGREEMENT, false, result,
                entryTypes.get(0), "PosValue", casByUser);
        
        assertEquals(1, agreement.getTotalSetCount());
        assertEquals(1, agreement.getIrrelevantSets().size());
        assertEquals(0, agreement.getRelevantSetCount());
    }

    @Test
    public void singleNoDifferencesWithAdditionalCas2Test()
        throws Exception
    {
        JCas user1 = JCasFactory.createJCas();
        user1.setDocumentText("test");

        JCas user2 = JCasFactory.createJCas();
        user2.setDocumentText("test");
        
        JCas user3 = JCasFactory.createJCas();
        user3.setDocumentText("test");
        POS pos3 = new POS(user3, 0, 4);
        pos3.addToIndexes();
        
        Map<String, List<JCas>> casByUser = new LinkedHashMap<>();
        casByUser.put("user1", asList(user1));
        casByUser.put("user2", asList(user2));
        casByUser.put("user3", asList(user3));
        
        List<String> entryTypes = asList(POS.class.getName());

        List<SpanDiffAdapter> diffAdapters = asList(SpanDiffAdapter.POS);

        DiffResult result = CasDiff2.doDiff(entryTypes, diffAdapters,
                LinkCompareBehavior.LINK_TARGET_AS_LABEL, casByUser);

        result.print(System.out);
        
        casByUser.remove("user3");
        
        AgreementResult agreement = AgreementUtils.getAgreement(
                ConcreteAgreementMeasure.KRIPPENDORFF_ALPHA_NOMINAL_AGREEMENT, false, result,
                entryTypes.get(0), "PosValue", casByUser);
        
        assertEquals(1, agreement.getTotalSetCount());
        assertEquals(1, agreement.getIrrelevantSets().size());
        assertEquals(0, agreement.getRelevantSetCount());
    }

    @Test
    public void twoWithoutLabelTest()
        throws Exception
    {
        JCas user1 = JCasFactory.createJCas();
        user1.setDocumentText("test");
        new POS(user1, 0, 1).addToIndexes();
        new POS(user1, 1, 2).addToIndexes();
        POS p1 = new POS(user1, 3, 4);
        p1.setPosValue("A");
        p1.addToIndexes();

        JCas user2 = JCasFactory.createJCas();
        user2.setDocumentText("test");
        new POS(user2, 0, 1).addToIndexes();
        new POS(user2, 2, 3).addToIndexes();
        POS p2 = new POS(user2, 3, 4);
        p2.setPosValue("B");
        p2.addToIndexes();
        
        
        Map<String, List<JCas>> casByUser = new LinkedHashMap<>();
        casByUser.put("user1", asList(user1));
        casByUser.put("user2", asList(user2));
        
        List<String> entryTypes = asList(POS.class.getName());

        List<SpanDiffAdapter> diffAdapters = asList(SpanDiffAdapter.POS);

        DiffResult result = CasDiff2.doDiff(entryTypes, diffAdapters,
                LinkCompareBehavior.LINK_TARGET_AS_LABEL, casByUser);

        result.print(System.out);
        
        AgreementResult agreement = AgreementUtils.getAgreement(
                ConcreteAgreementMeasure.KRIPPENDORFF_ALPHA_NOMINAL_AGREEMENT, false, result,
                entryTypes.get(0), "PosValue", casByUser);
        
        assertEquals(4, agreement.getTotalSetCount());
        assertEquals(0, agreement.getIrrelevantSets().size());
        // the following two counts are zero because the incomplete sets are not excluded!
        assertEquals(2, agreement.getIncompleteSetsByPosition().size());
        assertEquals(0, agreement.getIncompleteSetsByLabel().size());
        assertEquals(3, agreement.getSetsWithDifferences().size());
        assertEquals(4, agreement.getRelevantSetCount());
        assertEquals(0.4, agreement.getAgreement(), 0.01);
        ICodingAnnotationItem item1 = agreement.getStudy().getItem(0);
        ICodingAnnotationItem item2 = agreement.getStudy().getItem(1);
        ICodingAnnotationItem item3 = agreement.getStudy().getItem(2);
        assertEquals("", item1.getUnit(0).getCategory());
        assertEquals("", item1.getUnit(1).getCategory());
        assertEquals("", item2.getUnit(0).getCategory());
        assertEquals(null, item2.getUnit(1).getCategory());
        assertEquals(null, item3.getUnit(0).getCategory());
        assertEquals("", item3.getUnit(1).getCategory());
    }

    @Test
    public void someDifferencesTest()
        throws Exception
    {
        Map<String, List<JCas>> casByUser = DiffUtils.load(
                "casdiff/someDifferences/user1.conll",
                "casdiff/someDifferences/user2.conll");

        List<String> entryTypes = asList(POS.class.getName());

        List<SpanDiffAdapter> diffAdapters = asList(SpanDiffAdapter.POS);

        DiffResult result = CasDiff2.doDiff(entryTypes, diffAdapters,
                LinkCompareBehavior.LINK_TARGET_AS_LABEL, casByUser);

        result.print(System.out);
        
        assertEquals(26, result.size());
        assertEquals(4, result.getDifferingConfigurationSets().size());
        assertEquals(0, result.getIncompleteConfigurationSets().size());
        
        AgreementResult agreement = AgreementUtils.getCohenKappaAgreement(result, entryTypes.get(0),
                "PosValue", casByUser);
        assertEquals(0.836477987d, agreement.getAgreement(), 0.000001d);
        assertEquals(0, agreement.getIncompleteSetsByPosition().size());
    }

    @Test
    public void singleNoDifferencesTest()
        throws Exception
    {
        Map<String, List<JCas>> casByUser = DiffUtils.load(
                "casdiff/singleSpanNoDifference/data.conll",
                "casdiff/singleSpanNoDifference/data.conll");

        List<String> entryTypes = asList(POS.class.getName());

        List<? extends DiffAdapter> diffAdapters = asList(new SpanDiffAdapter(POS.class.getName(),
                "PosValue"));

        DiffResult result = CasDiff2.doDiff(entryTypes, diffAdapters,
                LinkCompareBehavior.LINK_TARGET_AS_LABEL, casByUser);

        result.print(System.out);
        
        assertEquals(1, result.size());
        assertEquals(0, result.getDifferingConfigurationSets().size());
        assertEquals(0, result.getIncompleteConfigurationSets().size());
        
        AgreementResult agreement = AgreementUtils.getCohenKappaAgreement(result, entryTypes.get(0),
                "PosValue", casByUser);
        assertEquals(Double.NaN, agreement.getAgreement(), 0.000001d);
        assertEquals(0, agreement.getIncompleteSetsByPosition().size());
    }

    @Test
    public void relationDistanceTest()
        throws Exception
    {
        Map<String, List<JCas>> casByUser = DiffUtils.load(
                "casdiff/relationDistance/user1.conll",
                "casdiff/relationDistance/user2.conll");

        List<String> entryTypes = asList(Dependency.class.getName());

        List<? extends DiffAdapter> diffAdapters = asList(new ArcDiffAdapter(
                Dependency.class.getName(), "Dependent", "Governor", "DependencyType"));

        DiffResult result = CasDiff2.doDiff(entryTypes, diffAdapters,
                LinkCompareBehavior.LINK_TARGET_AS_LABEL, casByUser);
        
        result.print(System.out);
        
        assertEquals(27, result.size());
        assertEquals(0, result.getDifferingConfigurationSets().size());
        assertEquals(2, result.getIncompleteConfigurationSets().size());
        
        AgreementResult agreement = AgreementUtils.getCohenKappaAgreement(result, entryTypes.get(0),
                "DependencyType", casByUser);
        assertEquals(1.0, agreement.getAgreement(), 0.000001d);
        assertEquals(2, agreement.getIncompleteSetsByPosition().size());
    }

    @Test
    public void spanLabelLabelTest()
        throws Exception
    {
        Map<String, List<JCas>> casByUser = DiffUtils.load(
                "casdiff/spanLabel/user1.conll",
                "casdiff/spanLabel/user2.conll");

        List<String> entryTypes = asList(POS.class.getName());

        List<? extends DiffAdapter> diffAdapters = asList(new SpanDiffAdapter(POS.class.getName(),
                "PosValue"));

        DiffResult result = CasDiff2.doDiff(entryTypes, diffAdapters,
                LinkCompareBehavior.LINK_TARGET_AS_LABEL, casByUser);
        
        result.print(System.out);
        
        assertEquals(26, result.size());
        assertEquals(1, result.getDifferingConfigurationSets().size());
        assertEquals(0, result.getIncompleteConfigurationSets().size());
        
        AgreementResult agreement = AgreementUtils.getCohenKappaAgreement(result, entryTypes.get(0),
                "PosValue", casByUser);
        assertEquals(0.958730d, agreement.getAgreement(), 0.000001d);
        assertEquals(0, agreement.getIncompleteSetsByPosition().size());
    }

    @Test
    public void relationLabelTest()
        throws Exception
    {
        Map<String, List<JCas>> casByUser = DiffUtils.load(
                "casdiff/relationLabel/user1.conll",
                "casdiff/relationLabel/user2.conll");

        List<String> entryTypes = asList(Dependency.class.getName());

        List<? extends DiffAdapter> diffAdapters = asList(new ArcDiffAdapter(
                Dependency.class.getName(), "Dependent", "Governor", "DependencyType"));

        DiffResult result = CasDiff2.doDiff(entryTypes, diffAdapters,
                LinkCompareBehavior.LINK_TARGET_AS_LABEL, casByUser);

        result.print(System.out);
        
        assertEquals(26, result.size());
        assertEquals(1, result.getDifferingConfigurationSets().size());
        assertEquals(0, result.getIncompleteConfigurationSets().size());
        
        AgreementResult agreement = AgreementUtils.getCohenKappaAgreement(result, entryTypes.get(0),
                "DependencyType", casByUser);
        assertEquals(0.958199d, agreement.getAgreement(), 0.000001d);
        assertEquals(0, agreement.getIncompleteSetsByPosition().size());
    }
    
    @Test
    public void relationStackedSpansTest()
        throws Exception
    {
        TypeSystemDescription global = TypeSystemDescriptionFactory.createTypeSystemDescription();
        TypeSystemDescription local = TypeSystemDescriptionFactory
                .createTypeSystemDescriptionFromPath(
                        "src/test/resources/desc/type/webannoTestTypes.xml");
       
        TypeSystemDescription merged = CasCreationUtils.mergeTypeSystems(asList(global, local));

        TokenBuilder<Token, Sentence> tb = new TokenBuilder<>(Token.class,
                Sentence.class);
        
        JCas jcasA = JCasFactory.createJCas(merged);
        {
            CAS casA = jcasA.getCas();
            tb.buildTokens(jcasA, "This is a test .");
            
            List<Token> tokensA = new ArrayList<>(select(jcasA, Token.class));
            Token t1A = tokensA.get(0);
            Token t2A = tokensA.get(tokensA.size() - 1);
            
            NamedEntity govA = new NamedEntity(jcasA, t1A.getBegin(), t1A.getEnd());
            govA.addToIndexes();
            // Here we add a stacked named entity!
            new NamedEntity(jcasA, t1A.getBegin(), t1A.getEnd()).addToIndexes();
            
            NamedEntity depA =  new NamedEntity(jcasA, t2A.getBegin(), t2A.getEnd());
            depA.addToIndexes();
    
            Type relationTypeA = casA.getTypeSystem().getType("webanno.custom.Relation");
            AnnotationFS fs1A = casA.createAnnotation(relationTypeA, depA.getBegin(),
                    depA.getEnd());
            FSUtil.setFeature(fs1A, "Governor", govA);
            FSUtil.setFeature(fs1A, "Dependent", depA);
            FSUtil.setFeature(fs1A, "value", "REL");
            casA.addFsToIndexes(fs1A);
        }

        JCas jcasB = JCasFactory.createJCas(merged);
        {
            CAS casB = jcasB.getCas();
            tb.buildTokens(jcasB, "This is a test .");
            
            List<Token> tokensB = new ArrayList<>(select(jcasB, Token.class));
            Token t1B = tokensB.get(0);
            Token t2B = tokensB.get(tokensB.size() - 1);
            
            NamedEntity govB = new NamedEntity(jcasB, t1B.getBegin(), t1B.getEnd());
            govB.addToIndexes();
            NamedEntity depB =  new NamedEntity(jcasB, t2B.getBegin(), t2B.getEnd());
            depB.addToIndexes();
    
            Type relationTypeB = casB.getTypeSystem().getType("webanno.custom.Relation");
            AnnotationFS fs1B = casB.createAnnotation(relationTypeB, depB.getBegin(),
                    depB.getEnd());
            FSUtil.setFeature(fs1B, "Governor", govB);
            FSUtil.setFeature(fs1B, "Dependent", depB);
            FSUtil.setFeature(fs1B, "value", "REL");
            casB.addFsToIndexes(fs1B);
        }

        Map<String, List<JCas>> casByUser = new LinkedHashMap<>();
        casByUser.put("user1", asList(jcasA));
        casByUser.put("user2", asList(jcasB));

        List<String> entryTypes = asList("webanno.custom.Relation");

        List<? extends DiffAdapter> diffAdapters = asList(new ArcDiffAdapter(
                "webanno.custom.Relation", WebAnnoConst.FEAT_REL_TARGET,
                WebAnnoConst.FEAT_REL_SOURCE, "value"));

        DiffResult diff = CasDiff2.doDiff(entryTypes, diffAdapters,
                LinkCompareBehavior.LINK_TARGET_AS_LABEL, casByUser);
        
        diff.print(System.out);
        
        assertEquals(1, diff.size());
        assertEquals(0, diff.getDifferingConfigurationSets().size());
        assertEquals(0, diff.getIncompleteConfigurationSets().size());
        
        // Check against new impl
        AgreementResult agreement = AgreementUtils.getCohenKappaAgreement(diff,
                "webanno.custom.Relation", "value", casByUser);

        // Asserts
        System.out.printf("Agreement: %s%n", agreement.toString());
        AgreementUtils.dumpAgreementStudy(System.out, agreement);

        assertEquals(1, agreement.getPluralitySets().size());
    }

    @Test
    public void multiLinkWithRoleNoDifferenceTest()
        throws Exception
    {
        JCas jcasA = JCasFactory.createJCas(DiffUtils.createMultiLinkWithRoleTestTypeSytem());
        DiffUtils.makeLinkHostFS(jcasA, 0, 0, DiffUtils.makeLinkFS(jcasA, "slot1", 0, 0));        
        DiffUtils.makeLinkHostFS(jcasA, 10, 10, DiffUtils.makeLinkFS(jcasA, "slot1", 10, 10));

        JCas jcasB = JCasFactory.createJCas(DiffUtils.createMultiLinkWithRoleTestTypeSytem());
        DiffUtils.makeLinkHostFS(jcasB, 0, 0, DiffUtils.makeLinkFS(jcasB, "slot1", 0, 0));
        DiffUtils.makeLinkHostFS(jcasB, 10, 10, DiffUtils.makeLinkFS(jcasB, "slot1", 10, 10));

        Map<String, List<JCas>> casByUser = new LinkedHashMap<>();
        casByUser.put("user1", asList(jcasA));
        casByUser.put("user2", asList(jcasB));

        List<String> entryTypes = asList(DiffUtils.HOST_TYPE);

        SpanDiffAdapter adapter = new SpanDiffAdapter(DiffUtils.HOST_TYPE);
        adapter.addLinkFeature("links", "role", "target");
        List<? extends DiffAdapter> diffAdapters = asList(adapter);

        DiffResult diff = CasDiff2.doDiff(entryTypes, diffAdapters,
                LinkCompareBehavior.LINK_TARGET_AS_LABEL, casByUser);
        
        diff.print(System.out);
        
        assertEquals(4, diff.size());
        assertEquals(0, diff.getDifferingConfigurationSets().size());
        assertEquals(0, diff.getIncompleteConfigurationSets().size());
        
        // Check against new impl
        AgreementResult agreement = AgreementUtils.getCohenKappaAgreement(diff, DiffUtils.HOST_TYPE, "links",
                casByUser);

        // Asserts
        System.out.printf("Agreement: %s%n", agreement.toString());
        AgreementUtils.dumpAgreementStudy(System.out, agreement);
        
        assertEquals(1.0d, agreement.getAgreement(), 0.00001d);
    }

    @Test
    public void multiLinkWithRoleLabelDifferenceTest()
        throws Exception
    {
        JCas jcasA = JCasFactory.createJCas(DiffUtils.createMultiLinkWithRoleTestTypeSytem());
        DiffUtils.makeLinkHostFS(jcasA, 0, 0, DiffUtils.makeLinkFS(jcasA, "slot1", 0, 0));     

        JCas jcasB = JCasFactory.createJCas(DiffUtils.createMultiLinkWithRoleTestTypeSytem());
        DiffUtils.makeLinkHostFS(jcasB, 0, 0, DiffUtils.makeLinkFS(jcasB, "slot2", 0, 0));

        Map<String, List<JCas>> casByUser = new LinkedHashMap<>();
        casByUser.put("user1", asList(jcasA));
        casByUser.put("user2", asList(jcasB));

        List<String> entryTypes = asList(DiffUtils.HOST_TYPE);

        SpanDiffAdapter adapter = new SpanDiffAdapter(DiffUtils.HOST_TYPE);
        adapter.addLinkFeature("links", "role", "target");
        List<? extends DiffAdapter> diffAdapters = asList(adapter);

        DiffResult diff = CasDiff2.doDiff(entryTypes, diffAdapters,
                LinkCompareBehavior.LINK_TARGET_AS_LABEL, casByUser);
        
        diff.print(System.out);
        
        assertEquals(3, diff.size());
        assertEquals(0, diff.getDifferingConfigurationSets().size());
        assertEquals(2, diff.getIncompleteConfigurationSets().size());
        
        // Check against new impl
        AgreementResult agreement = AgreementUtils.getCohenKappaAgreement(diff, DiffUtils.HOST_TYPE, "links",
                casByUser);

        // Asserts
        System.out.printf("Agreement: %s%n", agreement.toString());
        AgreementUtils.dumpAgreementStudy(System.out, agreement);
        
        assertEquals(Double.NaN, agreement.getAgreement(), 0.00001d);
    }

    @Test
    public void multiLinkWithRoleLabelDifferenceTest2()
        throws Exception
    {
        JCas jcasA = JCasFactory.createJCas(DiffUtils.createMultiLinkWithRoleTestTypeSytem());
        DiffUtils.makeLinkHostFS(jcasA, 0, 0, DiffUtils.makeLinkFS(jcasA, "slot1", 0, 0));     

        JCas jcasB = JCasFactory.createJCas(DiffUtils.createMultiLinkWithRoleTestTypeSytem());
        DiffUtils.makeLinkHostFS(jcasB, 0, 0, DiffUtils.makeLinkFS(jcasB, "slot2", 0, 0));

        Map<String, List<JCas>> casByUser = new LinkedHashMap<>();
        casByUser.put("user1", asList(jcasA));
        casByUser.put("user2", asList(jcasB));

        List<String> entryTypes = asList(DiffUtils.HOST_TYPE);

        SpanDiffAdapter adapter = new SpanDiffAdapter(DiffUtils.HOST_TYPE);
        adapter.addLinkFeature("links", "role", "target");
        List<? extends DiffAdapter> diffAdapters = asList(adapter);

        DiffResult diff = CasDiff2.doDiff(entryTypes, diffAdapters,
                LinkCompareBehavior.LINK_ROLE_AS_LABEL, casByUser);
        
        diff.print(System.out);
        
        assertEquals(2, diff.size());
        assertEquals(1, diff.getDifferingConfigurationSets().size());
        assertEquals(0, diff.getIncompleteConfigurationSets().size());
        
        // Check against new impl
        AgreementResult agreement = AgreementUtils.getCohenKappaAgreement(diff, DiffUtils.HOST_TYPE, "links",
                casByUser);

        // Asserts
        System.out.printf("Agreement: %s%n", agreement.toString());
        AgreementUtils.dumpAgreementStudy(System.out, agreement);
        
        assertEquals(0.0d, agreement.getAgreement(), 0.00001d);
    }

    @Test
    public void multiLinkWithRoleTargetDifferenceTest()
        throws Exception
    {
        JCas jcasA = JCasFactory.createJCas(DiffUtils.createMultiLinkWithRoleTestTypeSytem());
        DiffUtils.makeLinkHostFS(jcasA, 0, 0, DiffUtils.makeLinkFS(jcasA, "slot1", 0, 0));      

        JCas jcasB = JCasFactory.createJCas(DiffUtils.createMultiLinkWithRoleTestTypeSytem());
        DiffUtils.makeLinkHostFS(jcasB, 0, 0, DiffUtils.makeLinkFS(jcasB, "slot1", 10, 10));

        Map<String, List<JCas>> casByUser = new LinkedHashMap<>();
        casByUser.put("user1", asList(jcasA));
        casByUser.put("user2", asList(jcasB));

        List<String> entryTypes = asList(DiffUtils.HOST_TYPE);

        SpanDiffAdapter adapter = new SpanDiffAdapter(DiffUtils.HOST_TYPE);
        adapter.addLinkFeature("links", "role", "target");
        List<? extends DiffAdapter> diffAdapters = asList(adapter);

        DiffResult diff = CasDiff2.doDiff(entryTypes, diffAdapters,
                LinkCompareBehavior.LINK_TARGET_AS_LABEL, casByUser);
        
        diff.print(System.out);
        
        assertEquals(2, diff.size());
        assertEquals(1, diff.getDifferingConfigurationSets().size());
        assertEquals(0, diff.getIncompleteConfigurationSets().size());

        // Check against new impl
        AgreementResult agreement = AgreementUtils.getCohenKappaAgreement(diff, DiffUtils.HOST_TYPE, "links",
                casByUser);

        // Asserts
        System.out.printf("Agreement: %s%n", agreement.toString());
        AgreementUtils.dumpAgreementStudy(System.out, agreement);
        
        assertEquals(0.0, agreement.getAgreement(), 0.00001d);
    }

    @Test
    public void multiLinkWithRoleMultiTargetDifferenceTest()
        throws Exception
    {
        JCas jcasA = JCasFactory.createJCas(DiffUtils.createMultiLinkWithRoleTestTypeSytem());
        DiffUtils.makeLinkHostFS(jcasA, 0, 0, 
                DiffUtils.makeLinkFS(jcasA, "slot1", 0, 0),
                DiffUtils.makeLinkFS(jcasA, "slot1", 10, 10));      

        JCas jcasB = JCasFactory.createJCas(DiffUtils.createMultiLinkWithRoleTestTypeSytem());
        DiffUtils.makeLinkHostFS(jcasB, 0, 0, 
                DiffUtils.makeLinkFS(jcasB, "slot1", 10, 10));

        Map<String, List<JCas>> casByUser = new LinkedHashMap<>();
        casByUser.put("user1", asList(jcasA));
        casByUser.put("user2", asList(jcasB));

        List<String> entryTypes = asList(DiffUtils.HOST_TYPE);

        SpanDiffAdapter adapter = new SpanDiffAdapter(DiffUtils.HOST_TYPE);
        adapter.addLinkFeature("links", "role", "target");
        List<? extends DiffAdapter> diffAdapters = asList(adapter);

        DiffResult diff = CasDiff2.doDiff(entryTypes, diffAdapters,
                LinkCompareBehavior.LINK_TARGET_AS_LABEL, casByUser);
        
        diff.print(System.out);
        
        assertEquals(2, diff.size());
        assertEquals(1, diff.getDifferingConfigurationSets().size());
        assertEquals(0, diff.getIncompleteConfigurationSets().size());
        
//        // Check against new impl
//        AgreementResult agreement = AgreementUtils.getCohenKappaAgreement(diff, HOST_TYPE, "links",
//                casByUser);
//
//        // Asserts
//        System.out.printf("Agreement: %s%n", agreement.toString());
//        AgreementUtils.dumpAgreementStudy(System.out, agreement);
//        
//        assertEquals(0.0, agreement.getAgreement(), 0.00001d);
    }

    @Test
    public void multiLinkWithRoleMultiTargetDifferenceTest2()
        throws Exception
    {
        JCas jcasA = JCasFactory.createJCas(DiffUtils.createMultiLinkWithRoleTestTypeSytem());
        DiffUtils.makeLinkHostFS(jcasA, 0, 0, 
                DiffUtils.makeLinkFS(jcasA, "slot1", 0, 0),
                DiffUtils.makeLinkFS(jcasA, "slot1", 10, 10));      

        JCas jcasB = JCasFactory.createJCas(DiffUtils.createMultiLinkWithRoleTestTypeSytem());
        DiffUtils.makeLinkHostFS(jcasB, 0, 0, 
                DiffUtils.makeLinkFS(jcasB, "slot2", 10, 10));

        Map<String, List<JCas>> casByUser = new LinkedHashMap<>();
        casByUser.put("user1", asList(jcasA));
        casByUser.put("user2", asList(jcasB));

        List<String> entryTypes = asList(DiffUtils.HOST_TYPE);

        SpanDiffAdapter adapter = new SpanDiffAdapter(DiffUtils.HOST_TYPE);
        adapter.addLinkFeature("links", "role", "target");
        List<? extends DiffAdapter> diffAdapters = asList(adapter);

        DiffResult diff = CasDiff2.doDiff(entryTypes, diffAdapters,
                LinkCompareBehavior.LINK_TARGET_AS_LABEL, casByUser);
        
        diff.print(System.out);
        
        assertEquals(3, diff.size());
        assertEquals(1, diff.getDifferingConfigurationSets().size());
        assertEquals(2, diff.getIncompleteConfigurationSets().size());

//        // Check against new impl
//        AgreementResult agreement = AgreementUtils.getCohenKappaAgreement(diff, HOST_TYPE, "links",
//                casByUser);
//
//        // Asserts
//        System.out.printf("Agreement: %s%n", agreement.toString());
//        AgreementUtils.dumpAgreementStudy(System.out, agreement);
//        
//        assertEquals(0.0, agreement.getAgreement(), 0.00001d);
    }
    
    @Rule
    public DkproTestContext testContext = new DkproTestContext();
}
