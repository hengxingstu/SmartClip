package com.smartclip.clip;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import java.util.UUID;

import com.smartclip.clip.dto.ClipItemDetailResponse;
import com.smartclip.clip.dto.ClipItemListResponse;
import com.smartclip.clip.dto.ClipSearchRequest;
import com.smartclip.clip.dto.TagResponse;
import com.smartclip.clip.entity.ClipItem;
import com.smartclip.clip.enums.ClipListView;
import com.smartclip.clip.enums.ClipType;
import com.smartclip.clip.service.ClipItemService;
import com.smartclip.clip.service.TagService;
import com.smartclip.common.api.PageResponse;
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
 * 剪贴板内容服务集成测试，覆盖 SQLite 持久化、去重、视图筛选和标签联动链路。
 */
class ClipItemServiceIntegrationTest {

    @Autowired
    private ClipItemService clipItemService;

    @Autowired
    private TagService tagService;

    @Test
    /**
     * 这个用例验证相同文本只会生成一条主记录，并通过重复采集累计 copyCount 和事件信息。
     */
    void capturesAndDeduplicatesText() {
        String marker = UUID.randomUUID().toString();
        String content = "https://example.com/" + marker;

        Optional<ClipItem> first = clipItemService.captureText(content);
        Optional<ClipItem> second = clipItemService.captureText(content);

        assertThat(first).isPresent();
        assertThat(second).isPresent();
        assertThat(second.get().getId()).isEqualTo(first.get().getId());
        assertThat(second.get().getCopyCount()).isEqualTo(2);
        assertThat(second.get().getType()).isEqualTo(ClipType.URL);

        ClipItemDetailResponse detail = clipItemService.getDetail(first.get().getId());
        assertThat(detail.getContent()).isEqualTo(content);
        assertThat(detail.getFirstCopiedAt()).isNotNull();
        assertThat(detail.getLastCopiedAt()).isNotNull();
        assertThat(detail.getSubType()).isEqualTo("GENERAL");
        assertThat(detail.getTags()).extracting(TagResponse::getName).contains("url");

        ClipSearchRequest request = new ClipSearchRequest();
        request.setKeyword(marker);
        PageResponse<?> page = clipItemService.search(request);
        assertThat(page.getTotal()).isGreaterThanOrEqualTo(1);
    }

    @Test
    /**
     * 这个用例验证收藏视图只返回已收藏的记录，并且取消收藏后会立即从该视图消失。
     */
    void filtersFavoritesView() {
        String marker = UUID.randomUUID().toString();
        ClipItem favorite = clipItemService.captureText("favorite clip " + marker).orElseThrow();
        ClipItem regular = clipItemService.captureText("regular clip " + marker).orElseThrow();

        clipItemService.setFavorite(favorite.getId(), true);

        ClipSearchRequest request = new ClipSearchRequest();
        request.setKeyword(marker);
        request.setView(ClipListView.FAVORITES);

        PageResponse<ClipItemListResponse> page = clipItemService.search(request);
        assertThat(page.getItems()).hasSize(1);
        assertThat(page.getItems().get(0).getId()).isEqualTo(favorite.getId());
        assertThat(page.getItems().get(0).getIsFavorite()).isTrue();

        clipItemService.setFavorite(favorite.getId(), false);

        PageResponse<ClipItemListResponse> afterUnfavorite = clipItemService.search(request);
        assertThat(afterUnfavorite.getItems()).isEmpty();
        assertThat(regular.getId()).isNotEqualTo(favorite.getId());
    }

    @Test
    /**
     * 这个用例验证被忽略的内容不会出现在历史视图中，但仍能在 ignored 视图中找回并恢复。
     */
    void hidesIgnoredItemsUntilTheyAreRestored() {
        String marker = UUID.randomUUID().toString();
        ClipItem item = clipItemService.captureText("ignored clip " + marker).orElseThrow();

        clipItemService.softDelete(item.getId());

        ClipSearchRequest historyRequest = new ClipSearchRequest();
        historyRequest.setKeyword(marker);
        historyRequest.setView(ClipListView.HISTORY);

        PageResponse<ClipItemListResponse> historyPage = clipItemService.search(historyRequest);
        assertThat(historyPage.getItems()).isEmpty();

        ClipSearchRequest ignoredRequest = new ClipSearchRequest();
        ignoredRequest.setKeyword(marker);
        ignoredRequest.setView(ClipListView.IGNORED);

        PageResponse<ClipItemListResponse> ignoredPage = clipItemService.search(ignoredRequest);
        assertThat(ignoredPage.getItems()).hasSize(1);
        assertThat(ignoredPage.getItems().get(0).getId()).isEqualTo(item.getId());
        assertThat(ignoredPage.getItems().get(0).getIsIgnored()).isTrue();

        clipItemService.restore(item.getId());

        PageResponse<ClipItemListResponse> restoredHistory = clipItemService.search(historyRequest);
        assertThat(restoredHistory.getItems()).hasSize(1);
        assertThat(restoredHistory.getItems().get(0).getId()).isEqualTo(item.getId());
    }

    @Test
    /**
     * 这个用例验证高频视图会优先按复制次数排序，并在次数相同时再比较最后复制时间。
     */
    void sortsFrequentViewByCopyCountThenLastCopiedAt() {
        String marker = UUID.randomUUID().toString();

        ClipItem top = clipItemService.captureText("top clip " + marker).orElseThrow();
        clipItemService.captureText("top clip " + marker);
        clipItemService.captureText("top clip " + marker);

        ClipItem next = clipItemService.captureText("next clip " + marker).orElseThrow();
        clipItemService.captureText("next clip " + marker);

        clipItemService.captureText("other clip " + marker);

        ClipSearchRequest request = new ClipSearchRequest();
        request.setKeyword(marker);
        request.setView(ClipListView.FREQUENT);

        PageResponse<ClipItemListResponse> page = clipItemService.search(request);
        assertThat(page.getItems()).hasSizeGreaterThanOrEqualTo(3);
        assertThat(page.getItems().get(0).getId()).isEqualTo(top.getId());
        assertThat(page.getItems().get(0).getCopyCount()).isEqualTo(3);
        assertThat(page.getItems().get(1).getId()).isEqualTo(next.getId());
        assertThat(page.getItems().get(1).getCopyCount()).isEqualTo(2);
    }

    @Test
    /**
     * 这个用例验证新采集的 GitHub URL 会自动带上分类标签，并支持按标签过滤搜索结果。
     */
    void autoTagsNewClipsAndFiltersByTag() {
        String marker = UUID.randomUUID().toString();
        ClipItem item = clipItemService.captureText("https://github.com/HengxingStu/SmartClip?marker=" + marker)
                .orElseThrow();

        ClipItemDetailResponse detail = clipItemService.getDetail(item.getId());
        assertThat(detail.getSubType()).isEqualTo("GITHUB");
        assertThat(detail.getTags()).extracting(TagResponse::getName).contains("url", "github");

        ClipSearchRequest request = new ClipSearchRequest();
        request.setKeyword(marker);
        request.setTag("github");

        PageResponse<ClipItemListResponse> page = clipItemService.search(request);
        assertThat(page.getItems()).hasSize(1);
        assertThat(page.getItems().get(0).getId()).isEqualTo(item.getId());
        assertThat(page.getItems().get(0).getTags()).extracting(TagResponse::getName).contains("github");
    }

    @Test
    /**
     * 这个用例验证重复采集已存在内容时不会重置人工维护过的标签集合。
     */
    void repeatedCaptureDoesNotResetManualTags() {
        String marker = UUID.randomUUID().toString();
        String content = "select * from clip_item where title = '" + marker + "'";
        ClipItem item = clipItemService.captureText(content).orElseThrow();

        tagService.replaceClipTags(item.getId(), java.util.List.of("manual_" + marker.substring(0, 8)));
        clipItemService.captureText(content);

        ClipItemDetailResponse detail = clipItemService.getDetail(item.getId());
        assertThat(detail.getCopyCount()).isEqualTo(2);
        assertThat(detail.getTags()).extracting(TagResponse::getName)
                .containsExactly("manual_" + marker.substring(0, 8));
        assertThat(detail.getSubType()).isEqualTo("SELECT");
    }
}
