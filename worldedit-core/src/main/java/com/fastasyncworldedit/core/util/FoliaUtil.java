package com.fastasyncworldedit.core.util;

public final class FoliaUtil {

    private static final boolean FOLIA_DETECTED = detectFolia();

    private FoliaUtil() {
    }

    public static boolean isFoliaServer() {
        return FOLIA_DETECTED;
    }

    private static boolean detectFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }

}
