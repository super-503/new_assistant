package com.zengame.ai.apiclient.paper;

import com.zengame.ai.business.paper.domain.enums.PaperSource;
import com.zengame.ai.infrastructure.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/** 论文目录请求的超时和错误处理。 */
@Slf4j
@Component
@RequiredArgsConstructor
class PaperHttpClient {

    private final OkHttpClient httpClient;

    String fetch(PaperSource source, Request request) {
        Call call = httpClient.newCall(request);
        call.timeout().timeout(15, TimeUnit.SECONDS);
        try (Response response = call.execute()) {
            if (response.code() == 429) {
                throw new BusinessException(source.getDisplayName() + "检索过于频繁，请稍后重试");
            }
            ResponseBody body = response.body();
            if (!response.isSuccessful() || body == null) {
                log.warn("论文来源响应失败, source={}, status={}", source, response.code());
                throw unavailable(source);
            }
            return body.string();
        } catch (IOException e) {
            log.warn("论文来源请求失败, source={}", source, e);
            throw unavailable(source);
        }
    }

    static BusinessException unavailable(PaperSource source) {
        return new BusinessException(source.getDisplayName() + "暂时无法检索，请稍后重试");
    }
}
