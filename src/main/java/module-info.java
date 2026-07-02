module de.spiritscorp.datasync {
	requires transitive jakarta.json;

	requires javafx.controls;
	requires transitive javafx.graphics;
	requires javafx.base;

	requires transitive org.kordamp.ikonli.core;
	requires transitive org.kordamp.ikonli.javafx;
	requires org.kordamp.ikonli.materialdesign2;

	requires transitive java.desktop;

	opens de.spiritscorp.datasync.gui to javafx.graphics;

}
