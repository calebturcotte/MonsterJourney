package com.application.monsterjourney;

import java.util.Random;

public class CodeNameGenerator {
    /**
     * Utility class to generate Random names
     */
    private static final String[] COLORS =
            new String[] {
                    "Red",
                    "Orange",
                    "Yellow",
                    "Green",
                    "Blue",
                    "Indigo",
                    "Violet",
                    "Purple",
                    "Lavender",
                    "Fuchsia",
                    "Plum",
                    "Orchid",
                    "Magenta",
            };

    private static final String[] TREATS =
            new String[] {
                    "Alpha",
                    "Beta",
                    "Cupcake",
                    "Donut",
                    "Eclair",
                    "Froyo",
                    "Gingerbread",
                    "Honeycomb",
                    "Ice Cream Sandwich",
                    "Jellybean",
                    "Kit Kat",
                    "Lollipop",
                    "Marshmallow",
                    "Nougat",
                    "Oreo",
                    "Pie"
            };

    private static final Random generator = new Random();

    private CodeNameGenerator() {}

    /** Generate a random Android agent codename */
    public static String generate() {
        String color = COLORS[generator.nextInt(COLORS.length)];
        String treat = TREATS[generator.nextInt(TREATS.length)];
        return color + " " + treat;
    }
}
