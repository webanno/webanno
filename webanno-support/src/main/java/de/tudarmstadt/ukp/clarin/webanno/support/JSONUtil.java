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
package de.tudarmstadt.ukp.clarin.webanno.support;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;

import org.apache.commons.io.FileUtils;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.SerializableString;
import com.fasterxml.jackson.core.io.CharacterEscapes;
import com.fasterxml.jackson.core.io.SerializedString;

public class JSONUtil
{
    /**
     * Convert Java objects into JSON format and write it to a file
     *
     * @param aObject
     *            the object.
     * @param aFile
     *            the file
     * @throws IOException
     *             if an I/O error occurs.
     */
    public static void generatePrettyJson(MappingJackson2HttpMessageConverter jsonConverter,
            Object aObject, File aFile)
        throws IOException
    {
        FileUtils.writeStringToFile(aFile, toPrettyJsonString(jsonConverter, aObject), "UTF-8");
    }

    public static void generatePrettyJson(Object aObject, File aFile)
        throws IOException
    {
        FileUtils.writeStringToFile(aFile, toPrettyJsonString(aObject), "UTF-8");
    }

    public static String toPrettyJsonString(MappingJackson2HttpMessageConverter jsonConverter,
            Object aObject)
        throws IOException
    {
        return toJsonString(jsonConverter, true, aObject);
    }

    public static String toJsonString(Object aObject)
        throws IOException
    {
        return toJsonString(getJsonConverter(), false, aObject);
    }
    
    public static String toJsonString(MappingJackson2HttpMessageConverter jsonConverter,
            boolean aPretty, Object aObject)
        throws IOException
    {
        StringWriter out = new StringWriter();

        JsonGenerator jsonGenerator = jsonConverter.getObjectMapper().getFactory()
                .createGenerator(out);
        if (aPretty) {
            jsonGenerator.useDefaultPrettyPrinter();
        }

        jsonGenerator.writeObject(aObject);
        return out.toString();
    }

    public static <T> T fromJsonString(Class<T> aClass, String aJSON)
        throws IOException
    {
        if (aJSON == null) {
            return null;
        }
        else {
            return getJsonConverter().getObjectMapper().readValue(aJSON, aClass);
        }
    }

    public static String toPrettyJsonString(Object aObject)
        throws IOException
    {
        return toPrettyJsonString(getJsonConverter(), aObject);
    }
    public static MappingJackson2HttpMessageConverter getJsonConverter()
    {
        return ApplicationContextProvider.getApplicationContext()
                .getBean(MappingJackson2HttpMessageConverter.class);
    }
    
    public static String toInterpretableJsonString(Object aObject)
        throws IOException
    {
        StringWriter out = new StringWriter();
        JsonGenerator jsonGenerator = JSONUtil.getJsonConverter().getObjectMapper().getFactory()
                .createGenerator(out);
        jsonGenerator.setCharacterEscapes(JavaScriptCharacterEscapes.get());
        jsonGenerator.writeObject(aObject);
        return out.toString();
    }
    
    private static class JavaScriptCharacterEscapes extends CharacterEscapes {
        private static final long serialVersionUID = -2189758484099286957L;
        private final int[] asciiEscapes = standardAsciiEscapesForJSON();
        
        public static final JavaScriptCharacterEscapes INSTANCE = new JavaScriptCharacterEscapes();
        
        public static JavaScriptCharacterEscapes get() {
            return INSTANCE;
        }
        
        @Override
        public SerializableString getEscapeSequence(int aCh)
        {
            switch (aCh) {
            case '\u2028':
                return new SerializedString("\\u2028");
            case '\u2029':
                return new SerializedString("\\u2029");
            default:
                return null;
            }
        }
        
        @Override
        public int[] getEscapeCodesForAscii()
        {
            return asciiEscapes;
        }
    }
}
