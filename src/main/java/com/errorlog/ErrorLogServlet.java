package com.errorlog;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import jakarta.servlet.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.ArrayList;

@WebServlet("/logs")
public class ErrorLogServlet extends HttpServlet {

    // One manager for the whole app — reads/writes DB directly
    // No session needed — DB persists data for everyone
    private final ErrorLogManager manager = new ErrorLogManager();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        showPage(request, response, "all", null, null);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");
        String action   = request.getParameter("action");
        String logEntry = request.getParameter("logEntry");
        String sortBy   = request.getParameter("sortBy");

        String view       = "all";
        String successMsg = null;
        String errorMsg   = null;

        try {
            if ("add".equals(action)) {
                manager.addLog(logEntry);
                successMsg = "Log added: " + logEntry;

            } else if ("showAll".equals(action)) {
                view = "all";

            } else if ("showCritical".equals(action)) {
                view = "critical";

            } else if ("sort".equals(action)) {
                view = "sorted:" + (sortBy != null ? sortBy : "typeAsc");

            } else if ("clear".equals(action)) {
                manager.clearAll();
                successMsg = "All logs cleared from database.";
            }

        } catch (IllegalArgumentException ex) {
            errorMsg = ex.getMessage();
        } catch (SQLException ex) {
            errorMsg = "Database error: " + ex.getMessage();
        }

        showPage(request, response, view, successMsg, errorMsg);
    }

    private void showPage(HttpServletRequest request, HttpServletResponse response,
                          String view, String successMsg, String errorMsg)
            throws IOException {

        ArrayList<ServerError> displayList = new ArrayList<>();
        String dbError = null;

        try {
            if (view.startsWith("sorted:")) {
                String sortBy = view.substring(7);
                Comparator<ServerError> comp;
                if ("typeDesc".equals(sortBy))     comp = ErrorComparators.byErrorTypeDescending();
                else if ("id".equals(sortBy))       comp = ErrorComparators.byErrorId();
                else                                comp = ErrorComparators.byErrorType();
                displayList = manager.getSortedErrors(comp);
                view = "sorted";
            } else if ("critical".equals(view)) {
                displayList = manager.getCriticalErrors();
            } else {
                displayList = manager.getAllErrors();
                view = "all";
            }
        } catch (SQLException ex) {
            dbError = "Could not load logs: " + ex.getMessage();
        }

        // Get stats
        int total = 0, critical = 0, error = 0, warning = 0;
        try {
            total    = manager.countAll();
            critical = manager.countByType("CRITICAL");
            error    = manager.countByType("ERROR");
            warning  = manager.countByType("WARNING");
        } catch (SQLException ex) {
            // stats just show 0 if DB is unavailable
        }

        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();
        out.println(buildHtml(displayList, view, successMsg,
                              dbError != null ? dbError : errorMsg,
                              total, critical, error, warning));
    }

    // ── HTML builder ──────────────────────────────────
    private String buildHtml(ArrayList<ServerError> list, String view,
                              String successMsg, String errorMsg,
                              int total, int critical, int error, int warning) {
        StringBuffer sb = new StringBuffer();
        sb.append("<!DOCTYPE html><html lang='en'><head>")
          .append("<meta charset='UTF-8'>")
          .append("<meta name='viewport' content='width=device-width,initial-scale=1'>")
          .append("<title>Server Error Log Manager</title>")
          .append("<link href='https://fonts.googleapis.com/css2?family=JetBrains+Mono:wght@400;500&family=Syne:wght@400;500;600&display=swap' rel='stylesheet'>")
          .append(buildCss())
          .append("</head><body>")
          .append(buildHeader())
          .append("<div class='layout'>")
          .append(buildSidebar(total, critical, error, warning))
          .append(buildContent(list, view, successMsg, errorMsg))
          .append("</div></body></html>");
        return sb.toString();
    }

    private String buildCss() {
        return "<style>" +
            "*{box-sizing:border-box;margin:0;padding:0}" +
            ":root{--bg:#0f1117;--bg2:#161820;--bg3:#1e2030;--border:#2e3250;--border2:#3d4270;" +
            "--accent:#6c72f8;--accent2:#4ade80;--accent3:#f87171;--accent4:#fbbf24;" +
            "--text:#e2e4f0;--text2:#9198b8;--text3:#5a6080;" +
            "--font:'Syne',sans-serif;--mono:'JetBrains Mono',monospace}" +
            "body{background:var(--bg);color:var(--text);font-family:var(--font);height:100vh;overflow:hidden}" +
            ".header{background:var(--bg2);border-bottom:1px solid var(--border);padding:13px 24px;display:flex;align-items:center;gap:12px}" +
            ".hicon{width:32px;height:32px;background:var(--accent);border-radius:8px;display:flex;align-items:center;justify-content:center}" +
            ".htitle{font-size:15px;font-weight:600}" +
            ".hsub{font-size:11px;color:var(--text2);margin-left:auto;font-family:var(--mono);display:flex;align-items:center;gap:6px}" +
            ".dot{width:7px;height:7px;background:var(--accent2);border-radius:50%;display:inline-block}" +
            ".layout{display:grid;grid-template-columns:290px 1fr;height:calc(100vh - 56px)}" +
            ".sidebar{background:var(--bg2);border-right:1px solid var(--border);overflow-y:auto}" +
            ".ssec{padding:14px 16px;border-bottom:1px solid var(--border)}" +
            ".slabel{font-size:10px;font-weight:600;color:var(--text3);letter-spacing:.12em;text-transform:uppercase;margin-bottom:10px}" +
            "input,select{width:100%;background:var(--bg3);border:1px solid var(--border);color:var(--text);font-family:var(--mono);font-size:12px;padding:9px 12px;border-radius:8px;outline:none;margin-bottom:8px;transition:border .2s}" +
            "input:focus,select:focus{border-color:var(--accent)}" +
            "input::placeholder{color:var(--text3)}" +
            ".btn{width:100%;border:none;cursor:pointer;font-family:var(--font);font-weight:500;font-size:12px;padding:9px;border-radius:8px;display:flex;align-items:center;justify-content:center;gap:6px;margin-bottom:6px;transition:all .15s}" +
            ".bp{background:var(--accent);color:#fff}.bp:hover{background:#7c82f9}" +
            ".bs{background:rgba(74,222,128,.12);color:var(--accent2);border:1px solid rgba(74,222,128,.2)}.bs:hover{background:rgba(74,222,128,.22)}" +
            ".bd{background:rgba(248,113,113,.1);color:var(--accent3);border:1px solid rgba(248,113,113,.2)}.bd:hover{background:rgba(248,113,113,.2)}" +
            ".bw{background:rgba(251,191,36,.1);color:var(--accent4);border:1px solid rgba(251,191,36,.2)}.bw:hover{background:rgba(251,191,36,.2)}" +
            ".bg{background:var(--bg3);color:var(--text2);border:1px solid var(--border)}.bg:hover{border-color:var(--border2)}" +
            ".g2{display:grid;grid-template-columns:1fr 1fr;gap:6px;margin-bottom:6px}" +
            ".g2 .btn{margin:0}" +
            ".stats{display:grid;grid-template-columns:1fr 1fr;gap:8px}" +
            ".stat{background:var(--bg3);border:1px solid var(--border);border-radius:8px;padding:10px 12px}" +
            ".stn{font-size:22px;font-weight:600;font-family:var(--mono)}" +
            ".stl{font-size:10px;color:var(--text2);text-transform:uppercase;letter-spacing:.08em;margin-top:2px}" +
            ".ct .stn{color:var(--accent)}.cc .stn{color:var(--accent3)}.ce .stn{color:#c084fc}.cw .stn{color:var(--accent4)}" +
            ".content{display:flex;flex-direction:column;overflow:hidden}" +
            ".chead{padding:12px 20px;border-bottom:1px solid var(--border);background:var(--bg2);display:flex;align-items:center;gap:8px}" +
            ".badge{font-size:10px;font-family:var(--mono);padding:3px 10px;border-radius:20px;font-weight:500}" +
            ".ball{background:rgba(108,114,248,.12);color:var(--accent);border:1px solid rgba(108,114,248,.2)}" +
            ".bcrit{background:rgba(248,113,113,.1);color:var(--accent3)}" +
            ".bsort{background:rgba(251,191,36,.1);color:var(--accent4)}" +
            ".dbadge{font-size:10px;font-family:var(--mono);padding:2px 8px;border-radius:4px;background:rgba(74,222,128,.1);color:var(--accent2);border:1px solid rgba(74,222,128,.2);margin-left:auto}" +
            ".loglist{flex:1;overflow-y:auto;padding:14px 20px}" +
            ".entry{display:grid;grid-template-columns:36px 90px 1fr auto;gap:12px;align-items:center;padding:9px 14px;border-radius:9px;border:1px solid transparent;margin-bottom:4px;transition:all .15s}" +
            ".entry:hover{background:var(--bg3);border-color:var(--border)}" +
            ".eid{color:var(--text3);font-size:11px;font-family:var(--mono)}" +
            ".etype{display:inline-flex;padding:3px 9px;border-radius:5px;font-size:10px;font-weight:600;letter-spacing:.06em;font-family:var(--mono)}" +
            ".tCRITICAL{background:rgba(248,113,113,.12);color:#f87171;border:1px solid rgba(248,113,113,.25)}" +
            ".tERROR{background:rgba(192,132,252,.1);color:#c084fc;border:1px solid rgba(192,132,252,.2)}" +
            ".tWARNING{background:rgba(251,191,36,.1);color:#fbbf24;border:1px solid rgba(251,191,36,.2)}" +
            ".tINFO{background:rgba(108,114,248,.1);color:#818cf8;border:1px solid rgba(108,114,248,.2)}" +
            ".tUNKNOWN{background:rgba(90,96,128,.15);color:var(--text2);border:1px solid var(--border)}" +
            ".emsg{color:var(--text);font-size:12px;font-family:var(--mono);word-break:break-word}" +
            ".alert{padding:10px 16px;border-radius:8px;font-size:12px;margin:10px 20px 0}" +
            ".aok{background:rgba(74,222,128,.1);color:var(--accent2);border:1px solid rgba(74,222,128,.2)}" +
            ".aerr{background:rgba(248,113,113,.1);color:var(--accent3);border:1px solid rgba(248,113,113,.2)}" +
            ".empty{display:flex;flex-direction:column;align-items:center;justify-content:center;flex:1;gap:8px;color:var(--text3);padding:40px}" +
            "</style>";
    }

    private String buildHeader() {
        return "<div class='header'>" +
            "<div class='hicon'><svg width='18' height='18' viewBox='0 0 24 24' fill='none' stroke='white' stroke-width='2'>" +
            "<path d='M5 12h14M12 5l7 7-7 7'/></svg></div>" +
            "<span class='htitle'>Server Error Log Manager</span>" +
            "<div class='hsub'><span class='dot'></span>PostgreSQL · Render</div></div>";
    }

    private String buildSidebar(int total, int critical, int error, int warning) {
        StringBuffer sb = new StringBuffer();
        sb.append("<div class='sidebar'>")

          .append("<div class='ssec'><div class='slabel'>Add Log Entry</div>")
          .append("<form method='post' action='/logs'>")
          .append("<input type='text' name='logEntry' placeholder='CRITICAL: Disk full' autocomplete='off'/>")
          .append("<button class='btn bp' name='action' value='add'>+ Add Log</button>")
          .append("</form></div>")

          .append("<div class='ssec'><div class='slabel'>Actions</div>")
          .append("<div class='g2'>")
          .append("<form method='post' action='/logs'><button class='btn bs' name='action' value='showAll'>Show All</button></form>")
          .append("<form method='post' action='/logs'><button class='btn bd' name='action' value='showCritical'>Critical</button></form>")
          .append("</div>")
          .append("<form method='post' action='/logs'>")
          .append("<button class='btn bg' name='action' value='clear' onclick=\"return confirm('Clear ALL logs from database?')\">Clear All Logs</button>")
          .append("</form></div>")

          .append("<div class='ssec'><div class='slabel'>Sort</div>")
          .append("<form method='post' action='/logs'>")
          .append("<select name='sortBy'>")
          .append("<option value='typeAsc'>Error Type (A → Z)</option>")
          .append("<option value='typeDesc'>Error Type (Z → A)</option>")
          .append("<option value='id'>Error ID</option>")
          .append("</select>")
          .append("<button class='btn bw' name='action' value='sort'>Apply Sort</button>")
          .append("</form></div>")

          .append("<div class='ssec'><div class='slabel'>Statistics</div>")
          .append("<div class='stats'>")
          .append(stat(total,    "Total",    "ct"))
          .append(stat(critical, "Critical", "cc"))
          .append(stat(error,    "Error",    "ce"))
          .append(stat(warning,  "Warning",  "cw"))
          .append("</div></div></div>");

        return sb.toString();
    }

    private String stat(int n, String label, String cls) {
        return "<div class='stat " + cls + "'><div class='stn'>" + n +
               "</div><div class='stl'>" + label + "</div></div>";
    }

    private String buildContent(ArrayList<ServerError> list, String view,
                                 String successMsg, String errorMsg) {
        StringBuffer sb = new StringBuffer();

        String badgeCls  = "critical".equals(view) ? "bcrit" : "sorted".equals(view) ? "bsort" : "ball";
        String badgeText = "critical".equals(view) ? "critical only" : "sorted".equals(view) ? "sorted" : "all logs";

        sb.append("<div class='content'>")
          .append("<div class='chead'>")
          .append("<span style='font-size:13px;font-weight:500'>Log Output</span>")
          .append("<span class='badge ").append(badgeCls).append("'>").append(badgeText).append("</span>")
          .append("<span class='dbadge'>PostgreSQL</span>")
          .append("</div>");

        if (successMsg != null)
            sb.append("<div class='alert aok'>&#10003; ").append(escapeHtml(successMsg)).append("</div>");
        if (errorMsg != null)
            sb.append("<div class='alert aerr'>&#9888; ").append(escapeHtml(errorMsg)).append("</div>");

        sb.append("<div class='loglist'>");
        if (list.isEmpty()) {
            sb.append("<div class='empty'>")
              .append("<svg width='40' height='40' viewBox='0 0 24 24' fill='none' stroke='currentColor' stroke-width='1.5' opacity='.3'>")
              .append("<ellipse cx='12' cy='5' rx='9' ry='3'/><path d='M3 5v14c0 1.66 4.03 3 9 3s9-1.34 9-3V5'/><path d='M3 12c0 1.66 4.03 3 9 3s9-1.34 9-3'/>")
              .append("</svg>")
              .append("<div style='font-size:13px'>")
              .append("critical".equals(view) ? "No critical errors in database" : "No logs in database")
              .append("</div>")
              .append("<div style='font-size:11px;font-family:var(--mono)'>Add a log entry to get started</div>")
              .append("</div>");
        } else {
            for (ServerError e : list) {
                String tc = "t" + e.getErrorType().replaceAll("[^A-Z]", "");
                sb.append("<div class='entry'>")
                  .append("<span class='eid'>#").append(e.getErrorId()).append("</span>")
                  .append("<span class='etype ").append(tc).append("'>").append(escapeHtml(e.getErrorType())).append("</span>")
                  .append("<span class='emsg'>").append(escapeHtml(e.getMessage())).append("</span>")
                  .append("</div>");
            }
        }
        sb.append("</div></div>");
        return sb.toString();
    }

    private String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;");
    }
}