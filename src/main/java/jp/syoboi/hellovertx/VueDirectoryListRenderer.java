package jp.syoboi.hellovertx;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;

import java.io.*;
import java.util.List;

public class VueDirectoryListRenderer implements SimpleFileHandler.DirectoryListRenderer {

    JsonFactory jsonFactory = JsonFactory.builder().build();

    public VueDirectoryListRenderer() {
    }

    String getTemplate() {
        File f = new File("templates/directory_index.vue");

        try (InputStream fis = new FileInputStream(f)) {
            return new String(fis.readAllBytes());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "template load error";
    }

    @Override
    public String render(String path, List<FileEntry> files) {
        StringWriter sw = new StringWriter();
        try {
            JsonGenerator jg = jsonFactory.createGenerator(sw);
            jg.writeStartObject();
            jg.writeObjectField("title", path);
            jg.writeArrayFieldStart("files");
            for (FileEntry entry : files) {
                jg.writeStartObject();
                jg.writeObjectField("name", entry.getBaseName());
                jg.writeObjectField("isDirectory", entry.props.isDirectory());
                jg.writeObjectField("size", entry.props.size());
                jg.writeObjectField("lastModified", entry.props.lastModifiedTime());
                jg.writeEndObject();
            }
            jg.writeEndArray();
            jg.writeEndObject();
            jg.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return getTemplate().replace("<!-- insert -->",
                "<script>\n" +
                        "var data=" + sw.toString() + ";\n" +
                        "</script>\n");
    }
}
