package dev.datadecay.ddwhitelist.update;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class UpdateChecker {

    private final JavaPlugin plugin;

    private final String versionUrl = "https://raw.githubusercontent.com/DataDecay/ddwhitelist/main/version.txt";
    private final String downloadUrl = "https://github.com/DataDecay/ddwhitelist/releases/latest";

    public UpdateChecker(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void checkForUpdate() {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            String latest = getLatestVersion();
            String current = plugin.getDescription().getVersion();

            if (latest != null && isUpdateAvailable(current, latest)) {
                plugin.getLogger().info("A new version of " + plugin.getName() + " is available: " + latest);
                plugin.getLogger().info("Download it here: " + downloadUrl);
            } else if (latest != null) {
                plugin.getLogger().info(plugin.getName() + " is up to date.");
            }
        });
    }

    private String getLatestVersion() {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(versionUrl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setRequestProperty("User-Agent", plugin.getName() + " UpdateChecker");

            if (conn.getResponseCode() != 200) {
                plugin.getLogger().warning("Failed to check for updates: HTTP " + conn.getResponseCode());
                return null;
            }

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8)
            )) {
                String line = reader.readLine();
                return line == null ? null : line.trim();
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to check for updates: " + e.getMessage());
            return null;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private boolean isUpdateAvailable(String current, String latest) {
        Version cur = Version.parse(current);
        Version lat = Version.parse(latest);
        return cur != null && lat != null && lat.compareTo(cur) > 0;
    }

    private static final class Version implements Comparable<Version> {
        final int major, minor, patch;
        final String flag;

        private Version(int major, int minor, int patch, String flag) {
            this.major = major;
            this.minor = minor;
            this.patch = patch;
            this.flag = (flag == null || flag.isBlank()) ? null : flag;
        }

        static Version parse(String s) {
            if (s == null) return null;
            s = s.trim();
            if (s.isEmpty()) return null;
            if (s.startsWith("v") || s.startsWith("V")) s = s.substring(1);

            String base = s;
            String flag = null;

            int dash = s.indexOf('-');
            if (dash >= 0) {
                base = s.substring(0, dash);
                flag = s.substring(dash + 1).trim();
                if (flag.isEmpty()) flag = null;
            }

            String[] parts = base.split("\\.");
            if (parts.length != 3) return null;

            try {
                int major = Integer.parseInt(parts[0]);
                int minor = Integer.parseInt(parts[1]);
                int patch = Integer.parseInt(parts[2]);
                return new Version(major, minor, patch, flag);
            } catch (NumberFormatException e) {
                return null;
            }
        }

        @Override
        public int compareTo(Version o) {
            if (o == null) return 1;

            if (major != o.major) return Integer.compare(major, o.major);
            if (minor != o.minor) return Integer.compare(minor, o.minor);
            if (patch != o.patch) return Integer.compare(patch, o.patch);

            boolean thisStable = (flag == null);
            boolean otherStable = (o.flag == null);
            if (thisStable && otherStable) return 0;
            if (thisStable) return 1;
            if (otherStable) return -1;

            int c = flag.compareToIgnoreCase(o.flag);
            if (c != 0) return c;
            return flag.compareTo(o.flag);
        }
    }
}
