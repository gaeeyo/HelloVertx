package jp.syoboi.hellovertx;

import io.vertx.core.file.FileProps;

import javax.annotation.Nonnull;
import java.io.File;

public class FileEntry {
    public final String    name;
    public final FileProps props;

    public FileEntry(@Nonnull String name, @Nonnull FileProps props) {
        this.name = name;
        this.props = props;
    }

    public String getBaseName() {
        return name.substring(name.lastIndexOf(File.separator) + 1);
    }
}
