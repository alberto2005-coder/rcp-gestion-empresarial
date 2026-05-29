package com.empresa.rcp.module;

import javafx.scene.Parent;

public interface RCPModule {
    String getModuleName();
    String getIconCode(); // Ikonli icon identifier (e.g. "mdi2-account")
    Parent getView();
    void initializeModule();
}
