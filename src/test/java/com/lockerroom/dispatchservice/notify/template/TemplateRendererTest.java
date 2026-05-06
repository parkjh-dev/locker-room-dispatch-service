package com.lockerroom.dispatchservice.notify.template;

import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TemplateRendererTest {

    private final TemplateRenderer renderer = new TemplateRenderer();

    @Test
    void render_substitutesVariables() {
        String result = renderer.render("Hello {{name}}!", Map.of("name", "World"));
        assertThat(result).isEqualTo("Hello World!");
    }

    @Test
    void render_withMultipleVariables() {
        String result = renderer.render(
                "{{actor}}님이 {{post}}에 댓글을 남겼습니다.",
                Map.of("actor", "tester", "post", "글1"));
        assertThat(result).isEqualTo("tester님이 글1에 댓글을 남겼습니다.");
    }

    @Test
    void render_missingVariable_replacesWithEmpty() {
        String result = renderer.render("Hello {{name}}!", Map.of());
        assertThat(result).isEqualTo("Hello !");
    }

    @Test
    void render_nullVariablesMap_replacesAllWithEmpty() {
        String result = renderer.render("Hello {{name}}!", null);
        assertThat(result).isEqualTo("Hello !");
    }

    @Test
    void render_nullTemplate_returnsNull() {
        assertThat(renderer.render(null, Map.of())).isNull();
    }

    @Test
    void render_unrelatedBraces_leavesAsIs() {
        String result = renderer.render("price is {abc} won", Map.of());
        assertThat(result).isEqualTo("price is {abc} won");
    }

    @Test
    void render_dollarSignsInValue_areEscapedSafely() {
        String result = renderer.render("v={{x}}", Map.of("x", "$5 \\ literal"));
        assertThat(result).isEqualTo("v=$5 \\ literal");
    }
}
