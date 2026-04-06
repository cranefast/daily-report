package com.report.dailyreport.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PromptTemplateService {

    private final ResourceLoader resourceLoader;

    public String load(String classpathLocation) {
        Resource resource = resourceLoader.getResource("classpath:" + classpathLocation);
        try {
            return resource.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("프롬프트 리소스를 읽을 수 없습니다: " + classpathLocation, exception);
        }
    }

    public String render(String classpathLocation, Map<String, String> values) {
        String rendered = load(classpathLocation);
        for (Map.Entry<String, String> entry : values.entrySet()) {
            rendered = rendered.replace("{{" + entry.getKey() + "}}", entry.getValue() == null ? "" : entry.getValue());
        }
        return rendered;
    }
}
