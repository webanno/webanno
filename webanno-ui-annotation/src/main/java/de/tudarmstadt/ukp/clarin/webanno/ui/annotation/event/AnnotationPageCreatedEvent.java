/*
 * Copyright 2020
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
package de.tudarmstadt.ukp.clarin.webanno.ui.annotation.event;

import org.springframework.context.ApplicationEvent;


public class AnnotationPageCreatedEvent
    extends ApplicationEvent
{
    private static final long serialVersionUID = -1766021852410273014L;
    private final String username;
    private final long projectId;

    public AnnotationPageCreatedEvent(Object aSource, String aUser, long aProjectId)
    {
        super(aSource);
        username = aUser;
        projectId = aProjectId;
    }

    public String getUsername()
    {
        return username;
    }

    public Long getProjectId()
    {
        return projectId;
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append("AnnotationPageCreatedEvent [");
        builder.append("user=");
        builder.append(username);
        builder.append(", projectId=");
        builder.append(projectId);
        builder.append("]");
        return builder.toString();
    }
}
