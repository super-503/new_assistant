package com.zengame.ai.business.news.channel;

import com.google.common.collect.ImmutableSet;
import com.helger.css.ECSSVersion;
import com.helger.css.decl.CSSDeclaration;
import com.helger.css.decl.CSSDeclarationList;
import com.helger.css.handler.DoNothingCSSParseExceptionCallback;
import com.helger.css.reader.CSSReaderDeclarationList;
import com.helger.css.reader.CSSReaderSettings;
import com.helger.css.reader.errorhandler.DoNothingCSSInterpretErrorHandler;
import com.helger.css.reader.errorhandler.DoNothingCSSParseErrorHandler;
import com.helger.css.writer.CSSWriterSettings;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Cleaner;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 资讯正文清洗：插件拼装好的正文 HTML 入库前按白名单过滤。
 *
 * <p>保留段落、标题、列表、引用、代码块、表格、加粗、斜体、链接、图片、音频、视频和插件声明的播放器 iframe；
 * 去掉 {@code class}、{@code <style>}、脚本和布局容器。行内样式只保留排版属性，文字颜色和字号跟随阅读样式。
 */
@Component
public class ContentSanitizer {

    /**
     * 行内样式允许保留的排版属性：margin、padding、border、border-radius 含各方向的展开写法；
     * border 只保留宽度和线型的展开写法，边框颜色跟随阅读样式
     */
    private static final Set<String> ALLOWED_STYLE_PROPERTIES = ImmutableSet.<String>builder()
            .add("text-align", "line-height", "text-indent", "letter-spacing", "font-weight", "font-style",
                    "text-decoration", "display", "width", "max-width")
            .add("margin", "margin-top", "margin-right", "margin-bottom", "margin-left")
            .add("padding", "padding-top", "padding-right", "padding-bottom", "padding-left")
            .add("border", "border-top", "border-right", "border-bottom", "border-left")
            .add("border-width", "border-top-width", "border-right-width", "border-bottom-width", "border-left-width")
            .add("border-style", "border-top-style", "border-right-style", "border-bottom-style", "border-left-style")
            .add("border-radius", "border-top-left-radius", "border-top-right-radius", "border-bottom-right-radius",
                    "border-bottom-left-radius")
            .build();

    private static final Safelist SAFELIST = Safelist.relaxed()
            .addTags("audio", "video", "source", "iframe")
            .addAttributes("audio", "src", "controls")
            .addAttributes("video", "src", "poster", "controls")
            .addAttributes("source", "src", "type")
            .addAttributes("iframe", "src", "width", "height", "allowfullscreen", "frameborder")
            .addAttributes(":all", "style")
            .addProtocols("audio", "src", "http", "https")
            .addProtocols("video", "src", "http", "https")
            .addProtocols("video", "poster", "http", "https")
            .addProtocols("source", "src", "http", "https")
            .addProtocols("iframe", "src", "http", "https");

    private static final CSSReaderSettings CSS_READER_SETTINGS = new CSSReaderSettings()
            .setCSSVersion(ECSSVersion.CSS30)
            .setBrowserCompliantMode(true)
            .setCustomErrorHandler(new DoNothingCSSParseErrorHandler())
            .setCustomExceptionHandler(new DoNothingCSSParseExceptionCallback())
            .setInterpretErrorHandler(new DoNothingCSSInterpretErrorHandler());

    private static final CSSWriterSettings CSS_WRITER_SETTINGS = new CSSWriterSettings(ECSSVersion.CSS30, true);

    /**
     * 按白名单清洗正文。
     *
     * @param html         插件拼装好的正文 HTML
     * @param embedDomains 所属渠道允许内嵌的播放器域名，按 iframe src 的主机名完全匹配
     * @return 清洗后的正文 HTML；没有可保留的内容时为空字符串
     */
    public String sanitize(String html, List<String> embedDomains) {
        Document cleaned = new Cleaner(SAFELIST).clean(Jsoup.parseBodyFragment(html));
        cleaned.outputSettings().prettyPrint(false);
        cleaned.body().select("iframe").stream()
                .filter(iframe -> !isEmbeddable(iframe, embedDomains))
                .forEach(Element::remove);
        cleaned.body().select("[style]").forEach(this::filterStyle);
        return cleaned.body().html().trim();
    }

    /**
     * iframe 的 src 主机名在渠道声明的播放器域名内才可保留。
     */
    private boolean isEmbeddable(Element iframe, List<String> embedDomains) {
        return Optional.of(iframe.attr("src"))
                .filter(StringUtils::isNotBlank)
                .map(src -> UriComponentsBuilder.fromUriString(src).build().getHost())
                .map(host -> host.toLowerCase(Locale.ROOT))
                .filter(embedDomains::contains)
                .isPresent();
    }

    /**
     * 行内样式只保留排版属性；没有可保留的属性或样式无法解析时删除整个 style 属性。
     */
    private void filterStyle(Element element) {
        String style = Optional.ofNullable(CSSReaderDeclarationList.readFromString(element.attr("style"), CSS_READER_SETTINGS))
                .map(CSSDeclarationList::getAllDeclarations)
                .map(declarations -> declarations.stream()
                        .filter(declaration -> ALLOWED_STYLE_PROPERTIES.contains(declaration.getProperty().toLowerCase(Locale.ROOT)))
                        .map(this::writeDeclaration)
                        .collect(Collectors.joining(";")))
                .orElse(StringUtils.EMPTY);
        if (StringUtils.isEmpty(style)) {
            element.removeAttr("style");
        } else {
            element.attr("style", style);
        }
    }

    private String writeDeclaration(CSSDeclaration declaration) {
        return declaration.getAsCSSString(CSS_WRITER_SETTINGS, 0);
    }
}
