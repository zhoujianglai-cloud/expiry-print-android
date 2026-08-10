package com.fjxm.print;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class SeedDataTest {
    @Test
    public void seedContainsRecoveredMaterials() throws Exception {
        String json = new String(Files.readAllBytes(Paths.get("src/main/assets/materials.json")),
                StandardCharsets.UTF_8);
        assertEquals(161, occurrences(json, "\"product\":"));
        assertTrue(json.contains("茉莉茶叶"));
        assertTrue(json.contains("水吧"));
    }

    private static int occurrences(String text, String token) {
        int count = 0;
        int cursor = 0;
        while ((cursor = text.indexOf(token, cursor)) >= 0) {
            count++;
            cursor += token.length();
        }
        return count;
    }
}
