package com.cryptoinc.orderboard;

import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

public class IOUtil {
    public static String readString(Resource resource) throws IOException {
        try (Reader reader = new InputStreamReader(resource.getInputStream())) {
            StringBuilder builder = new StringBuilder();
            char[] buffer = new char[1024];
            int count;
            while ((count = reader.read(buffer)) > 0) {
                builder.append(buffer, 0, count);
            }
            return builder.toString();
        }
    }
}
