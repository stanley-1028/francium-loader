package com.francium.resolver.model;

import com.francium.api.PublicApi;
import java.util.Objects;
import java.util.regex.*;

/**
 * 語義化版本 (Semantic Versioning 2.0)。
 * 
 * 格式: MAJOR.MINOR.PATCH[-PRERELEASE][+BUILD]
 * 例子: 1.20.4, 2.0.0-beta.1, 1.21.0-pre3+mc1.21
 */
@PublicApi
public class SemanticVersion implements Comparable<SemanticVersion> {
    private final int major;
    private final int minor;
    private final int patch;
    private final String preRelease; // null if none
    private final String build;      // null if none

    public SemanticVersion(int major, int minor, int patch) {
        this(major, minor, patch, null, null);
    }

    public SemanticVersion(int major, int minor, int patch, String preRelease, String build) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
        this.preRelease = preRelease != null && !preRelease.isEmpty() ? preRelease : null;
        this.build = build != null && !build.isEmpty() ? build : null;
    }

    /**
     * 解析版本字串。
     */
    public static SemanticVersion parse(String version) {
        if (version == null || version.isEmpty()) {
            throw new IllegalArgumentException("Version cannot be null/empty");
        }
        
        // Strip leading 'v' or 'V'
        if (version.startsWith("v") || version.startsWith("V")) {
            version = version.substring(1);
        }
        
        // Split off build metadata
        String build = null;
        int buildIdx = version.indexOf('+');
        if (buildIdx >= 0) {
            build = version.substring(buildIdx + 1);
            version = version.substring(0, buildIdx);
        }
        
        // Split off pre-release
        String preRelease = null;
        int preIdx = version.indexOf('-');
        if (preIdx >= 0) {
            preRelease = version.substring(preIdx + 1);
            version = version.substring(0, preIdx);
        }
        
        // Parse MAJOR.MINOR.PATCH
        String[] parts = version.split("\\.");
        if (parts.length < 1) {
            throw new IllegalArgumentException("Invalid version: " + version);
        }
        
        int major = Integer.parseInt(parts[0]);
        int minor = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
        int patch = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;
        
        return new SemanticVersion(major, minor, patch, preRelease, build);
    }

    /**
     * 嘗試解析，失敗返回 null。
     */
    public static SemanticVersion tryParse(String version) {
        try {
            return parse(version);
        } catch (Exception e) {
            return null;
        }
    }

    // --- Incremental versions (for range boundaries) ---
    /** 返回下一個主版本號 (major+1, 0, 0)，用於範圍邊界計算。 */
    public SemanticVersion nextMajor() { return new SemanticVersion(major + 1, 0, 0); }
    /** 返回下一個次版本號 (major, minor+1, 0)。 */
    public SemanticVersion nextMinor() { return new SemanticVersion(major, minor + 1, 0); }
    /** 返回下一個修訂版號 (major, minor, patch+1)。 */
    public SemanticVersion nextPatch() { return new SemanticVersion(major, minor, patch + 1); }

    // --- Comparison (pre-release < release) ---
    @Override
    public int compareTo(SemanticVersion other) {
        int cmp = Integer.compare(this.major, other.major);
        if (cmp != 0) return cmp;
        
        cmp = Integer.compare(this.minor, other.minor);
        if (cmp != 0) return cmp;
        
        cmp = Integer.compare(this.patch, other.patch);
        if (cmp != 0) return cmp;
        
        // Pre-release comparison
        if (this.preRelease == null && other.preRelease == null) return 0;
        if (this.preRelease == null) return 1;  // no pre-release > pre-release
        if (other.preRelease == null) return -1;
        
        return comparePreRelease(this.preRelease, other.preRelease);
    }

    private int comparePreRelease(String a, String b) {
        String[] partsA = a.split("\\.");
        String[] partsB = b.split("\\.");
        
        int len = Math.min(partsA.length, partsB.length);
        for (int i = 0; i < len; i++) {
            boolean aIsNum = partsA[i].matches("\\d+");
            boolean bIsNum = partsB[i].matches("\\d+");
            
            if (aIsNum && bIsNum) {
                int cmp = Integer.compare(Integer.parseInt(partsA[i]), Integer.parseInt(partsB[i]));
                if (cmp != 0) return cmp;
            } else if (aIsNum) {
                return -1; // numeric < string
            } else if (bIsNum) {
                return 1;
            } else {
                int cmp = partsA[i].compareTo(partsB[i]);
                if (cmp != 0) return cmp;
            }
        }
        return Integer.compare(partsA.length, partsB.length);
    }

    // --- Accessors ---
    /** 返回主版本號 (MAJOR)。 */
    public int major() { return major; }
    /** 返回次版本號 (MINOR)。 */
    public int minor() { return minor; }
    /** 返回修訂版號 (PATCH)。 */
    public int patch() { return patch; }
    /** 返回預發布標籤（若無則為 null）。 */
    public String preRelease() { return preRelease; }
    /** 返回建置元數據（若無則為 null）。 */
    public String build() { return build; }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(major).append('.').append(minor).append('.').append(patch);
        if (preRelease != null) sb.append('-').append(preRelease);
        if (build != null) sb.append('+').append(build);
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof SemanticVersion v)) return false;
        return major == v.major && minor == v.minor && patch == v.patch 
            && Objects.equals(preRelease, v.preRelease);
    }

    @Override
    public int hashCode() {
        return Objects.hash(major, minor, patch, preRelease);
    }
}
