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

    opens com.empresa.rcp to javafx.fxml;

    exports com.empresa.rcp;

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
}
