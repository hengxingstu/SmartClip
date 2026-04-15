package com.smartclip.clip;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import java.util.UUID;

import com.smartclip.clip.dto.ClipItemDetailResponse;
import com.smartclip.clip.dto.ClipItemListResponse;
import com.smartclip.clip.dto.ClipSearchRequest;
import com.smartclip.clip.entity.ClipItem;
import com.smartclip.clip.enums.ClipListView;
import com.smartclip.clip.enums.ClipType;
import com.smartclip.clip.service.ClipItemService;
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
 * 剪贴板内容服务集成测试，覆盖 SQLite 持久化、去重和查询链路。
 */
class ClipItemServiceIntegrationTest {

    @Autowired
    private ClipItemService clipItemService;

    @Test
    /**
     * 验证相同文本只生成一个 ClipItem，并累加复制次数。
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

        ClipSearchRequest request = new ClipSearchRequest();
        request.setKeyword(marker);
        PageResponse<?> page = clipItemService.search(request);
        assertThat(page.getTotal()).isGreaterThanOrEqualTo(1);
    }

    @Test
    /**
     * 楠岃瘉鏀惰棌瑙嗗浘浠呰繑鍥炲凡鏀惰棌涓旀湭蹇界暐鐨勫唴瀹癸紝鍙栨秷鏀惰棌鍚庡簲浠庤鍥句腑娑堝け銆?
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
     * 楠岃瘉蹇界暐鍚庤褰曚粠鏅€氬巻鍙蹭腑闅愯棌锛屼粎鍦?ignored 瑙嗗浘鍙锛屾仮澶嶅悗鍙噸鏂板嚭鐜般€?
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
     * 楠岃瘉楂橀瑙嗗浘鎸?copyCount 鍜?lastCopiedAt 鍊掑簭鎺掑簭锛岄珮棰戝唴瀹逛紭鍏堝睍绀恒€?
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
}
