package com.francium.profiler.visualize;

import com.francium.api.PublicApi;
import com.francium.profiler.report.ProfilerReport;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 效能報告視覺化引擎。
 *
 * 把 ProfilerReport 的 JSON 輸出變成一個漂亮的、
 * 可以在瀏覽器中互動瀏覽的單頁應用。
 *
 * 使用方式:
 * <pre>
 *   ProfilerReport report = new ProfilerReport(memoryManager, "My 200-Mod Pack");
 *   report.capture();  // 擷取幾次快照
 *   report.capture();
 *   report.saveTo(dir);
 *
 *   // 啟動視覺化瀏覽器
 *   ReportViewer.open(dir.resolve("francium-profiler-report.json"));
 * </pre>
 *
 * 不需要伺服器、不需要 npm、不需要安裝任何東西。
 * 就是一個 HTML 檔案，雙擊打開就能看。
 */
@PublicApi
public class ReportViewer {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReportViewer.class);

    private static final String TEMPLATE_PATH = "/francium/profiler/dashboard.html";

    /**
     * 生成一個完全獨立的 HTML dashboard，
     * 內嵌 JSON 數據，雙擊即可在瀏覽器中查看。
     *
     * @param jsonReport  ProfilerReport.toJson() 的輸出
     * @param outputPath  輸出的 HTML 檔案路徑
     */
    public static void generateStandalone(String jsonReport, Path outputPath) throws IOException {
        // Read dashboard template from classpath
        String template;
        try (var in = ReportViewer.class.getResourceAsStream(TEMPLATE_PATH)) {
            if (in == null) {
                template = getFallbackTemplate();
            } else {
                template = new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            }
        }

        // Inject JSON data into the template
        String dashboard = template.replace(
            "/* __FRANCIUM_PROFILER_DATA__ */",
            "const PROFILER_DATA = " + jsonReport + ";"
        );

        Files.createDirectories(outputPath.getParent());
        Files.writeString(outputPath, dashboard);

        LOGGER.info("Fr Visualizer: Dashboard saved to " + outputPath.toAbsolutePath());
    }

    /**
     * 從 ProfilerReport 物件直接生成 dashboard。
     */
    public static void generateFrom(ProfilerReport report, Path outputPath) throws IOException {
        generateStandalone(report.toJson(), outputPath);
    }

    /**
     * 在預設瀏覽器中開啟 dashboard。
     *
     * @param dashboardPath  HTML dashboard 檔案路徑
     */
    public static void open(Path dashboardPath) throws IOException {
        if (!Files.exists(dashboardPath)) {
            throw new IOException("Dashboard file not found: " + dashboardPath);
        }

        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            Desktop.getDesktop().browse(dashboardPath.toUri());
            LOGGER.info("Fr Visualizer: Opening dashboard in browser...");
        } else {
            LOGGER.info("Fr Visualizer: Dashboard ready at " + dashboardPath.toAbsolutePath());
            LOGGER.info("  Open this file in your browser to view the report.");
        }
    }

    /**
     * 一鍵生成 &amp; 開啟。
     */
    public static void show(ProfilerReport report, Path outputDir) throws IOException {
        Path htmlPath = outputDir.resolve("francium-dashboard-" +
            Instant.now().toString().replace(":", "-") + ".html");
        generateFrom(report, htmlPath);
        open(htmlPath);
    }

    /**
     * 備用模板：當 classpath 中的 dashboard.html 不可用時。
     */
    private static String getFallbackTemplate() {
        return """
        <!DOCTYPE html>
        <html lang="zh-TW">
        <head>
        <meta charset="UTF-8">
        <title>Francium Profiler Dashboard</title>
        <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body { font-family: system-ui, sans-serif; background: #0d1117; color: #c9d1d9; padding: 2rem; max-width: 1200px; margin: 0 auto; }
        h1 { color: #58a6ff; margin-bottom: 2rem; text-align: center; }
        .cards { display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: 1rem; margin-bottom: 2rem; }
        .card { background: #161b22; border: 1px solid #30363d; border-radius: 8px; padding: 1.5rem; text-align: center; }
        .card .value { font-size: 2rem; font-weight: bold; color: #58a6ff; }
        .card .label { font-size: 0.85rem; color: #8b949e; margin-top: 0.5rem; }
        table { width: 100%; border-collapse: collapse; margin-top: 1rem; }
        th, td { padding: 0.75rem; text-align: left; border-bottom: 1px solid #21262d; }
        th { color: #8b949e; font-size: 0.8rem; text-transform: uppercase; }
        footer { text-align: center; color: #484f58; margin-top: 3rem; padding-top: 2rem; border-top: 1px solid #21262d; }
        footer a { color: #58a6ff; }
        </style>
        </head>
        <body>
        <h1>🧪 Francium Profiler Dashboard</h1>
        <p style="text-align:center;color:#8b949e;margin-bottom:2rem;">
        Dashboard template not found. Using fallback.<br>
        Please rebuild francium-profiler with dashboard.html in resources.
        </p>
        <div id="app"><p>Loading profiler data...</p></div>
        <script>
        /* __FRANCIUM_PROFILER_DATA__ */
        document.addEventListener('DOMContentLoaded', () => {
            if (typeof PROFILER_DATA === 'undefined') {
                document.getElementById('app').innerHTML =
                    '<p style="color:#f85149;">No profiler data found. Run ProfilerReport first.</p>';
                return;
            }
            const d = PROFILER_DATA;
            document.getElementById('app').innerHTML =
                '<div class="cards">' +
                `<div class="card"><div class="value">${d.memory?.heapUsedMB || '?'} MB</div><div class="label">堆記憶體</div></div>` +
                `<div class="card"><div class="value">${d.memory?.gcCount || '?'}</div><div class="label">GC 次數</div></div>` +
                `<div class="card"><div class="value">${d.mods?.length || '?'}</div><div class="label">已載入模組</div></div>` +
                '</div>' +
                '<table><tr><th>模組</th><th>估算記憶體</th><th>類別數</th><th>載入時間</th></tr>' +
                (d.mods || []).map(m =>
                    `<tr><td>${m.id}</td><td>${m.estimatedKB || '?'} KB</td><td>${m.classes || '?'}</td><td>${m.loadTimeMs || '?'}ms</td></tr>`
                ).join('') +
                '</table>';
        });
        </script>
        <footer>
            <p>Generated by <a href="https://github.com/stanley-1028/francium-loader">Francium Mod Loader</a> Profiler</p>
            <p>「不要手動管理記憶體，讓 Francium 替你打工。」</p>
        </footer>
        </body>
        </html>
        """;
    }
}
