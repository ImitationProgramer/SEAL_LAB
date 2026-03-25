package com.seal.seal_lab.core.enums;

public enum PubCategory {
    INT_JOURNAL("International Journal"),
    INT_CONFERENCE("International Conference"),
    DOM_JOURNAL("Domestic Journal"),
    DOM_CONFERENCE("Domestic Conference"),
    INT_PATENT("International Patent Registration"),
    DOM_PATENT("Domestic Patent Registration");

    private final String displayName;
    PubCategory(String displayName) { this.displayName = displayName; }
    public String getDisplayName() { return displayName; }
}
