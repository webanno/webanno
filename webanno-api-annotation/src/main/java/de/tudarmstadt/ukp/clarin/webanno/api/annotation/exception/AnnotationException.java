/*
 * Copyright 2013
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
package de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception;

/**
 * throw an exception if the a span annotation is not in the same sentence
 */
public class AnnotationException
    extends Exception
{
    private static final long serialVersionUID = 1280015349963924638L;

    public AnnotationException(String message)
    {
        super(message);
    }

    public AnnotationException()
    {
        super();
    }

    public AnnotationException(String aMessage, Throwable aCause)
    {
        super(aMessage, aCause);
    }

    public AnnotationException(Throwable aCause)
    {
        super(aCause);
    }
}
