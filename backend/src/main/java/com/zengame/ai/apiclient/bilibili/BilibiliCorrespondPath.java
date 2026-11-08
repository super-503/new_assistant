package com.zengame.ai.apiclient.bilibili;

import cn.hutool.core.util.HexUtil;

import javax.crypto.Cipher;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * cookie 刷新流程中的 correspondPath：用 B站网页端公钥对 {@code refresh_{毫秒时间戳}} 做 RSA-OAEP（SHA-256）加密，
 * 结果转成小写 hex，用于请求 correspond 页面取 refresh_csrf。
 */
public final class BilibiliCorrespondPath {

    private static final String PUBLIC_KEY_BASE64 = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDLgd2OAkcGVtoE3ThUREbio0Eg"
            + "Uc/prcajMKXvkCKFCWhJYJcLkcM2DKKcSeFpD/j6Boy538YXnR6VhcuUJOhH2x71"
            + "nzPjfdTcqMz7djHum0qSZA0AyCBDABUqCrfNgCiJ00Ra7GmRj+YCK1NJEuewlb40"
            + "JNrRuoEUXpabUzGB8QIDAQAB";

    private static final OAEPParameterSpec OAEP_SHA256 = new OAEPParameterSpec(
            "SHA-256", "MGF1", MGF1ParameterSpec.SHA256, PSource.PSpecified.DEFAULT);

    private BilibiliCorrespondPath() {
    }

    /**
     * @param timestampMillis 当前毫秒时间戳
     * @return correspondPath
     * @throws IllegalStateException JDK 不支持所需的加密算法
     */
    public static String generate(long timestampMillis) {
        try {
            PublicKey publicKey = KeyFactory.getInstance("RSA")
                    .generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(PUBLIC_KEY_BASE64)));
            Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPPadding");
            cipher.init(Cipher.ENCRYPT_MODE, publicKey, OAEP_SHA256);
            byte[] encrypted = cipher.doFinal(("refresh_" + timestampMillis).getBytes(StandardCharsets.UTF_8));
            return HexUtil.encodeHexStr(encrypted);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("生成 B站 correspondPath 失败", e);
        }
    }
}
