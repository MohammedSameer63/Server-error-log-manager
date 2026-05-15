package com.errorlog;

import org.apache.catalina.Context;
import org.apache.catalina.startup.Tomcat;

import java.io.File;

public class Main {

    public static void main(String[] args) throws Exception {

        // Initialize DB table before starting server
        // Creates table if it doesn't exist — safe to run every startup
        System.out.println("Initializing database...");
        DBConnection.initializeTable();

        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "9090"));

        Tomcat tomcat = new Tomcat();
        tomcat.setPort(port);
        tomcat.getConnector();

        File webappDir = new File(System.getProperty("java.io.tmpdir"), "errorlog-webapp");
        webappDir.mkdirs();

        Context ctx = tomcat.addWebapp("", webappDir.getAbsolutePath());

        Tomcat.addServlet(ctx, "ErrorLogServlet", new ErrorLogServlet());
        ctx.addServletMappingDecoded("/logs", "ErrorLogServlet");
        ctx.addServletMappingDecoded("/",     "ErrorLogServlet");

        tomcat.start();
        System.out.println("========================================");
        System.out.println("  Server Error Log Manager started!");
        System.out.println("  Port: " + port);
        System.out.println("  DB:   PostgreSQL (Render)");
        System.out.println("========================================");
        tomcat.getServer().await();
    }
}