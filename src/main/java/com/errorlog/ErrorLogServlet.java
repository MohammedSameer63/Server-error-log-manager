package com.errorlog;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import jakarta.servlet.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

@WebServlet("/logs")
public class ErrorLogServlet extends HttpServlet {

    // ─────────────────────────────────────────────────
    // GET → show the main page with current log state
    // ─────────────────────────────────────────────────
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // Ensure manager exists in session
        getOrCreateManager(request.getSession());
        showPage(request, response, "all", null, null);
    }

    // ─────────────────────────────────────────────────
    // POST → handle button actions from HTML form
    // ─────────────────────────────────────────────────
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");
        HttpSession session = request.getSession();
        ErrorLogManager manager = getOrCreateManager(session);

        String action  = request.getParameter("action");
        String logEntry = request.getParameter("logEntry");
        String sortBy  = request.getParameter("sortBy");

        String view         = "all";
        String successMsg   = null;
        String errorMsg     = null;

        try {
            if ("add".equals(action)) {
                manager.addLog(logEntry);
                successMsg = "Log added: " + logEntry;

            } else if ("showAll".equals(action)) {
                view = "all";

            } else if ("showCritical".equals(action)) {
                view = "critical";

            } else if ("sort".equals(action)) {
                if ("typeAsc".equals(sortBy)) {
                    manager.sortBy(ErrorComparators.byErrorType());
                } else if ("typeDesc".equals(sortBy)) {
                    manager.sortBy(ErrorComparators.byErrorTypeDescending());
                } else if ("id".equals(sortBy)) {
                    manager.sortBy(ErrorComparators.byErrorId());
                }
                successMsg = "Logs sorted successfully.";

            } else if ("clear".equals(action)) {
                manager.clearAll();
                successMsg = "All logs cleared.";
            }

        } catch (IllegalArgumentException ex) {
            errorMsg = ex.getMessage();
        }

        showPage(request, response, view, successMsg, errorMsg);
    }

    // ─────────────────────────────────────────────────
    // Build and send the full HTML page
    // ─────────────────────────────────────────────────
    private void showPage(HttpServletRequest request, HttpServletResponse response,
                          String view, String successMsg, String errorMsg)
            throws IOException {

        ErrorLogManager manager = getOrCreateManager(request.getSession());
        ArrayList<ServerError> displayList = "critical".equals(view)
                ? manager.getCriticalErrors()
                : manager.getAllErrors();

        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();

        out.println(buildHtml(manager, displayList, view, successMsg, errorMsg));
    }

    // ─────────────────────────────────────────────────
    // HTML builder using StringBuffer
    // ─────────────────────────────────────────────────
    private String buildHtml(ErrorLogManager manager,
                              ArrayList<ServerError> displayList,
                              String view,
                              String successMsg,
                              String errorMsg) {

        StringBuffer sb = new StringBuffer();

        sb.append("<!DOCTYPE html><html lang='en'><head>")
          .append("<meta charset='UTF-8'>")
          .append("<meta name='viewport' content='width=device-width, initial-scale=1.0'>")
          .append("<title>Server Error Log Manager</title>")
          .append("<link rel='preconnect' href='https://fonts.googleapis.com'>")
          .append("<link href='https://fonts.googleapis.com/css2?family=JetBrains+Mono:wght@400;500&family=Syne:wght@400;500;600&display=swap' rel='stylesheet'>")
          .append(buildCss())
          .append("</head><body>")
          .append(buildHeader())
          .append("<div class='layout'>")
          .append(buildSidebar(manager))
          .append(buildContent(displayList, view, successMsg, errorMsg))
          .append("</div>")
          .append("</body></html>");

        return sb.toString();
    }

    private String buildCss() {
        return "<style>" +
            "*{box-sizing:border-box;margin:0;padding:0}" +
            ":root{--bg:#0f1117;--bg2:#161820;--bg3:#1e2030;--bg4:#252840;" +
            "--border:#2e3250;--border2:#3d4270;" +
            "--accent:#6c72f8;--accent2:#4ade80;--accent3:#f87171;--accent4:#fbbf24;" +
            "--text:#e2e4f0;--text2:#9198b8;--text3:#5a6080;" +
            "--radius:10px;--font:'Syne',sans-serif;--mono:'JetBrains Mono',monospace}" +
            "body{background:var(--bg);color:var(--text);font-family:var(--font);min-height:100vh}" +
            ".header{background:var(--bg2);border-bottom:1px solid var(--border);padding:14px 24px;display:flex;align-items:center;gap:12px}" +
            ".header-icon{width:34px;height:34px;background:var(--accent);border-radius:9px;display:flex;align-items:center;justify-content:center;flex-shrink:0}" +
            ".header-title{font-size:16px;font-weight:600;letter-spacing:0.02em}" +
            ".header-sub{font-size:12px;color:var(--text2);margin-left:auto;font-family:var(--mono);display:flex;align-items:center;gap:6px}" +
            ".dot{width:7px;height:7px;background:var(--accent2);border-radius:50%)}" +
            ".layout{display:grid;grid-template-columns:300px 1fr;height:calc(100vh - 57px)}" +
            ".sidebar{background:var(--bg2);border-right:1px solid var(--border);overflow-y:auto;display:flex;flex-direction:column}" +
            ".s-sec{padding:16px;border-bottom:1px solid var(--border)}" +
            ".s-label{font-size:10px;font-weight:600;color:var(--text3);letter-spacing:.12em;text-transform:uppercase;margin-bottom:10px}" +
            "input[type=text],select{width:100%;background:var(--bg3);border:1px solid var(--border);color:var(--text);font-family:var(--mono);font-size:12px;padding:9px 12px;border-radius:8px;outline:none;transition:border .2s;margin-bottom:8px}" +
            "input[type=text]:focus,select:focus{border-color:var(--accent)}" +
            "input::placeholder{color:var(--text3)}" +
            ".btn{border:none;cursor:pointer;font-family:var(--font);font-weight:500;font-size:12px;padding:9px 14px;border-radius:8px;transition:all .15s;display:inline-flex;align-items:center;justify-content:center;gap:6px;letter-spacing:.02em;width:100%;margin-bottom:6px}" +
            ".btn-p{background:var(--accent);color:#fff}.btn-p:hover{background:#7c82f9}" +
            ".btn-s{background:rgba(74,222,128,.12);color:var(--accent2);border:1px solid rgba(74,222,128,.2)}.btn-s:hover{background:rgba(74,222,128,.22)}" +
            ".btn-d{background:rgba(248,113,113,.1);color:var(--accent3);border:1px solid rgba(248,113,113,.2)}.btn-d:hover{background:rgba(248,113,113,.2)}" +
            ".btn-w{background:rgba(251,191,36,.1);color:var(--accent4);border:1px solid rgba(251,191,36,.2)}.btn-w:hover{background:rgba(251,191,36,.2)}" +
            ".btn-g{background:var(--bg3);color:var(--text2);border:1px solid var(--border)}.btn-g:hover{border-color:var(--border2)}" +
            ".grid2{display:grid;grid-template-columns:1fr 1fr;gap:6px;margin-bottom:6px}" +
            ".grid2 .btn{margin:0}" +
            ".stats{display:grid;grid-template-columns:1fr 1fr;gap:8px}" +
            ".stat{background:var(--bg3);border:1px solid var(--border);border-radius:8px;padding:10px 12px}" +
            ".stat-n{font-size:22px;font-weight:600;font-family:var(--mono)}" +
            ".stat-l{font-size:10px;color:var(--text2);text-transform:uppercase;letter-spacing:.08em;margin-top:2px}" +
            ".c-total .stat-n{color:var(--accent)}.c-crit .stat-n{color:var(--accent3)}" +
            ".c-err .stat-n{color:#c084fc}.c-warn .stat-n{color:var(--accent4)}" +
            ".content{display:flex;flex-direction:column;overflow:hidden}" +
            ".c-head{padding:12px 20px;border-bottom:1px solid var(--border);background:var(--bg2);display:flex;align-items:center;gap:8px}" +
            ".c-title{font-size:13px;font-weight:500}" +
            ".badge{font-size:10px;font-family:var(--mono);padding:3px 10px;border-radius:20px;font-weight:500}" +
            ".b-all{background:rgba(108,114,248,.12);color:var(--accent);border:1px solid rgba(108,114,248,.2)}" +
            ".b-crit{background:rgba(248,113,113,.1);color:var(--accent3)}" +
            ".log-list{flex:1;overflow-y:auto;padding:16px 20px}" +
            ".log-entry{display:grid;grid-template-columns:36px 90px 1fr;gap:12px;align-items:start;padding:10px 14px;border-radius:9px;border:1px solid transparent;transition:all .15s;margin-bottom:4px}" +
            ".log-entry:hover{background:var(--bg3);border-color:var(--border)}" +
            ".log-id{color:var(--text3);font-size:11px;font-family:var(--mono);padding-top:2px}" +
            ".log-type{display:inline-flex;padding:3px 9px;border-radius:5px;font-size:10px;font-weight:600;letter-spacing:.06em;font-family:var(--mono);width:fit-content}" +
            ".t-CRITICAL{background:rgba(248,113,113,.12);color:#f87171;border:1px solid rgba(248,113,113,.25)}" +
            ".t-ERROR{background:rgba(192,132,252,.1);color:#c084fc;border:1px solid rgba(192,132,252,.2)}" +
            ".t-WARNING{background:rgba(251,191,36,.1);color:#fbbf24;border:1px solid rgba(251,191,36,.2)}" +
            ".t-INFO{background:rgba(108,114,248,.1);color:#818cf8;border:1px solid rgba(108,114,248,.2)}" +
            ".t-UNKNOWN{background:rgba(90,96,128,.15);color:var(--text2);border:1px solid var(--border)}" +
            ".log-msg{color:var(--text);font-size:13px;font-family:var(--mono);padding-top:2px;word-break:break-word}" +
            ".alert{padding:10px 16px;border-radius:8px;font-size:13px;margin:12px 20px 0;}" +
            ".alert-ok{background:rgba(74,222,128,.1);color:var(--accent2);border:1px solid rgba(74,222,128,.2)}" +
            ".alert-err{background:rgba(248,113,113,.1);color:var(--accent3);border:1px solid rgba(248,113,113,.2)}" +
            ".empty{display:flex;flex-direction:column;align-items:center;justify-content:center;flex:1;gap:8px;color:var(--text3);padding:40px}" +
            ".empty svg{opacity:.3}" +
            ".empty-l{font-size:14px}.empty-h{font-size:12px;font-family:var(--mono)}" +
            "</style>";
    }

    private String buildHeader() {
        return "<div class='header'>" +
            "<div class='header-icon'>" +
            "<svg width='18' height='18' viewBox='0 0 24 24' fill='none' stroke='white' stroke-width='2'>" +
            "<path d='M5 12h14M12 5l7 7-7 7'/></svg></div>" +
            "<span class='header-title'>Server Error Log Manager</span>" +
            "<div class='header-sub'><div class='dot' style='width:7px;height:7px;background:#4ade80;border-radius:50%'></div>" +
            "servlet running</div></div>";
    }

    private String buildSidebar(ErrorLogManager manager) {
        StringBuffer sb = new StringBuffer();
        sb.append("<div class='sidebar'>")

          // ── Add Log form
          .append("<div class='s-sec'>")
          .append("<div class='s-label'>Add Log Entry</div>")
          .append("<form method='post' action='/logs'>")
          .append("<input type='text' name='logEntry' placeholder='CRITICAL: Disk full' autocomplete='off' />")
          .append("<button class='btn btn-p' type='submit' name='action' value='add'>+ Add Log</button>")
          .append("</form></div>")

          // ── Actions
          .append("<div class='s-sec'>")
          .append("<div class='s-label'>Actions</div>")
          .append("<div class='grid2'>")
          .append("<form method='post' action='/logs'><button class='btn btn-s' name='action' value='showAll'>Show All</button></form>")
          .append("<form method='post' action='/logs'><button class='btn btn-d' name='action' value='showCritical'>Critical</button></form>")
          .append("</div>")
          .append("<form method='post' action='/logs'>" )
          .append("<button class='btn btn-g' name='action' value='clear' ")
          .append("onclick=\"return confirm('Clear all logs?')\">Clear All Logs</button>")
          .append("</form></div>")

          // ── Sort
          .append("<div class='s-sec'>")
          .append("<div class='s-label'>Sort</div>")
          .append("<form method='post' action='/logs'>")
          .append("<select name='sortBy'>")
          .append("<option value='typeAsc'>Error Type (A → Z)</option>")
          .append("<option value='typeDesc'>Error Type (Z → A)</option>")
          .append("<option value='id'>Error ID</option>")
          .append("</select>")
          .append("<button class='btn btn-w' name='action' value='sort'>Apply Sort</button>")
          .append("</form></div>")

          // ── Stats
          .append("<div class='s-sec'>")
          .append("<div class='s-label'>Statistics</div>")
          .append("<div class='stats'>")
          .append(statCard(manager.getAllErrors().size(), "Total", "c-total"))
          .append(statCard(manager.countByType("CRITICAL"), "Critical", "c-crit"))
          .append(statCard(manager.countByType("ERROR"), "Error", "c-err"))
          .append(statCard(manager.countByType("WARNING"), "Warning", "c-warn"))
          .append("</div></div>")

          .append("</div>");

        return sb.toString();
    }

    private String statCard(int count, String label, String cls) {
        return "<div class='stat " + cls + "'>" +
               "<div class='stat-n'>" + count + "</div>" +
               "<div class='stat-l'>" + label + "</div></div>";
    }

    private String buildContent(ArrayList<ServerError> list,
                                 String view,
                                 String successMsg,
                                 String errorMsg) {
        StringBuffer sb = new StringBuffer();

        sb.append("<div class='content'>")
          .append("<div class='c-head'>")
          .append("<span class='c-title'>Log Output</span>")
          .append("<span class='badge ").append("critical".equals(view) ? "b-crit" : "b-all").append("'>")
          .append("critical".equals(view) ? "critical only" : "all logs")
          .append("</span></div>");

        // Notification messages
        if (successMsg != null) {
            sb.append("<div class='alert alert-ok'>&#10003; ").append(escapeHtml(successMsg)).append("</div>");
        }
        if (errorMsg != null) {
            sb.append("<div class='alert alert-err'>&#9888; ").append(escapeHtml(errorMsg)).append("</div>");
        }

        // Log list
        sb.append("<div class='log-list'>");
        if (list.isEmpty()) {
            sb.append("<div class='empty'>")
              .append("<svg width='40' height='40' viewBox='0 0 24 24' fill='none' stroke='currentColor' stroke-width='1.5'>")
              .append("<path d='M9 17H7A5 5 0 017 7h2M15 7h2a5 5 0 010 10h-2M11 12h2'/></svg>")
              .append("<div class='empty-l'>")
              .append("critical".equals(view) ? "No critical errors found" : "No logs yet")
              .append("</div>")
              .append("<div class='empty-h'>")
              .append("critical".equals(view) ? "Add a CRITICAL: ... log to see it here" : "Add a log entry using the form")
              .append("</div></div>");
        } else {
            for (ServerError error : list) {
                String typeClass = "t-" + error.getErrorType();
                sb.append("<div class='log-entry'>")
                  .append("<span class='log-id'>#").append(error.getErrorId()).append("</span>")
                  .append("<span class='log-type ").append(typeClass).append("'>")
                  .append(escapeHtml(error.getErrorType())).append("</span>")
                  .append("<span class='log-msg'>").append(escapeHtml(error.getMessage())).append("</span>")
                  .append("</div>");
            }
        }
        sb.append("</div></div>");
        return sb.toString();
    }

    // Prevent XSS — always escape user input before putting in HTML
    private String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private ErrorLogManager getOrCreateManager(HttpSession session) {
        ErrorLogManager manager = (ErrorLogManager) session.getAttribute("manager");
        if (manager == null) {
            manager = new ErrorLogManager();
            session.setAttribute("manager", manager);
        }
        return manager;
    }
}
