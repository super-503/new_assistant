package com.zengame.ai.apiclient.aibase;

import cn.hutool.core.collection.CollectionUtil;
import com.zengame.ai.infrastructure.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * @author jurzis.zhang
 * @version v1.0.0
 * @date 2025/6/20 10:39
 **/
@Slf4j
@Component
@RequiredArgsConstructor
public class AiBaseClient {

    private final RestTemplate restTemplate;

    private static final String INFORMATION_URL = "https://app.chinaz.com/djflkdsoisknfoklsyhownfrlewfknoiaewf/ai/GetAiInfoList.aspx";

    ParameterizedTypeReference<List<InformationVO>> INFORMATION_RESPONSE = new ParameterizedTypeReference<>() {};

    /**
     * 查询aibase的资讯信息
     * @param page
     * @return
     */
    @Retryable(value = {RestClientException.class, BusinessException.class}, maxAttempts = 5, backoff = @Backoff(delay = 1000))
    public List<InformationVO> queryInformationData(Integer page) {
        HttpHeaders httpHeaders = initializeHeaders();
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(INFORMATION_URL)
                .queryParam("flag", "zh")
                .queryParam("type", 1)
                .queryParam("page", page)
                .queryParam("pagesize", 100);
        String uriString = builder.toUriString();
        ResponseEntity<List<InformationVO>> result = restTemplate.exchange(new RequestEntity<>(null, httpHeaders, HttpMethod.GET, URI.create(uriString)), INFORMATION_RESPONSE);
        if (result.getStatusCode() != HttpStatus.OK) {
            log.error("查询资讯信息失败, 详细错误信息是：{}", result.getStatusCode());
            throw new BusinessException("查询资讯信息失败");
        }
        return result.getBody();
    }

    public HttpHeaders initializeHeaders() {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        return httpHeaders;
    }

    /**
     * 查询所有信息数据，自动分页直至没有更多数据
     * @return 所有资讯信息列表
     */
    public List<InformationVO> queryAllInformationData() {
        List<InformationVO> allInformation = new ArrayList<>();
        int currentPage = 1;
        
        while (true) {
            List<InformationVO> pageData = queryInformationData(currentPage);
            // 如果返回为空或者空列表，说明已经没有更多数据，退出循环
            if (CollectionUtil.isEmpty(pageData)) {
                break;
            }
            // 将当前页数据添加到总结果中
            allInformation.addAll(pageData);
            // 页码递增，准备获取下一页
            currentPage++;
            // 日志记录当前进度
            log.debug("已获取第 {} 页数据，当前总记录数: {}", currentPage - 1, allInformation.size());
        }
        
        return allInformation;
    }
}
