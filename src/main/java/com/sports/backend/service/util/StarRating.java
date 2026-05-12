package com.sports.backend.service.util;

/**
 *  0        → 0 ★
 *  1–2      → 1 ★
 *  3–5      → 2 ★
 *  6–10     → 3 ★
 *  11–20    → 4 ★
 *  21+      → 5 ★
 */
public final class StarRating {

    private StarRating() { }

    public static int calculate(long favoriteCount) {
        if (favoriteCount <= 0)  return 0;
        if (favoriteCount <= 2)  return 1;
        if (favoriteCount <= 5)  return 2;
        if (favoriteCount <= 10) return 3;
        if (favoriteCount <= 20) return 4;
        return 5;
    }
}
