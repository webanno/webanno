/*
 * Copyright 2018
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
package de.tudarmstadt.ukp.clarin.webanno.conll;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;
import static org.apache.uima.fit.factory.CollectionReaderFactory.createReaderDescription;

import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.cas.CAS;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.dkpro.core.io.conll.ConllUReader;
import org.dkpro.core.io.conll.ConllUWriter;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.api.format.FormatSupport;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;

@Component
public class ConllUFormatSupport
    implements FormatSupport
{
    public static final String ID = "conllu";
    public static final String NAME = "CoNLL-U";
    
    @Override
    public String getId()
    {
        return ID;
    }

    @Override
    public String getName()
    {
        return NAME;
    }

    @Override
    public boolean isReadable()
    {
        return true;
    }
    
    @Override
    public boolean isWritable()
    {
        return true;
    }

    @Override
    public CollectionReaderDescription getReaderDescription(TypeSystemDescription aTSD)
        throws ResourceInitializationException
    {
        return createReaderDescription(ConllUReader.class);
    }
    
    @Override
    public AnalysisEngineDescription getWriterDescription(Project aProject,
            TypeSystemDescription aTSD, CAS aCAS)
        throws ResourceInitializationException
    {
        // TODO: The backported ConllUWriter should be removed again when DKPro Core 2.0.1 or higher
        // is released with the appropriate fix for the line breaks within sentences
        return createEngineDescription(ConllUWriter.class);
    }
}
