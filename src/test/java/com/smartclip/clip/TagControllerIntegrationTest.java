package com.smartclip.clip;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartclip.clip.entity.ClipItem;
import com.smartclip.clip.service.ClipItemService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@AutoConfigureMockMvc
@SpringBootTest(properties = {
        "smartclip.data-dir=target/test-data",
        "smartclip.scheduler.tick-ms=60000",
        "spring.datasource.url=jdbc:sqlite:target/test-data/smartclip-test.db",
        "spring.task.scheduling.enabled=false"
})
/**
 * 标签控制器集成测试，验证标签接口的创建、绑定、过滤和删除行为。
 */
class TagControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ClipItemService clipItemService;

    @Test
    /**
     * 这个用例验证重复创建同名标签时会复用已有记录，而不是生成新的标签行。
     */
    void createsTagsIdempotently() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String tagName = "backend_" + suffix;

        long firstId = createTag(tagName);
        long secondId = createTag(tagName.toUpperCase());

        assertThat(secondId).isEqualTo(firstId);
    }

    @Test
    /**
     * 这个用例验证标签替换接口会更新剪贴板记录的标签集合，并支持随后按标签过滤查询。
     */
    void replacesClipTagsAndFiltersClipsByTag() throws Exception {
        String marker = UUID.randomUUID().toString();
        String tagName = "manual_" + marker.substring(0, 8);
        String chineseTag = "\u540e\u7aef";
        ClipItem item = clipItemService.captureText("manual tag clip " + marker).orElseThrow();

        mockMvc.perform(put("/api/clips/{id}/tags", item.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"names\":[\"" + tagName + "\",\"\\u540e\\u7aef\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(2));

        mockMvc.perform(get("/api/clips/{id}/tags", item.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.name=='" + tagName + "')]").exists())
                .andExpect(jsonPath("$.data[?(@.name=='" + chineseTag + "')]").exists());

        mockMvc.perform(get("/api/clips")
                        .param("keyword", marker)
                        .param("tag", tagName)
                        .param("view", "history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[0].id").value(item.getId()));
    }

    @Test
    /**
     * 这个用例验证删除标签后会同步清理标签关系，避免记录仍然挂着已不存在的标签。
     */
    void deletesTagAndItsRelations() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String tagName = "delete_" + suffix;
        ClipItem item = clipItemService.captureText("delete tag clip " + suffix).orElseThrow();
        long tagId = createTag(tagName);

        mockMvc.perform(put("/api/clips/{id}/tags", item.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"names\":[\"" + tagName + "\"]}"))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/tags/{id}", tagId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/api/clips/{id}/tags", item.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    /**
     * 这个辅助方法通过控制器创建标签，并返回响应中的标签主键供后续测试复用。
     */
    private long createTag(String tagName) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + tagName + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").exists())
                .andReturn();
        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        return root.path("data").path("id").asLong();
    }
}
