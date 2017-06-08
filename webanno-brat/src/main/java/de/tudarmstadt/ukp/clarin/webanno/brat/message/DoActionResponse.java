/*
 * Copyright 2014
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
package de.tudarmstadt.ukp.clarin.webanno.brat.message;

/**
 * Response for the {@code doAction} command.
 *
 * This command is part of WebAnno and not contained in the original brat.
 */
public class DoActionResponse
    extends AjaxResponse
{
    public static final String COMMAND = "doAction";

    public DoActionResponse()
    {
        super(COMMAND);
    }

    public static boolean is(String aCommand)
    {
        return COMMAND.equals(aCommand);
    }
}
