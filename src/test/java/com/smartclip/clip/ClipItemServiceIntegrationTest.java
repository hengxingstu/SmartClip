package com.smartclip.clip;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import java.util.UUID;

import com.smartclip.clip.dto.ClipItemDetailResponse;
import com.smartclip.clip.dto.ClipSearchRequest;
import com.smartclip.clip.entity.ClipItem;
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
}
