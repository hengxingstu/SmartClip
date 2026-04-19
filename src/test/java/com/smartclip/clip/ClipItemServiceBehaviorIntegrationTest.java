package com.smartclip.clip;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.UUID;

import com.smartclip.clip.dto.ClipCopyResponse;
import com.smartclip.clip.dto.ClipItemDetailResponse;
import com.smartclip.clip.dto.ClipItemListResponse;
import com.smartclip.clip.dto.ClipSearchRequest;
import com.smartclip.clip.dto.TagResponse;
import com.smartclip.clip.entity.ClipItem;
import com.smartclip.clip.enums.ClipType;
import com.smartclip.clip.service.ClipItemService;
import com.smartclip.clip.service.TagService;
import com.smartclip.clipboard.ClipboardService;
import com.smartclip.common.api.PageResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest(properties = {
        "smartclip.data-dir=target/test-data",
        "smartclip.scheduler.tick-ms=60000",
        "spring.datasource.url=jdbc:sqlite:target/test-data/smartclip-test.db",
        "spring.task.scheduling.enabled=false"
})
/**
 * 行为级集成测试，补充验证剪贴板内容服务在状态切换和标签排序上的细节行为。
 */
class ClipItemServiceBehaviorIntegrationTest {

    @Autowired
    private ClipItemService clipItemService;

    @Autowired
    private TagService tagService;

    @MockBean
    private ClipboardService clipboardService;

    @Test
    /**
     * 这个用例验证默认搜索不会返回已忽略内容，只有显式开启 includeIgnored 时才会带出这些记录。
     */
    void defaultSearchIncludesIgnoredOnlyWhenRequested() {
        String marker = UUID.randomUUID().toString();
        ClipItem item = clipItemService.captureText("ignored default search " + marker).orElseThrow();
        clipItemService.softDelete(item.getId());

        ClipSearchRequest defaultRequest = new ClipSearchRequest();
        defaultRequest.setKeyword(marker);

        PageResponse<ClipItemListResponse> defaultPage = clipItemService.search(defaultRequest);
        assertThat(defaultPage.getItems()).isEmpty();

        ClipSearchRequest includeIgnoredRequest = new ClipSearchRequest();
        includeIgnoredRequest.setKeyword(marker);
        includeIgnoredRequest.setIncludeIgnored(true);

        PageResponse<ClipItemListResponse> includeIgnoredPage = clipItemService.search(includeIgnoredRequest);
        assertThat(includeIgnoredPage.getItems()).hasSize(1);
        assertThat(includeIgnoredPage.getItems().get(0).getId()).isEqualTo(item.getId());
        assertThat(includeIgnoredPage.getItems().get(0).getIsIgnored()).isTrue();
    }

    @Test
    /**
     * 这个用例验证已忽略内容被再次采集后会恢复显示状态，并且手工维护的标签不会被自动分类覆盖。
     */
    void repeatedCaptureRestoresIgnoredItemAndKeepsManualTags() {
        String marker = UUID.randomUUID().toString();
        String content = "git status " + marker;
        String manualTag = "manual" + marker.substring(0, 8);
        ClipItem item = clipItemService.captureText(content).orElseThrow();

        tagService.replaceClipTags(item.getId(), List.of(manualTag));
        clipItemService.softDelete(item.getId());

        ClipItem recaptured = clipItemService.captureText(content).orElseThrow();
        ClipItemDetailResponse detail = clipItemService.getDetail(item.getId());

        assertThat(recaptured.getId()).isEqualTo(item.getId());
        assertThat(recaptured.getCopyCount()).isEqualTo(2);
        assertThat(detail.getIsIgnored()).isFalse();
        assertThat(detail.getTags()).extracting(TagResponse::getName).containsExactly(manualTag);
        assertThat(detail.getSubType()).isEqualTo("GIT");
    }

    @Test
    /**
     * 这个用例验证复制到系统剪贴板时会调用写入接口，同时正确更新复制次数和最后复制时间。
     */
    void copyToClipboardWritesContentAndUpdatesCopyStats() {
        String marker = UUID.randomUUID().toString();
        String content = "clipboard copy " + marker;
        ClipItem item = clipItemService.captureText(content).orElseThrow();
        ClipItemDetailResponse beforeCopy = clipItemService.getDetail(item.getId());

        ClipCopyResponse response = clipItemService.copyToClipboard(item.getId());
        ClipItemDetailResponse afterCopy = clipItemService.getDetail(item.getId());

        verify(clipboardService).writeText(content);
        assertThat(response.isCopied()).isTrue();
        assertThat(response.getId()).isEqualTo(item.getId());
        assertThat(response.getCopyCount()).isEqualTo(beforeCopy.getCopyCount() + 1);
        assertThat(response.getCopiedAt()).isNotNull();
        assertThat(afterCopy.getCopyCount()).isEqualTo(beforeCopy.getCopyCount() + 1);
        assertThat(afterCopy.getLastCopiedAt()).isAfterOrEqualTo(beforeCopy.getLastCopiedAt());
    }

    @Test
    /**
     * 这个用例验证按类型筛选后的搜索结果仍会返回按不区分大小写排序好的标签列表。
     */
    void searchReturnsTagsSortedCaseInsensitivelyAndSupportsTypeFilter() {
        String marker = UUID.randomUUID().toString();
        ClipItem sqlItem = clipItemService.captureText("select * from clip_item where marker = '" + marker + "'")
                .orElseThrow();
        clipItemService.captureText("https://example.com/" + marker).orElseThrow();

        String zeta = "zeta" + marker.substring(0, 5);
        String alpha = "Alpha" + marker.substring(0, 5);
        String beta = "beta" + marker.substring(0, 5);
        tagService.replaceClipTags(sqlItem.getId(), List.of(zeta, alpha, beta));

        ClipSearchRequest request = new ClipSearchRequest();
        request.setKeyword(marker);
        request.setType(ClipType.SQL);

        PageResponse<ClipItemListResponse> page = clipItemService.search(request);

        assertThat(page.getItems()).hasSize(1);
        assertThat(page.getItems().get(0).getId()).isEqualTo(sqlItem.getId());
        assertThat(page.getItems().get(0).getType()).isEqualTo(ClipType.SQL);
        assertThat(page.getItems().get(0).getTags()).extracting(TagResponse::getName)
                .containsExactly(alpha, beta, zeta);
    }
}
