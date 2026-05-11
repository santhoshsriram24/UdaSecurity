module catpoint.security {
    requires catpoint.image;
    requires java.base;
    requires java.desktop;
    requires java.logging;

    exports com.udacity.catpoint.application;
    exports com.udacity.catpoint.data;
    exports com.udacity.catpoint.service;

    opens com.udacity.catpoint.data to com.google.gson;
}
