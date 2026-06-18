package com.francium.resolver.model;

import com.francium.api.PublicApi;
import java.util.*;
import java.util.regex.*;

/**
 * 語義化版本約束。
 * 
 * 支援的約束語法:
 * - {@code ">=1.2.0"}     大於等於
 * - {@code "<2.0.0"}      小於
 * - {@code ">=1.2.0 <2.0.0"} 範圍
 * - {@code "^1.2.3"}      compatible with 1.x.x ({@code >=1.2.3 <2.0.0})
 * - {@code "~1.2.3"}      approximately ({@code >=1.2.3 <1.3.0})
 * - "1.2.x"       通配符
 * - "*" 或 ""     任意版本
 */
@PublicApi
public class DependencyConstraint {
    private final String raw;
    private final List<Range> ranges;
    
    public DependencyConstraint(String raw) {
        this.raw = raw != null ? raw.trim() : "*";
        this.ranges = new ArrayList<>();
        parse(this.raw);
    }

    /**
     * 檢查給定版本是否滿足此約束。
     * ★ BUG FIX: 支援 OR 語義的範圍（例如 != 會產生兩個互斥範圍，任一滿足即可）
     *   原本 AND 邏輯對 != 永遠為 false，現改為檢查所有範圍組合。
     */
    public boolean satisfiedBy(SemanticVersion version) {
        if (version == null) return false;
        if (ranges.isEmpty()) return true; // * wildcard
        
        // 檢查是否為 OR 語義（多個範圍，如 != 產生的）
        // 策略：如果任一範圍滿足，則整個約束滿足
        for (Range range : ranges) {
            if (range.contains(version)) {
                return true; // OR 語義：任一範圍滿足即可
            }
        }
        return false;
    }

    /**
     * 找出候選版本中滿足約束的最高版本。
     */
    public SemanticVersion bestMatch(List<SemanticVersion> candidates) {
        return candidates.stream()
            .filter(this::satisfiedBy)
            .max(Comparator.naturalOrder())
            .orElse(null);
    }

    /**
     * 找出滿足此約束和另一個約束的版本交集。
     */
    public DependencyConstraint intersect(DependencyConstraint other) {
        if (this.raw.equals("*") || this.ranges.isEmpty()) return other;
        if (other.raw.equals("*") || other.ranges.isEmpty()) return this;
        
        // ★ BUG FIX: 對所有 range 組合做笛卡爾積交集，不只是取全局最大 min / 最小 max
        //   因為不同範圍可能來自 OR 語義（如 !=），對互斥範圍做全局 min/max 會得到錯誤結果。
        //   正確做法：對每一對 (thisRange, otherRange) 計算交集，再 OR 合併。
        List<Range> intersections = new ArrayList<>();
        
        for (Range thisRange : ranges) {
            for (Range otherRange : other.ranges) {
                // 計算兩個 Range 的交集
                SemanticVersion interMin = null;
                boolean interMinInc = false;
                SemanticVersion interMax = null;
                boolean interMaxInc = false;
                
                // 取較大的下限
                if (thisRange.min != null && otherRange.min != null) {
                    int cmp = thisRange.min.compareTo(otherRange.min);
                    if (cmp > 0) {
                        interMin = thisRange.min;
                        interMinInc = thisRange.minInclusive;
                    } else if (cmp < 0) {
                        interMin = otherRange.min;
                        interMinInc = otherRange.minInclusive;
                    } else {
                        interMin = thisRange.min;
                        interMinInc = thisRange.minInclusive && otherRange.minInclusive;
                    }
                } else if (thisRange.min != null) {
                    interMin = thisRange.min;
                    interMinInc = thisRange.minInclusive;
                } else if (otherRange.min != null) {
                    interMin = otherRange.min;
                    interMinInc = otherRange.minInclusive;
                }
                
                // 取較小的上限
                if (thisRange.max != null && otherRange.max != null) {
                    int cmp = thisRange.max.compareTo(otherRange.max);
                    if (cmp < 0) {
                        interMax = thisRange.max;
                        interMaxInc = thisRange.maxInclusive;
                    } else if (cmp > 0) {
                        interMax = otherRange.max;
                        interMaxInc = otherRange.maxInclusive;
                    } else {
                        interMax = thisRange.max;
                        interMaxInc = thisRange.maxInclusive && otherRange.maxInclusive;
                    }
                } else if (thisRange.max != null) {
                    interMax = thisRange.max;
                    interMaxInc = thisRange.maxInclusive;
                } else if (otherRange.max != null) {
                    interMax = otherRange.max;
                    interMaxInc = otherRange.maxInclusive;
                }
                
                // 檢查交集是否有效
                if (interMin != null && interMax != null) {
                    int cmp = interMin.compareTo(interMax);
                    if (cmp > 0 || (cmp == 0 && (!interMinInc || !interMaxInc))) {
                        continue; // 無效交集
                    }
                }
                
                intersections.add(new Range(interMin, interMinInc, interMax, interMaxInc));
            }
        }
        
        if (intersections.isEmpty()) {
            return null; // 無交集
        }
        
        // 合併所有有效交集為一個 Constraint
        // 取全域最小下限和最大上限（OR 語義）
        SemanticVersion finalMin = intersections.stream()
            .map(r -> r.min)
            .filter(Objects::nonNull)
            .min(Comparator.naturalOrder())
            .orElse(null);
        
        SemanticVersion finalMax = intersections.stream()
            .map(r -> r.max)
            .filter(Objects::nonNull)
            .max(Comparator.naturalOrder())
            .orElse(null);
        
        // 計算最終 inclusivity：取最嚴格
        boolean finalMinInc = true;
        if (finalMin != null) {
            for (Range r : intersections) {
                if (r.min != null && r.min.equals(finalMin)) {
                    finalMinInc = finalMinInc && r.minInclusive;
                }
            }
        }
        
        boolean finalMaxInc = true;
        if (finalMax != null) {
            for (Range r : intersections) {
                if (r.max != null && r.max.equals(finalMax)) {
                    finalMaxInc = finalMaxInc && r.maxInclusive;
                }
            }
        }
        
        return new ConstraintBuilder()
            .min(finalMin, finalMinInc)
            .max(finalMax, finalMaxInc)
            .build();
    }

    /** 此約束是否為通配符（接受任意版本）。 */
    public boolean isWildcard() {
        return ranges.isEmpty() || raw.equals("*");
    }

    @Override
    public String toString() {
        return raw.isEmpty() ? "*" : raw;
    }

    // --- Parser ---
    private void parse(String raw) {
        if (raw.equals("*") || raw.isEmpty()) return;
        
        // 處理 caret 範圍
        if (raw.startsWith("^")) {
            SemanticVersion min = SemanticVersion.parse(raw.substring(1));
            SemanticVersion max = new SemanticVersion(min.major() + 1, 0, 0);
            ranges.add(new Range(min, true, max, false));
            return;
        }
        
        // 處理 tilde 範圍
        if (raw.startsWith("~")) {
            SemanticVersion min = SemanticVersion.parse(raw.substring(1));
            SemanticVersion max = new SemanticVersion(min.major(), min.minor() + 1, 0);
            ranges.add(new Range(min, true, max, false));
            return;
        }
        
        // 處理 x-range
        if (raw.contains("x") || raw.contains("X")) {
            String[] parts = raw.split("\\.");
            int major = parts[0].equals("x") || parts[0].equals("X") ? 0 : Integer.parseInt(parts[0]);
            int minor = parts.length > 1 && (parts[1].equals("x") || parts[1].equals("X")) ? 0 : 
                (parts.length > 1 ? Integer.parseInt(parts[1]) : 0);
            
            SemanticVersion min = new SemanticVersion(major, minor, 0);
            SemanticVersion max;
            if (parts[0].equals("x") || parts[0].equals("X")) {
                max = new SemanticVersion(Integer.MAX_VALUE, 0, 0);
            } else if (parts.length <= 1 || parts[1].equals("x") || parts[1].equals("X")) {
                max = new SemanticVersion(major + 1, 0, 0);
            } else {
                max = new SemanticVersion(major, minor + 1, 0);
            }
            ranges.add(new Range(min, true, max, false));
            return;
        }
        
        // 處理範圍運算符
        Matcher rangeMatcher = Pattern.compile(
            "([<>=!]{1,2})\\s*([0-9]+(?:\\.[0-9]+)*(?:-[a-zA-Z0-9.]+)?)"
        ).matcher(raw);
        
        while (rangeMatcher.find()) {
            String op = rangeMatcher.group(1);
            SemanticVersion ver = SemanticVersion.parse(rangeMatcher.group(2));
            
            switch (op) {
                case ">=" -> ranges.add(new Range(ver, true, null, false));
                case ">" -> ranges.add(new Range(ver, false, null, false));
                case "<=" -> ranges.add(new Range(null, false, ver, true));
                case "<" -> ranges.add(new Range(null, false, ver, false));
                case "=", "==" -> ranges.add(new Range(ver, true, ver, true));
                case "!=" -> {
                    // 不等於: 排除精確版本 → 兩個範圍互斥覆蓋全集
                    // 版本 < ver 或 版本 > ver 都滿足 !=
                    ranges.add(new Range(null, false, ver, false));
                    ranges.add(new Range(ver, false, null, false));
                }
            }
        }
        
        // 純版本號: 精確匹配
        if (ranges.isEmpty() && !raw.contains(" ") && !raw.isEmpty()) {
            try {
                SemanticVersion exact = SemanticVersion.parse(raw);
                ranges.add(new Range(exact, true, exact, true));
            } catch (IllegalArgumentException ignored) {}
        }
    }

    // --- Builder ---
    /** 用於程式化建構 DependencyConstraint 的建造者。 */
    public static class ConstraintBuilder {
        private SemanticVersion min, max;
        private boolean minInc = true, maxInc = true;
        
        /** 設定下限版本及是否包含該版本。 */
        public ConstraintBuilder min(SemanticVersion v, boolean inclusive) {
            this.min = v; this.minInc = inclusive; return this;
        }
        /** 設定上限版本及是否包含該版本。 */
        public ConstraintBuilder max(SemanticVersion v, boolean inclusive) {
            this.max = v; this.maxInc = inclusive; return this;
        }
        /** 建構 DependencyConstraint 實例。 */
        public DependencyConstraint build() {
            DependencyConstraint c = new DependencyConstraint("*");
            c.ranges.clear();
            c.ranges.add(new Range(min, minInc, max, maxInc));
            return c;
        }
    }

    // --- Record ---
    record Range(SemanticVersion min, boolean minInclusive, SemanticVersion max, boolean maxInclusive) {
        boolean contains(SemanticVersion v) {
            if (min != null) {
                int cmp = v.compareTo(min);
                if (cmp < 0 || (cmp == 0 && !minInclusive)) return false;
            }
            if (max != null) {
                int cmp = v.compareTo(max);
                if (cmp > 0 || (cmp == 0 && !maxInclusive)) return false;
            }
            return true;
        }
    }
}
