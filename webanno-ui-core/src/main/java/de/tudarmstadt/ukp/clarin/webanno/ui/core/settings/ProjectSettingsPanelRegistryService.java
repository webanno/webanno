/*
 * Copyright 2017
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
package de.tudarmstadt.ukp.clarin.webanno.ui.core.settings;

import java.io.Serializable;
import java.util.List;

import org.apache.wicket.markup.html.panel.Panel;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;

public interface ProjectSettingsPanelRegistryService
{
    static final String SERVICE_NAME = "projectSettingsPanelRegistryService";
    
    List<ProjectSettingsPanelRegistryService.ProjectSettingsPanelDecl> getPanels();

    public static class ProjectSettingsPanelDecl
        implements Serializable
    {
        private static final long serialVersionUID = -2464913342442260640L;
        
        public Condition condition;
        public String label;
        public Class<? extends Panel> panel;
        public int prio;
    }

    @FunctionalInterface
    public static interface Condition
        extends Serializable
    {
        boolean applies(Project aProject, boolean aExportInProgress);
    }
}
