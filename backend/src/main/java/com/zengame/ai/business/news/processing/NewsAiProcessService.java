package com.zengame.ai.business.news.processing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import com.zengame.ai.business.news.NewsAiFieldCatalog;
import com.zengame.ai.business.news.domain.enums.NewsItemAction;
import com.zengame.ai.business.news.domain.enums.NewsItemStatus;
import com.zengame.ai.business.news.processing.domain.bo.NewsAiCall;
import com.zengame.ai.business.news.processing.domain.bo.NewsAiModelReply;
import com.zengame.ai.business.news.processing.domain.bo.NewsAiOutcome;
import com.zengame.ai.business.news.processing.domain.bo.NewsAiProcessLogDetail;
import com.zengame.ai.business.news.setting.NewsAiModelSettingService;
import com.zengame.ai.business.news.setting.domain.bo.NewsAiModelConfig;
import com.zengame.ai.infrastructure.exception.BusinessException;
import com.zengame.ai.infrastructure.util.JacksonUtil;
import com.zengame.ai.repository.ai.mapper.NewsAiFieldMapper;
import com.zengame.ai.repository.ai.mapper.NewsItemLogMapper;
import com.zengame.ai.repository.ai.mapper.NewsItemMapper;
import com.zengame.ai.repository.ai.po.NewsAiFieldPO;
import com.zengame.ai.repository.ai.po.NewsItemLogPO;
import com.zengame.ai.repository.ai.po.NewsItemPO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;

/**
 * 数据加工：按 AI 字段配置，对一条资讯调用一次模型，同时生成所有参与生成的字段，结果挂在资讯上。
 *
 * <p>模型调用都以计费用户的身份在 {@link NewsProcessExecutor} 里执行，调用方同步等待结果。
 * 合法的值写入 ai_values_json，人工修改过的字段不覆盖；未生成的字段记入 ai_failed_keys_json，已有的值保留。
 * 每次加工写一条 PROCESSED（本次字段全部生成）或 PROCESS_FAILED（有字段未生成）处理记录。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NewsAiProcessService {

    /**
     * 自动加工每批同时发起的调用数，与线程池大小一致
     */
    private static final int BATCH_SIZE = NewsProcessExecutor.POOL_SIZE;

    private final NewsItemMapper newsItemMapper;

    private final NewsItemLogMapper newsItemLogMapper;

    private final NewsAiFieldMapper newsAiFieldMapper;

    private final NewsAiFieldCatalog newsAiFieldCatalog;

    private final NewsAiModelSettingService newsAiModelSettingService;

    private final NewsAiPromptBuilder promptBuilder;

    private final NewsAiOutputValidator outputValidator;

    private final NewsAiModelInvoker modelInvoker;

    private final TransactionTemplate transactionTemplate;

    /**
     * 自动加工新入库的资讯。已配置模型时逐条加工后转入待审，未配置模型时直接转入待审。
     *
     * @param itemIds 加工中的资讯
     * @throws BusinessException 资讯不存在或不在加工中
     */
    public void processItems(List<Long> itemIds) {
        if (CollectionUtils.isEmpty(itemIds)) {
            return;
        }
        Optional<NewsAiModelConfig> config = newsAiModelSettingService.current();
        if (config.isEmpty()) {
            newsItemMapper.markPendingWithoutProcessing(itemIds);
            return;
        }
        NewsAiModelConfig model = config.get();
        List<NewsAiFieldPO> enabledFields = newsAiFieldCatalog.listEnabled();
        for (List<Long> batch : Lists.partition(itemIds, BATCH_SIZE)) {
            List<NewsAiCall> calls = batch.stream()
                    .map(itemId -> loadItem(itemId, NewsItemStatus.PROCESSING))
                    .map(item -> start(item, promptBuilder.selectApplicable(enabledFields, item.getContentType()), model))
                    .collect(Collectors.toList());
            calls.forEach(call -> finish(call, null, NewsItemStatus.PROCESSING));
        }
    }

    /**
     * 重新加工一条待审资讯：所有启用且适用的字段重新生成，人工修改过的字段保留。
     *
     * @param itemId   资讯
     * @param operator 操作人登录名
     * @throws BusinessException 资讯不存在或不在待审
     */
    public void reprocess(Long itemId, String operator) {
        NewsAiModelConfig model = newsAiModelSettingService.requireConfigured();
        NewsItemPO item = loadItem(itemId, NewsItemStatus.PENDING);
        List<NewsAiFieldPO> fields = promptBuilder.selectApplicable(newsAiFieldCatalog.listEnabled(), item.getContentType());
        finish(start(item, fields, model), operator, NewsItemStatus.PENDING);
    }

    /**
     * 为一条待审资讯单独生成一个字段，生成的值写回资讯。
     *
     * @param itemId   资讯
     * @param fieldKey 字段标识
     * @param operator 操作人登录名
     * @return 字段值的 JSON 字符串：文本、单选为 JSON 字符串，数字为 JSON 数字，多选为 JSON 字符串数组
     * @throws BusinessException 资讯不存在或不在待审；字段不存在、不参与这条资讯的生成或已人工修改；生成失败
     */
    public String generateField(Long itemId, String fieldKey, String operator) {
        NewsAiModelConfig model = newsAiModelSettingService.requireConfigured();
        NewsItemPO item = loadItem(itemId, NewsItemStatus.PENDING);
        NewsAiFieldPO field = Optional.ofNullable(newsAiFieldMapper.selectByFieldKey(fieldKey))
                .filter(newsAiFieldCatalog::isAvailable)
                .orElseThrow(() -> new BusinessException("AI 字段不存在: " + fieldKey));
        if (!promptBuilder.isApplicable(field, item.getContentType())) {
            throw new BusinessException("字段「" + field.getName() + "」不参与这条资讯的生成：已停用、没有选项或只对文章生成");
        }
        if (StringListJson.read(item.getEditedKeysJson()).contains(fieldKey)) {
            throw new BusinessException("字段「" + field.getName() + "」已人工修改，不能重新生成");
        }
        NewsAiOutcome outcome = finish(start(item, Collections.singletonList(field), model), operator, NewsItemStatus.PENDING);
        return Optional.ofNullable(outcome.getGeneration().getValues().get(fieldKey))
                .map(JsonNode::toString)
                .orElseThrow(() -> new BusinessException("字段「" + field.getName() + "」生成失败："
                        + outcome.getError().orElse("模型输出不符合字段要求")));
    }

    /**
     * 发起一条资讯的模型调用；没有参与生成的字段时不调用。
     */
    private NewsAiCall start(NewsItemPO item, List<NewsAiFieldPO> fields, NewsAiModelConfig model) {
        if (fields.isEmpty()) {
            return new NewsAiCall(item, fields, null);
        }
        return new NewsAiCall(item, fields, modelInvoker.submit(model, promptBuilder.build(item, fields)));
    }

    /**
     * 等待模型返回，校验结果并写回资讯，追加处理记录。
     *
     * @param call           已发起的加工
     * @param operator       操作人登录名；自动加工为空
     * @param expectedStatus 写回时资讯应处的状态
     * @return 本次加工的结局
     */
    private NewsAiOutcome finish(NewsAiCall call, String operator, NewsItemStatus expectedStatus) {
        NewsAiOutcome outcome = call.getReply()
                .map(reply -> await(call, reply))
                .orElseGet(NewsAiOutcome::nothingToGenerate);
        apply(call.getItem().getId(), keysOf(call.getFields()), outcome, operator, expectedStatus);
        return outcome;
    }

    /**
     * 等待模型返回并校验输出。调用异常或输出无法解析时，参与生成的字段全部记为未生成，原因记入处理记录。
     */
    private NewsAiOutcome await(NewsAiCall call, CompletableFuture<NewsAiModelReply> future) {
        Long itemId = call.getItem().getId();
        List<String> fieldKeys = keysOf(call.getFields());
        NewsAiModelReply reply;
        try {
            reply = future.join();
        } catch (CompletionException e) {
            Throwable cause = Optional.ofNullable(e.getCause()).orElse(e);
            log.error("资讯加工模型调用失败, itemId={}, fieldKeys={}", itemId, fieldKeys, cause);
            return NewsAiOutcome.failed(fieldKeys, null, messageOf(cause));
        }
        try {
            return NewsAiOutcome.generated(outputValidator.validate(call.getFields(), reply.getContent()), reply.getTraceId());
        } catch (RuntimeException e) {
            log.error("资讯加工模型输出无法解析, itemId={}, traceId={}", itemId, reply.getTraceId(), e);
            return NewsAiOutcome.failed(fieldKeys, reply.getTraceId(), messageOf(e));
        }
    }

    /**
     * 把加工结局合并进资讯的最新数据：人工修改过的字段不动；本次参与生成的字段先移出未生成列表，再按本次结果记入；状态改为待审。
     */
    private void apply(Long itemId, List<String> fieldKeys, NewsAiOutcome outcome, String operator,
                       NewsItemStatus expectedStatus) {
        NewsItemPO item = loadItem(itemId, expectedStatus);
        Set<String> editedKeys = new LinkedHashSet<>(StringListJson.read(item.getEditedKeysJson()));
        ObjectNode values = (ObjectNode) JacksonUtil.readTree(item.getAiValuesJson());
        outcome.getGeneration().getValues().fields().forEachRemaining(entry -> {
            if (!editedKeys.contains(entry.getKey())) {
                values.set(entry.getKey(), entry.getValue());
            }
        });
        List<String> failedKeys = outcome.getGeneration().getFailedKeys().stream()
                .filter(key -> !editedKeys.contains(key))
                .collect(Collectors.toList());
        Set<String> itemFailedKeys = new LinkedHashSet<>(StringListJson.read(item.getAiFailedKeysJson()));
        itemFailedKeys.removeAll(fieldKeys);
        itemFailedKeys.addAll(failedKeys);

        item.setAiValuesJson(values.toString());
        item.setAiFailedKeysJson(StringListJson.write(itemFailedKeys));
        item.setStatus(NewsItemStatus.PENDING);

        NewsItemLogPO itemLog = new NewsItemLogPO();
        itemLog.setItemId(itemId);
        itemLog.setAction(failedKeys.isEmpty() ? NewsItemAction.PROCESSED : NewsItemAction.PROCESS_FAILED);
        itemLog.setOperator(operator);
        itemLog.setDetailJson(JacksonUtil.writeValueAsString(new NewsAiProcessLogDetail(fieldKeys, failedKeys,
                outcome.getTraceId().orElse(null), outcome.getError().orElse(null))));
        transactionTemplate.executeWithoutResult(status -> {
            newsItemMapper.updateById(item);
            newsItemLogMapper.insert(itemLog);
        });
    }

    /**
     * 查出资讯并校验状态。
     *
     * @throws BusinessException 资讯不存在或状态不符
     */
    private NewsItemPO loadItem(Long itemId, NewsItemStatus expectedStatus) {
        NewsItemPO item = Optional.ofNullable(newsItemMapper.selectById(itemId))
                .orElseThrow(() -> new BusinessException("资讯不存在: " + itemId));
        if (item.getStatus() != expectedStatus) {
            throw new BusinessException("资讯当前为「" + item.getStatus().getDesc() + "」，只有「"
                    + expectedStatus.getDesc() + "」的资讯可以加工: " + itemId);
        }
        return item;
    }

    private List<String> keysOf(List<NewsAiFieldPO> fields) {
        return fields.stream().map(NewsAiFieldPO::getFieldKey).collect(Collectors.toList());
    }

    private String messageOf(Throwable throwable) {
        return Optional.ofNullable(throwable.getMessage())
                .filter(StringUtils::isNotBlank)
                .orElse(throwable.getClass().getSimpleName());
    }
}
