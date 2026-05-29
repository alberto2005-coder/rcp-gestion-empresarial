@SuppressWarnings("module")
module com.empresa.rcp {
    requires transitive javafx.controls;
    requires transitive javafx.fxml;
    requires transitive javafx.graphics;
    requires java.sql;
    requires atlantafx.base;
    requires com.zaxxer.hikari;
    requires jbcrypt;
    requires com.github.librepdf.openpdf;
    requires jakarta.mail;
    requires org.slf4j;
    requires com.sun.jna;
    requires com.sun.jna.platform;
    requires org.kordamp.ikonli.javafx;

    uses com.empresa.rcp.module.RCPModule;

    opens com.empresa.rcp to javafx.fxml;

    exports com.empresa.rcp;
    exports com.empresa.rcp.util;

    opens com.empresa.rcp.module.auth to javafx.fxml;
    exports com.empresa.rcp.module.auth;

    opens com.empresa.rcp.module.dashboard to javafx.fxml;

    exports com.empresa.rcp.module.dashboard;

    opens com.empresa.rcp.module.clientes to javafx.fxml;

    exports com.empresa.rcp.module.clientes;

    opens com.empresa.rcp.module.empleados to javafx.fxml;

    exports com.empresa.rcp.module.empleados;

    opens com.empresa.rcp.module.pedidos to javafx.fxml;

    exports com.empresa.rcp.module.pedidos;

    opens com.empresa.rcp.module.configuracion to javafx.fxml;

    exports com.empresa.rcp.module.configuracion;

    opens com.empresa.rcp.module.reportes to javafx.fxml;

    exports com.empresa.rcp.module.reportes;
    exports com.empresa.rcp.sync;
}
