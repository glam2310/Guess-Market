module engine {
    // מייצאים את החבילות החוצה כדי שמודול ה-UI יוכל להשתמש בהן
    exports engine;
    exports models;
    exports exception;
    exports dto;

    // דורשים את הספרייה של JAXB כדי להמשיך לעבוד עם XML
    requires jakarta.xml.bind;

    // פותחים את החבילות של המודלים וה-JAXB כדי שהספרייה תוכל לקרוא ולכתוב אליהן בזמן ריצה
    opens models to jakarta.xml.bind;
    opens jaxb.generated to jakarta.xml.bind;
}