package com.cleanabap.plugin.integration;

import com.cleanabap.core.profiles.CleanupProfile;
import com.cleanabap.plugin.Activator;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages cleanup profiles — loading, saving, and switching the active profile.
 *
 * <p>Profiles are stored in the Eclipse workspace metadata directory
 * under {@code .metadata/.plugins/com.cleanabap.plugin/profiles/}</p>
 */
public class ProfileManager {

    private static ProfileManager instance;

    private CleanupProfile activeProfile;
    private final List<CleanupProfile> profiles = new ArrayList<>();

    private ProfileManager() {
        initializeDefaults();
    }

    public static synchronized ProfileManager getInstance() {
        if (instance == null) {
            instance = new ProfileManager();
        }
        return instance;
    }

    // ─── Initialization ──────────────────────────────────────────

    private void initializeDefaults() {
        profiles.add(CleanupProfile.createEssential());
        profiles.add(CleanupProfile.createDefault());
        profiles.add(CleanupProfile.createFull());
        activeProfile = profiles.get(1); // Default

        // Try to load saved profiles
        loadSavedProfiles();
    }

    private void loadSavedProfiles() {
        File profileDir = getProfileDirectory();
        if (profileDir == null || !profileDir.exists()) return;

        File[] files = profileDir.listFiles((dir, name) -> name.endsWith(".profile"));
        if (files == null) return;

        for (File file : files) {
            try {
                CleanupProfile profile = CleanupProfile.loadFromFile(file);
                profiles.add(profile);
            } catch (Exception e) {
                System.err.println("Failed to load profile: " + file.getName());
            }
        }

        // Load active profile preference
        try {
            String activeName = Activator.getDefault().getPreferenceStore()
                .getString("activeProfile");
            if (activeName != null && !activeName.isEmpty()) {
                profiles.stream()
                    .filter(p -> p.getName().equals(activeName))
                    .findFirst()
                    .ifPresent(p -> activeProfile = p);
            }
        } catch (Exception e) {
            // Use default
        }
    }

    // ─── Profile Access ──────────────────────────────────────────

    public CleanupProfile getActiveProfile() {
        return activeProfile;
    }

    public void setActiveProfile(CleanupProfile profile) {
        this.activeProfile = profile;
        // Save preference
        try {
            Activator.getDefault().getPreferenceStore()
                .setValue("activeProfile", profile.getName());
        } catch (Exception e) {
            // Ignore
        }
    }

    public List<CleanupProfile> getAllProfiles() {
        return new ArrayList<>(profiles);
    }

    // ─── Profile Management ──────────────────────────────────────

    public CleanupProfile createCustomProfile(String name) {
        CleanupProfile custom = CleanupProfile.createCustom(name, activeProfile);
        profiles.add(custom);
        saveProfile(custom);
        return custom;
    }

    public void deleteProfile(CleanupProfile profile) {
        if (profile.getType() != CleanupProfile.ProfileType.CUSTOM) return;
        profiles.remove(profile);
        if (activeProfile == profile) {
            activeProfile = profiles.get(1); // Fall back to Default
        }
        deleteProfileFile(profile);
    }

    public void saveProfile(CleanupProfile profile) {
        File dir = getProfileDirectory();
        if (dir == null) return;
        dir.mkdirs();

        File file = new File(dir, sanitizeFilename(profile.getName()) + ".profile");
        try {
            profile.saveToFile(file);
        } catch (Exception e) {
            System.err.println("Failed to save profile: " + e.getMessage());
        }
    }

    public void importProfile(File file) throws Exception {
        CleanupProfile imported = CleanupProfile.loadFromFile(file);
        profiles.add(imported);
        saveProfile(imported);
    }

    public void exportProfile(CleanupProfile profile, File file) throws Exception {
        profile.saveToFile(file);
    }

    // ─── Helpers ─────────────────────────────────────────────────

    private File getProfileDirectory() {
        try {
            return Activator.getDefault().getStateLocation()
                .append("profiles").toFile();
        } catch (Exception e) {
            return new File(System.getProperty("user.home"), ".cleanabap/profiles");
        }
    }

    private void deleteProfileFile(CleanupProfile profile) {
        File dir = getProfileDirectory();
        if (dir == null) return;
        File file = new File(dir, sanitizeFilename(profile.getName()) + ".profile");
        if (file.exists()) file.delete();
    }

    private String sanitizeFilename(String name) {
        return name.replaceAll("[^a-zA-Z0-9_-]", "_");
    }
}
