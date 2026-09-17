module UI {
    // דורשים את הספריות של JavaFX
    requires javafx.controls;
    requires javafx.fxml;

    // דורשים את מודול המנוע שלנו כדי שנוכל להשתמש בו
    requires engine;

    // פותחים את החבילה שלנו (ui) כדי ש-JavaFX יוכל לטעון את ה-FXML
    opens ui to javafx.fxml;
    exports ui;
}