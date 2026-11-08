package com.zengame.ai.news;

import com.zengame.ai.business.news.channel.ChannelPlugin;
import com.zengame.ai.business.news.channel.ContentSanitizer;
import com.zengame.ai.business.news.channel.ItemIngestService;
import com.zengame.ai.business.news.channel.bo.ChannelDescriptor;
import com.zengame.ai.business.news.channel.bo.FetchedItem;
import com.zengame.ai.business.news.channel.bo.Playback;
import com.zengame.ai.business.news.domain.enums.NewsContentType;
import com.zengame.ai.business.news.domain.enums.NewsPlaybackKind;
import com.zengame.ai.repository.ai.mapper.NewsItemLogMapper;
import com.zengame.ai.repository.ai.mapper.NewsItemMapper;
import com.zengame.ai.repository.ai.po.NewsFeedPO;
import com.zengame.ai.repository.ai.po.NewsItemPO;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ItemIngestServiceTest {

    @Test
    void savesSourceMediaAndSanitizesBody() {
        NewsItemMapper itemMapper = mock(NewsItemMapper.class);
        TransactionTemplate transactions = mock(TransactionTemplate.class);
        doAnswer(invocation -> {
            invocation.<Consumer<TransactionStatus>>getArgument(0).accept(mock(TransactionStatus.class));
            return null;
        }).when(transactions).executeWithoutResult(any());
        when(itemMapper.insert(any())).thenAnswer(invocation -> {
            invocation.<NewsItemPO>getArgument(0).setId(12L);
            return 1;
        });
        ChannelPlugin plugin = mock(ChannelPlugin.class);
        when(plugin.descriptor()).thenReturn(ChannelDescriptor.builder().embedDomains(List.of()).build());

        NewsFeedPO feed = new NewsFeedPO();
        feed.setId(1L);
        feed.setChannelCode("x");
        feed.setContentType(NewsContentType.VIDEO);
        FetchedItem item = FetchedItem.builder()
                .canonicalUrl("https://example.org/articles/12")
                .title("Sample")
                .publishTime(LocalDateTime.of(2025, 6, 1, 10, 0))
                .coverUrl("https://example.org/cover.jpg")
                .bodyHtml("<img src='https://example.org/image.jpg' onerror='alert(1)'><script>alert(1)</script>")
                .playback(Playback.builder().kind(NewsPlaybackKind.FILE).url("https://example.org/video.mp4").build())
                .stats(List.of())
                .build();

        ItemIngestService service = new ItemIngestService(
                new ContentSanitizer(), itemMapper, mock(NewsItemLogMapper.class), transactions);

        assertThat(service.ingest(plugin, feed, item)).contains(12L);
        ArgumentCaptor<NewsItemPO> saved = ArgumentCaptor.forClass(NewsItemPO.class);
        verify(itemMapper).insert(saved.capture());
        assertThat(saved.getValue().getCoverUrl()).isEqualTo("https://example.org/cover.jpg");
        assertThat(saved.getValue().getPlaybackJson()).contains("https://example.org/video.mp4");
        assertThat(saved.getValue().getBodyHtml()).contains("https://example.org/image.jpg")
                .doesNotContain("script", "onerror");
    }
}
