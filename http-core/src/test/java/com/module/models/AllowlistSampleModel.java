package com.module.models;

/**
 * Test fixture: a class living under the default-allowed prefix {@code com.module.models.}
 * so {@code DeserializeAllowlistTest} can exercise the allow path (W2-M2).
 */
public class AllowlistSampleModel {
    private String id;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
