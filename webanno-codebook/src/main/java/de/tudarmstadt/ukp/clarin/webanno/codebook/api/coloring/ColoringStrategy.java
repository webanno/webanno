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
package de.tudarmstadt.ukp.clarin.webanno.codebook.api.coloring;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.VID;

public abstract class ColoringStrategy
{
    

    public static String getCodebookAnnotationStyle(int aOrder) {
        String[] colors = ColoringStrategy.PALETTE_CODEBOOK;
        String codeColor = colors[(aOrder - 1) % colors.length];
        Color bgcolor = getBgColor();
        String bgcolorHex = String.format("#%02x%02x%02x", bgcolor.getRed(), bgcolor.getGreen(),
                bgcolor.getBlue());

        return "color:" + codeColor + ";font-weight: bold;" + "background-color:" + bgcolorHex
                + ";";
    }
    
    public static String getCodebookDiffColor(boolean aHasDiff) {

        if (aHasDiff) {

            return "background-color: #eea7a7;";
        } else {

            return "background-color: #a0cc9c;";
        }
    }
    
    public static String getCodebookDiffColor(boolean aHasDiff, boolean isAddressed) {


        if (aHasDiff && !isAddressed) {

            return "background-color: #A9E2F3;";
        } 

        else if (aHasDiff && isAddressed) {

            return "background-color: #eea7a7;";
        } 

        else {

            return "background-color: #a0cc9c;";
        }
    }
    
    
    public static String getCodebookFgStyle(int aOrder) {
        String[] colors = ColoringStrategy.PALETTE_CODEBOOK;
        String codeColor = aOrder == 0 ? colors[0]
                : colors[(aOrder - 1) % colors.length];
        return "color:" + codeColor + ";font-weight: bold;";
    }
    
    public static String getCodebookBgStyle() {
        Color bgcolor = getBgColor();
        String bgcolorHex = String.format("#%02x%02x%02x", bgcolor.getRed(), bgcolor.getGreen(),
                bgcolor.getBlue());

        return "background-color:" + bgcolorHex + ";";
    }

    private static Color getBgColor() {
        return  new Color(182, 178, 178);
    }
    
    /**
     * Filter out too light colors from the palette - those that do not show propely on a ligth
     * background. The threshold controls what to filter.
     *
     * @param aPalette
     *            the palette.
     * @param aThreshold
     *            the lightness threshold (0 = black, 255 = white)
     * @return the filtered palette.
     */
    public static String[] filterLightColors(String[] aPalette, int aThreshold)
    {
        List<String> filtered = new ArrayList<>();
        for (String color : aPalette) {
            if (!isTooLight(color, aThreshold)) {
                filtered.add(color);
            }
        }
        return filtered.toArray(new String[filtered.size()]);
    }
   
    
    public static boolean isTooLight(String aColor, int aThreshold)
    {
        // http://24ways.org/2010/calculating-color-contrast/
        // http://stackoverflow.com/questions/11867545/change-text-color-based-on-brightness-of-the-covered-background-area
        int r = Integer.valueOf(aColor.substring(1, 3), 16);
        int g = Integer.valueOf(aColor.substring(3, 5), 16);
        int b = Integer.valueOf(aColor.substring(5, 7), 16);
        int yiq = ((r * 299) + (g * 587) + (b * 114)) / 1000;
        return yiq > aThreshold;
    }

    private final static int LIGHTNESS_FILTER_THRESHOLD = 180;

    public final static String DISABLED = "#bebebe";

    public final static String[] PALETTE_CODEBOOK = { "#000099", "#0E1FF2", "#031D4F", "#074C03",
            "#311E1E", "#5A2618", "#9B2DA4", "#331A3F", "#683A9A", "#ff7f00",
            "#DF5D11", "#e31a1c" };

    public final static String[] PALETTE_CODEBOOK_FILTERED = filterLightColors(PALETTE_CODEBOOK,
            LIGHTNESS_FILTER_THRESHOLD);
    
    public abstract String getColor(VID aVid, String aLabel);
}
