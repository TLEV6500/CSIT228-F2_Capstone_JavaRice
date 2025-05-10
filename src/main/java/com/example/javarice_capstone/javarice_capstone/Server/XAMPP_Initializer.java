package com.example.javarice_capstone.javarice_capstone.Server;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

public class XAMPP_Initializer {

    private static final String XAMPP_URL = "https://sourceforge.net/projects/xampp/files/XAMPP%20Windows/8.2.12/xampp-windows-x64-8.2.12-0-VS16-installer.exe/download";
    private static final String INSTALLER_PATH = "installers/xampp-installer-8.2.12.exe";
    private static final String XAMPP_DEFAULT_PATH = "C:\\xampp\\xampp-control.exe";
    private static final String APACHE_BIN = "C:\\xampp\\apache\\bin\\httpd.exe";
    private static final String MYSQL_BIN = "C:\\xampp\\mysql\\bin\\mysqld.exe";

    public static void start(){
        install();
        startXAMPP();
        startServices();
        addShutdownHook();
    }

    public static void install() {
        try {
            // Check if XAMPP is already installed
            File xampp = new File(XAMPP_DEFAULT_PATH);
            File xamppInstaller = new File(INSTALLER_PATH);

            if (xampp.exists()) {
                System.out.println("✅ XAMPP is already installed at: " + xampp.getAbsolutePath());
                return;
            } else if(xamppInstaller.exists()) {
                System.out.println("❌ XAMPP not found. ✅ XAMPP installer exists. Proceeding to install.");
            } else {
                System.out.println("❌ XAMPP not found. ❌ XAMPP installer doesn't exists. Proceeding to download.");
                downloadXAMPP();
            }

            // Run installer with admin rights and wait
            String command = "powershell -Command \"Start-Process '" + xamppInstaller.getAbsolutePath().replace("\\", "\\\\") +
                    "' -Verb runAs -Wait\"";

            ProcessBuilder pb = new ProcessBuilder("cmd", "/c", command);
            pb.inheritIO();
            Process process = pb.start();
            process.waitFor();

            System.out.println("✅ XAMPP installer finished.");

            waitForAndKillXamppControlPanel();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
    public static void downloadXAMPP() {
        try {
            System.out.println("🔽 Downloading XAMPP from: " + XAMPP_URL);
            URL url = new URL(XAMPP_URL);
            URLConnection connection = url.openConnection();
            int fileSize = connection.getContentLength();

            File saveFile = new File(INSTALLER_PATH);
            saveFile.getParentFile().mkdirs();

            try (InputStream inputStream = connection.getInputStream();
                 FileOutputStream outputStream = new FileOutputStream(saveFile)) {

                byte[] buffer = new byte[4096];
                int bytesRead;
                long totalBytesRead = 0;
                int percentCompleted = 0;

                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                    totalBytesRead += bytesRead;

                    int newPercent = (int) (totalBytesRead * 100 / fileSize);
                    if (newPercent != percentCompleted) {
                        percentCompleted = newPercent;
                        System.out.print("\rProgress: " + percentCompleted + "%");
                    }
                }

                System.out.println("\n✅ Download complete: " + saveFile.getAbsolutePath());
            }

        } catch (IOException e) {
            System.err.println("❌ Download failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void startXAMPP() {
        try {
            installServices();
            // Start Apache
            new ProcessBuilder("cmd", "/c", "net start Apache2.4").start();
            // Start MySQL
            new ProcessBuilder("cmd", "/c", "net start MySQL").start();
            System.out.println("✅ Apache and MySQL services started silently.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void stopXAMPP() {
        try {
            System.out.println("🛑 Stopping Apache and MySQL services...");

            // Stop Apache
            ProcessBuilder apacheStop = new ProcessBuilder("cmd", "/c", "sc stop Apache2.4");
            apacheStop.inheritIO();
            apacheStop.start().waitFor();

            // Stop MySQL
            ProcessBuilder mysqlStop = new ProcessBuilder("cmd", "/c", "sc stop mysql");
            mysqlStop.inheritIO();
            mysqlStop.start().waitFor();
            System.out.println("✅ Apache and MySQL services stopped.");
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void installServices() {
        try {
            if (isServiceInstalled("Apache2.4") && isServiceInstalled("mysql")) {
                System.out.println("✅ Apache and MySQL services are already installed.");
                return;
            }

            // Install Apache as a service
            Process apacheService = new ProcessBuilder("cmd.exe", "/c", "\"" + APACHE_BIN + "\" -k install")
                    .inheritIO()
                    .start();
            apacheService.waitFor();

            // Install MySQL as a service
            Process mysqlService = new ProcessBuilder("cmd.exe", "/c", "\"" + MYSQL_BIN + "\" --install")
                    .inheritIO()
                    .start();
            mysqlService.waitFor();

            System.out.println("✅ Apache and MySQL services registered.");
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static boolean isServiceInstalled(String serviceName) {
        try {
            ProcessBuilder pb = new ProcessBuilder("cmd", "/c", "sc query " + serviceName);
            Process process = pb.start();

            try (InputStream is = process.getInputStream()) {
                String output = new String(is.readAllBytes());
                return !output.contains("FAILED 1060");
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }


    public static void startServices() {
        try {
            Process startApache = new ProcessBuilder("cmd.exe", "/c", "net start Apache2.4")
                    .inheritIO()
                    .start();
            startApache.waitFor();

            Process startMySQL = new ProcessBuilder("cmd.exe", "/c", "net start mysql")
                    .inheritIO()
                    .start();
            startMySQL.waitFor();

            System.out.println("✅ Apache and MySQL services started.");
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void waitForAndKillXamppControlPanel() {
        try {
            System.out.println("⏳ Waiting for XAMPP Control Panel to launch...");
            boolean found = false;
            for (int i = 0; i < 5; i++) {
                Process processList = new ProcessBuilder("cmd", "/c", "tasklist /FI \"IMAGENAME eq xampp-control.exe\"")
                        .redirectErrorStream(true)
                        .start();

                try (InputStream is = processList.getInputStream()) {
                    String output = new String(is.readAllBytes());
                    if (output.contains("xampp-control.exe")) {
                        found = true;
                        break;
                    }
                }

                Thread.sleep(1000); // Wait 1 second before checking again
            }

            if (found) {
                System.out.println("🛑 XAMPP Control Panel detected. Closing...");
                new ProcessBuilder("cmd", "/c", "taskkill /F /IM xampp-control.exe").start().waitFor();
                System.out.println("✅ XAMPP Control Panel closed.");
            } else {
                System.out.println("⚠️ XAMPP Control Panel was not detected within the time limit.");
            }

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    // Shutdown hook to stop XAMPP when the Java application exits
    public static void addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(XAMPP_Initializer::stopXAMPP));
    }
}
