package com.smartclip.clip;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import com.smartclip.clip.dto.TagResponse;
import com.smartclip.clip.entity.ClipItem;
import com.smartclip.clip.service.ClipItemService;
import com.smartclip.clip.service.TagService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "smartclip.data-dir=target/test-data",
        "smartclip.scheduler.tick-ms=60000",
        "spring.datasource.url=jdbc:sqlite:target/test-data/smartclip-test.db",
        "spring.task.scheduling.enabled=false"
})
/**
 * 标签服务集成测试，聚焦标签规范化、关系替换和失败回滚这几个核心规则。
 */
class TagServiceIntegrationTest {

    @Autowired
    private TagService tagService;

    @Autowired
    private ClipItemService clipItemService;

    @Test
    /**
     * 这个用例验证标签替换时会裁剪空白、忽略大小写重复项，并允许中文标签正常写入和查询。
     */
    void replaceClipTagsNormalizesWhitespaceAndDeduplicatesNamesCaseInsensitively() {
        String marker = UUID.randomUUID().toString();
        ClipItem item = clipItemService.captureText("tag normalization " + marker).orElseThrow();
        String alpha = "Alpha" + marker.substring(0, 8);
        String beta = "Beta" + marker.substring(0, 8);
        String han = "\u540e\u7aef" + marker.substring(0, 4);

        List<TagResponse> replaced = tagService.replaceClipTags(item.getId(), List.of(
                "  " + alpha + "  ",
                alpha.toLowerCase(Locale.ROOT),
                beta,
                han,
                han
        ));

        assertThat(replaced).extracting(TagResponse::getName)
                .containsExactly(alpha, beta, han);
        assertThat(tagService.listClipTags(item.getId())).extracting(TagResponse::getName)
                .containsExactlyInAnyOrder(alpha, beta, han);
        assertThat(tagService.findClipIdsByTag(alpha.toLowerCase(Locale.ROOT)))
                .contains(item.getId());
    }

    @Test
    /**
     * 这个用例验证传入空列表替换标签时会清空当前记录已有的全部标签关系。
     */
    void replaceClipTagsWithEmptyListClearsExistingRelations() {
        String marker = UUID.randomUUID().toString();
        ClipItem item = clipItemService.captureText("tag clearing " + marker).orElseThrow();
        String tagName = "keep" + marker.substring(0, 8);

        tagService.replaceClipTags(item.getId(), List.of(tagName));
        List<TagResponse> cleared = tagService.replaceClipTags(item.getId(), List.of());

        assertThat(cleared).isEmpty();
        assertThat(tagService.listClipTags(item.getId())).isEmpty();
        assertThat(tagService.findClipIdsByTag(tagName)).doesNotContain(item.getId());
    }

    @Test
    /**
     * 这个用例验证当新标签列表中包含非法名称时，服务会抛错并保留替换前的标签状态。
     */
    void replaceClipTagsRejectsInvalidNamesWithoutRemovingExistingTags() {
        String marker = UUID.randomUUID().toString();
        ClipItem item = clipItemService.captureText("tag rollback " + marker).orElseThrow();
        String existingTag = "stable" + marker.substring(0, 8);

        tagService.replaceClipTags(item.getId(), List.of(existingTag));

        assertThatThrownBy(() -> tagService.replaceClipTags(item.getId(), List.of(existingTag, "bad tag!")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("may only contain");

        assertThat(tagService.listClipTags(item.getId())).extracting(TagResponse::getName)
                .containsExactly(existingTag);
    }
}
