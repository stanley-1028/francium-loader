package com.francium.forge.adapter;

/**
 * Forge 模組中繼資料
 * 
 * 從 mcmod.info 或 mods.toml 中解析出的模組資訊
 * 
 * @author Francium Team
 * @since 2.5.0
 */
public class ForgeModMetadata {
    
    /** 模組 ID */
    private String modId;
    
    /** 模組名稱 */
    private String name;
    
    /** 版本 */
    private String version;
    
    /** 描述 */
    private String description;
    
    /** 作者列表 */
    private String[] authors;
    
    /** 主類別 */
    private String modClass;
    
    /** Minecraft 版本範圍 */
    private String mcVersion;
    
    /** Forge 版本範圍 */
    private String forgeVersion;
    
    /** 依賴列表 */
    private Dependency[] dependencies;
    
    /** 模組來源檔案路徑 */
    private String sourcePath;
    
    /** 模組格式版本（1: mcmod.info, 2: mods.toml） */
    private int formatVersion;
    
    /**
     * 依賴資訊
     */
    public static class Dependency {
        private String modId;
        private String versionRange;
        private boolean mandatory;
        private String ordering;
        private String side;
        
        public Dependency() {}
        
        public Dependency(String modId, String versionRange, boolean mandatory) {
            this.modId = modId;
            this.versionRange = versionRange;
            this.mandatory = mandatory;
        }
        
        public String getModId() { return modId; }
        public void setModId(String modId) { this.modId = modId; }
        
        public String getVersionRange() { return versionRange; }
        public void setVersionRange(String versionRange) { this.versionRange = versionRange; }
        
        public boolean isMandatory() { return mandatory; }
        public void setMandatory(boolean mandatory) { this.mandatory = mandatory; }
        
        public String getOrdering() { return ordering; }
        public void setOrdering(String ordering) { this.ordering = ordering; }
        
        public String getSide() { return side; }
        public void setSide(String side) { this.side = side; }
        
        @Override
        public String toString() {
            return modId + "@" + versionRange + (mandatory ? " (required)" : " (optional)");
        }
    }
    
    public ForgeModMetadata() {}
    
    public String getModId() { return modId; }
    public void setModId(String modId) { this.modId = modId; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String[] getAuthors() { return authors; }
    public void setAuthors(String[] authors) { this.authors = authors; }
    
    public String getModClass() { return modClass; }
    public void setModClass(String modClass) { this.modClass = modClass; }
    
    public String getMcVersion() { return mcVersion; }
    public void setMcVersion(String mcVersion) { this.mcVersion = mcVersion; }
    
    public String getForgeVersion() { return forgeVersion; }
    public void setForgeVersion(String forgeVersion) { this.forgeVersion = forgeVersion; }
    
    public Dependency[] getDependencies() { return dependencies; }
    public void setDependencies(Dependency[] dependencies) { this.dependencies = dependencies; }
    
    public String getSourcePath() { return sourcePath; }
    public void setSourcePath(String sourcePath) { this.sourcePath = sourcePath; }
    
    public int getFormatVersion() { return formatVersion; }
    public void setFormatVersion(int formatVersion) { this.formatVersion = formatVersion; }
    
    /**
     * 檢查是否為有效的 Forge 模組
     */
    public boolean isValid() {
        return modId != null && !modId.isEmpty() 
            && version != null && !version.isEmpty();
    }
    
    @Override
    public String toString() {
        return modId + " " + version + " (" + name + ")";
    }
}
