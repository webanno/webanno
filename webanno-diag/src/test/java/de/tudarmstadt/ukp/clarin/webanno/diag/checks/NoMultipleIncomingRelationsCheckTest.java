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
package de.tudarmstadt.ukp.clarin.webanno.diag.checks;

import static org.junit.Assert.assertFalse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.diag.CasDoctor.LogMessage;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.Dependency;
import de.tudarmstadt.ukp.dkpro.core.testing.DkproTestContext;

@RunWith(SpringRunner.class)
public class NoMultipleIncomingRelationsCheckTest {


    @Configuration
    @Import({AnnotationSchemaService.class, NoMultipleIncomingRelationsCheck.class})
    static class Config {
    }
    
    @Rule
    public DkproTestContext testContext = new DkproTestContext();

    @MockBean
    private AnnotationSchemaService annotationService;
    
    @Autowired
    NoMultipleIncomingRelationsCheck check;

    @Test
    public void testFail() throws Exception {

        AnnotationLayer layer1 = new AnnotationLayer();
        Mockito.when(annotationService.listAnnotationLayer(Mockito.isNull()))
                .thenReturn(Arrays.asList(layer1));

        JCas jcas = JCasFactory.createJCas();

        jcas.setDocumentText("This is a test.");

        Token spanThis = new Token(jcas, 0, 4);
        spanThis.addToIndexes();

        Token spanIs = new Token(jcas, 6, 8);
        Token spanA = new Token(jcas, 9, 10);
        spanA.addToIndexes();

        Dependency dep1 = new Dependency(jcas, 0, 8);
        dep1.setGovernor(spanThis);
        dep1.setDependent(spanIs);
        dep1.addToIndexes();

        Dependency dep2 = new Dependency(jcas, 0, 10);
        dep2.setGovernor(spanA);
        dep2.setDependent(spanIs);
        dep2.addToIndexes();

        List<LogMessage> messages = new ArrayList<>();

        boolean result = check.check(null, jcas.getCas(), messages);

        messages.forEach(System.out::println);

        assertFalse(result);
    }

    @Test
    public void testOK() throws Exception {

    }


}
