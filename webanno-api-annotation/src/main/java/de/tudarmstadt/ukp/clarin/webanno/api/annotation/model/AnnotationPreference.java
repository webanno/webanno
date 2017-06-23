/*
 * Copyright 2012
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
package de.tudarmstadt.ukp.clarin.webanno.api.annotation.model;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.coloring.ColoringStrategy.ColoringStrategyType;

/**
 * This is a class representing the bean objects to store users preference of annotation settings
 * such as annotation layers, number of sentence to display at a time, visibility of lemma and
 * whether to allow auto page scrolling.
 *
 */
public class AnnotationPreference
    implements Serializable
{
    private static final long serialVersionUID = 2202236699782758271L;

    public static final int FONT_SIZE_MIN = 10;
    public static final int FONT_SIZE_MAX = 17;
    public static final int FONT_SIZE_DEFAULT = 13;
    
    public static final int SIDEBAR_SIZE_MIN = 10;
    public static final int SIDEBAR_SIZE_MAX = 50;
    public static final int SIDEBAR_SIZE_DEFAULT = 20;
    
    // Id of annotation layers, to be stored in the properties file comma separated: 12, 34,....
    private List<Long> annotationLayers;

    private int windowSize;

    private int curationWindowSize = 10;

    private boolean scrollPage = true;
    
    // if a default layer is to be set
    private boolean rememberLayer;
    
//    // determine if static color for annotations will be used or we shall
//    // dynamically generate one
    private boolean staticColor = true;
    
	private Map<Long, ColoringStrategyType> colorPerLayer;
    
    private int sidebarSize;
    private int fontSize;
    
    private String editor;

    public List<Long> getAnnotationLayers()
    {
        return annotationLayers;
    }

    public void setAnnotationLayers(List<Long> aAnnotationLayers)
    {
        annotationLayers = aAnnotationLayers;
    }

    /**
     * The number of sentences to be displayed at a time
     */
    public int getWindowSize()
    {
        return windowSize;
    }

    /**
     * The number of sentences to be displayed at a time
     */
    public void setWindowSize(int aWindowSize)
    {
        windowSize = aWindowSize;
    }

    /**
     * Get the number of sentences curation window display at the left side.
     */
    public int getCurationWindowSize()
    {
        return curationWindowSize;
    }

    /**
     * set the number of sentences curation window display at the left side
     *
     */
    public void setCurationWindowSize(int curationWindowSize)
    {
        this.curationWindowSize = curationWindowSize;
    }

    /**
     * Used to enable/disable auto-scrolling while annotation
     */
    public boolean isScrollPage()
    {
        return scrollPage;
    }

    /**
     * Used to enable/disable auto-scrolling while annotation
     */
    public void setScrollPage(boolean aScrollPage)
    {
        scrollPage = aScrollPage;
    }

    /**
     * 
     * @return
     */
    public boolean isRememberLayer()
    {
        return rememberLayer;
    }

    /**
     * 
     * @param aRememberLayer
     */
    public void setRememberLayer(boolean aRememberLayer)
    {
        rememberLayer = aRememberLayer;
    }

    /**
     * 
     * @return
     */
    public Map<Long, ColoringStrategyType> getColorPerLayer() 
    {
		return colorPerLayer;
	}

    /**
     * 
     * @param colorPerLayer
     */
	public void setColorPerLayer(Map<Long, ColoringStrategyType> colorPerLayer) 
	{
		this.colorPerLayer = colorPerLayer;
	}

    public boolean isStaticColor()
    {
        return staticColor;
    }

    public void setStaticColor(boolean staticColor)
    {
        this.staticColor = staticColor;
    }

    public int getSidebarSize()
    {
        if (sidebarSize < SIDEBAR_SIZE_MIN || sidebarSize > SIDEBAR_SIZE_MAX) {
            return SIDEBAR_SIZE_DEFAULT;
        }
        else {
            return sidebarSize;
        }
    }

    public void setSidebarSize(int aSidebarSize)
    {
        if (aSidebarSize > SIDEBAR_SIZE_MAX) {
            sidebarSize = SIDEBAR_SIZE_MAX;
        }
        else if (aSidebarSize < SIDEBAR_SIZE_MIN) {
            sidebarSize = SIDEBAR_SIZE_MIN;
        }
        else {
            sidebarSize = aSidebarSize;
        }
    }
    
    public int getFontSize()
    {
        if (fontSize < FONT_SIZE_MIN || fontSize > FONT_SIZE_MAX) {
            return FONT_SIZE_DEFAULT;
        }
        else {
            return fontSize;
        }
    }

    public void setFontSize(int aFontSize)
    {
        if (aFontSize > FONT_SIZE_MAX) {
            fontSize = FONT_SIZE_MAX;
        }
        else if (aFontSize < FONT_SIZE_MIN) {
            fontSize = FONT_SIZE_MIN;
        }
        else {
            fontSize = aFontSize;
        }
    }
    
    public String getEditor()
    {
        return editor;
    }
    
    public void setEditor(String aEditor)
    {
        editor = aEditor;
    }
}
