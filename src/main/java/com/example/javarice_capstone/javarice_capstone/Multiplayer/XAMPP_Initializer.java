package com.example.javarice_capstone.javarice_capstone.Multiplayer;

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

    public static void start() {
        stopXAMPP();
        install();
        startXAMPP();
    }

    public static void install() {
        try {
            File xampp = new File(XAMPP_DEFAULT_PATH);
            File xamppInstaller = new File(INSTALLER_PATH);

            if (xampp.exists()) {
                System.out.println("✅ XAMPP is already installed at: " + xampp.getAbsolutePath());
                return;
            } else if (!xamppInstaller.exists()) {
                System.out.println("❌ XAMPP not found. ❌ XAMPP installer doesn't exist. Proceeding to download.");
                downloadXAMPP();
            } else {
                System.out.println("❌ XAMPP not found. ✅ XAMPP installer exists. Proceeding to install.");
            }

            String command = "powershell -Command \"Start-Process '" + xamppInstaller.getAbsolutePath().replace("\\", "\\\\") + "' -Verb runAs -Wait\"";
            new ProcessBuilder("cmd", "/c", command).inheritIO().start().waitFor();
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
            System.out.println("🚀 Starting Apache and MySQL without installing services...");
            new ProcessBuilder("C:\\xampp\\apache\\bin\\httpd.exe").start();
            new ProcessBuilder("C:\\xampp\\mysql\\bin\\mysqld.exe").start();
            System.out.println("✅ Apache and MySQL started as standalone processes.");
        } catch (IOException e) {
            System.err.println("❌ Failed to start XAMPP components.");
            e.printStackTrace();
        }
    }


    public static void stopXAMPP() {
        try {
            System.out.println("🛑 Attempting to stop Apache and MySQL (non-service mode)...");
            new ProcessBuilder("cmd", "/c", "taskkill /F /IM httpd.exe").start().waitFor();
            new ProcessBuilder("cmd", "/c", "taskkill /F /IM mysqld.exe").start().waitFor();
            System.out.println("✅ Apache and MySQL stopped.");
        } catch (IOException | InterruptedException e) {
            System.err.println("❌ Failed to stop XAMPP components.");
            e.printStackTrace();
        }
    }

    public static boolean isServiceStarted(String serviceName) {
        try {
            Process process = new ProcessBuilder("cmd", "/c", "sc query " + serviceName).start();
            String output = new String(process.getInputStream().readAllBytes());
            return output.contains("STATE") && output.contains("RUNNING");
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static void waitForAndKillXamppControlPanel() {
        try {
            System.out.println("⏳ Waiting for XAMPP Control Panel to launch...");
            boolean found = false;
            for (int i = 0; i < 5; i++) {
                Process process = new ProcessBuilder("cmd", "/c", "tasklist /FI \"IMAGENAME eq xampp-control.exe\"").start();
                String output = new String(process.getInputStream().readAllBytes());
                if (output.contains("xampp-control.exe")) {
                    found = true;
                    break;
                }
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

    public static void addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            stopXAMPP();
            if (SessionState.LobbyCode != null) {
                System.out.println("🧹 Cleaning up hosted lobby: " + SessionState.LobbyCode);
                if (LobbyManager.deleteLobby(SessionState.LobbyCode)) {
                    System.out.println("✅ Lobby deleted from database.");
                } else {
                    System.out.println("⚠️ Failed to delete lobby.");
                }
            }
        }));
    }
}
