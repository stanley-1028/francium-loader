package com.francium.loader;

import com.francium.api.PublicApi;

/**
 * Francium 全域異常類別，取代裸 throws Exception。
 *
 * 提供具體的錯誤碼與階段分類，讓呼叫端能精準處理不同類型錯誤。
 */
@PublicApi
public class FranciumException extends Exception {

    /** 錯誤發生的生命週期階段。 */
    public enum Phase {
        DISCOVERY,      // 模組掃描階段
        RESOLUTION,     // SAT 依賴解析階段
        BRIDGING,       // AI 版本橋接階段
        LOADING,        // 類別載入階段
        LIFECYCLE,      // 生命週期回呼階段
        VALIDATION,     // 安全驗證階段
        CONFIGURATION,   // 設定階段
        UNKNOWN
    }

    private final Phase phase;
    private final String detail;

    public FranciumException(Phase phase, String message) {
        super(message);
        this.phase = phase;
        this.detail = "";
    }

    public FranciumException(Phase phase, String message, Throwable cause) {
        super(message, cause);
        this.phase = phase;
        this.detail = "";
    }

    public FranciumException(Phase phase, String message, String detail) {
        super(message);
        this.phase = phase;
        this.detail = detail;
    }

    public Phase getPhase() {
        return phase;
    }

    public String getDetail() {
        return detail;
    }

    @Override
    public String toString() {
        return "[Francium:" + phase + "] " + getMessage()
            + (detail.isEmpty() ? "" : " (" + detail + ")");
    }
}
