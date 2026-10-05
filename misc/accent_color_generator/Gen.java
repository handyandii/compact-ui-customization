/*
 *     Copyright (C) 2026 The Gramophone contributors
 *
 *     Gramophone is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Gramophone is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

import com.google.android.material.color.utilities.*;
import java.util.*;
import java.nio.file.*;

/** Generates Material 3 light/dark color resources + theme overlays for the accent presets. */
public class Gen {
    public static void main(String[] a) throws Exception {
        String res = a[0];
        LinkedHashMap<String, Integer> presets = new LinkedHashMap<>();
        presets.put("red", 0xFFE53935);
        presets.put("pink", 0xFFEC407A);
        presets.put("purple", 0xFF8E24AA);
        presets.put("indigo", 0xFF3F51B5);
        presets.put("blue", 0xFF1E88E5);
        presets.put("teal", 0xFF00897B);
        presets.put("green", 0xFF43A047);
        presets.put("yellow", 0xFFFDD835);
        presets.put("orange", 0xFFFB8C00);
        presets.put("grey", 0xFF9E9E9E);

        MaterialDynamicColors m = new MaterialDynamicColors();
        Object[][] attrs = {
            {"colorPrimary", "primary", m.primary()},
            {"colorOnPrimary", "onPrimary", m.onPrimary()},
            {"colorPrimaryContainer", "primaryContainer", m.primaryContainer()},
            {"colorOnPrimaryContainer", "onPrimaryContainer", m.onPrimaryContainer()},
            {"colorSecondary", "secondary", m.secondary()},
            {"colorOnSecondary", "onSecondary", m.onSecondary()},
            {"colorSecondaryContainer", "secondaryContainer", m.secondaryContainer()},
            {"colorOnSecondaryContainer", "onSecondaryContainer", m.onSecondaryContainer()},
            {"colorTertiary", "tertiary", m.tertiary()},
            {"colorOnTertiary", "onTertiary", m.onTertiary()},
            {"colorTertiaryContainer", "tertiaryContainer", m.tertiaryContainer()},
            {"colorOnTertiaryContainer", "onTertiaryContainer", m.onTertiaryContainer()},
            {"colorError", "error", m.error()},
            {"colorOnError", "onError", m.onError()},
            {"colorErrorContainer", "errorContainer", m.errorContainer()},
            {"colorOnErrorContainer", "onErrorContainer", m.onErrorContainer()},
            {"android:colorBackground", "background", m.background()},
            {"colorOnBackground", "onBackground", m.onBackground()},
            {"colorSurface", "surface", m.surface()},
            {"colorOnSurface", "onSurface", m.onSurface()},
            {"colorSurfaceVariant", "surfaceVariant", m.surfaceVariant()},
            {"colorOnSurfaceVariant", "onSurfaceVariant", m.onSurfaceVariant()},
            {"colorOutline", "outline", m.outline()},
            {"colorOutlineVariant", "outlineVariant", m.outlineVariant()},
            {"colorSurfaceInverse", "inverseSurface", m.inverseSurface()},
            {"colorOnSurfaceInverse", "inverseOnSurface", m.inverseOnSurface()},
            {"colorPrimaryInverse", "inversePrimary", m.inversePrimary()},
            {"colorPrimaryFixed", "primaryFixed", m.primaryFixed()},
            {"colorOnPrimaryFixed", "onPrimaryFixed", m.onPrimaryFixed()},
            {"colorPrimaryFixedDim", "primaryFixedDim", m.primaryFixedDim()},
            {"colorOnPrimaryFixedVariant", "onPrimaryFixedVariant", m.onPrimaryFixedVariant()},
            {"colorSecondaryFixed", "secondaryFixed", m.secondaryFixed()},
            {"colorOnSecondaryFixed", "onSecondaryFixed", m.onSecondaryFixed()},
            {"colorSecondaryFixedDim", "secondaryFixedDim", m.secondaryFixedDim()},
            {"colorOnSecondaryFixedVariant", "onSecondaryFixedVariant", m.onSecondaryFixedVariant()},
            {"colorTertiaryFixed", "tertiaryFixed", m.tertiaryFixed()},
            {"colorOnTertiaryFixed", "onTertiaryFixed", m.onTertiaryFixed()},
            {"colorTertiaryFixedDim", "tertiaryFixedDim", m.tertiaryFixedDim()},
            {"colorOnTertiaryFixedVariant", "onTertiaryFixedVariant", m.onTertiaryFixedVariant()},
            {"colorSurfaceDim", "surfaceDim", m.surfaceDim()},
            {"colorSurfaceBright", "surfaceBright", m.surfaceBright()},
            {"colorSurfaceContainerLowest", "surfaceContainerLowest", m.surfaceContainerLowest()},
            {"colorSurfaceContainerLow", "surfaceContainerLow", m.surfaceContainerLow()},
            {"colorSurfaceContainer", "surfaceContainer", m.surfaceContainer()},
            {"colorSurfaceContainerHigh", "surfaceContainerHigh", m.surfaceContainerHigh()},
            {"colorSurfaceContainerHighest", "surfaceContainerHighest", m.surfaceContainerHighest()},
        };

        String gen = "<!-- GENERATED with Material color utilities (SchemeContent) from the seed colors in\n" +
                     "     misc/accent_color_generator/Gen.java (keep in sync with AccentColors.kt). See compact_device_screen_support.md before editing by hand. -->\n";
        for (boolean dark : new boolean[]{false, true}) {
            StringBuilder sb = new StringBuilder("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" + gen + "<resources>\n");
            for (Map.Entry<String, Integer> p : presets.entrySet()) {
                DynamicScheme s = new SchemeContent(Hct.fromInt(p.getValue()), dark, 0.0);
                sb.append("    <!-- ").append(p.getKey()).append(String.format(" (seed #%06X) -->\n", p.getValue() & 0xFFFFFF));
                for (Object[] at : attrs) {
                    int argb = ((DynamicColor) at[2]).getArgb(s);
                    sb.append(String.format("    <color name=\"accent_%s_%s\">#%06X</color>\n",
                            p.getKey(), at[1], argb & 0xFFFFFF));
                }
            }
            sb.append("</resources>\n");
            Files.writeString(Paths.get(res, dark ? "values-night" : "values", "colors_accent_presets.xml"), sb.toString());
        }

        StringBuilder th = new StringBuilder("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" + gen +
            "<!-- Theme overlays applied on top of the app theme by BaseActivity for the \"Color theme\"\n" +
            "     setting. Light/dark colors come from values/ and values-night/. -->\n<resources>\n");
        for (String name : presets.keySet()) {
            String cap = name.substring(0, 1).toUpperCase() + name.substring(1);
            th.append("\n    <style name=\"ThemeOverlay.Gramophone.Accent.").append(cap).append("\" parent=\"\">\n");
            for (Object[] at : attrs) {
                th.append(String.format("        <item name=\"%s\">@color/accent_%s_%s</item>\n", at[0], name, at[1]));
            }
            th.append("    </style>\n");
        }
        th.append("</resources>\n");
        Files.writeString(Paths.get(res, "values", "themes_accent_presets.xml"), th.toString());
    }
}
