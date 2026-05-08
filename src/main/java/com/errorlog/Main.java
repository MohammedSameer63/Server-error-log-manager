package com.errorlog;

import org.apache.catalina.Context;
import org.apache.catalina.startup.Tomcat;

import java.io.File;

public class Main {

    public static void main(String[] args) throws Exception {

        // Render assigns PORT via environment variable
        // Default is 9090 locally (8080 is reserved for Jenkins)
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "9090"));

        Tomcat tomcat = new Tomcat();
        tomcat.setPort(port);
        tomcat.getConnector();

        // Use a temp directory — works correctly after mvn package
        // The old "src/main/webapp" path breaks once you run the JAR
        // from a different directory
        File webappDir = new File(System.getProperty("java.io.tmpdir"), "errorlog-webapp");
        webappDir.mkdirs();

        Context ctx = tomcat.addWebapp("", webappDir.getAbsolutePath());

        // Register ErrorLogServlet at /logs
        Tomcat.addServlet(ctx, "ErrorLogServlet", new ErrorLogServlet());
        ctx.addServletMappingDecoded("/logs", "ErrorLogServlet");

        // Redirect root "/" → "/logs" so browser doesn't need /logs in URL
        Tomcat.addServlet(ctx, "RedirectServlet", new jakarta.servlet.http.HttpServlet() {
            @Override
            protected void doGet(jakarta.servlet.http.HttpServletRequest req,
                                 jakarta.servlet.http.HttpServletResponse res)
                    throws java.io.IOException {
                res.sendRedirect("/logs");
            }
            @Override
            protected void doPost(jakarta.servlet.http.HttpServletRequest req,
                                  jakarta.servlet.http.HttpServletResponse res)
                    throws java.io.IOException {
                res.sendRedirect("/logs");
            }
        });
        ctx.addServletMappingDecoded("/", "RedirectServlet");

        tomcat.start();
        System.out.println("========================================");
        System.out.println("  Server Error Log Manager started!");
        System.out.println("  URL: http://localhost:" + port);
        System.out.println("========================================");
        tomcat.getServer().await();
    }
}
