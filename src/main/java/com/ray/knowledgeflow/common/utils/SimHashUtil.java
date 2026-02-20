package com.ray.knowledgeflow.common.utils;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.StringTokenizer;

/**
 * SimHash 文本指纹工具
 * 用于快速检测文本相似度
 *
 * @author Ray
 * @since 2024-01-17
 */
public class SimHashUtil {

    private static final int HASH_BITS = 64;

    /**
     * 计算文本的 SimHash 值
     *
     * @param text 文本内容
     * @return SimHash 值
     */
    public static long simHash(String text) {
        if (text == null || text.trim().isEmpty()) {
            return 0;
        }

        int[] v = new int[HASH_BITS];

        // 分词（简单按空格分词）
        StringTokenizer tokenizer = new StringTokenizer(text);
        while (tokenizer.hasMoreTokens()) {
            String word = tokenizer.nextToken();
            long hash = hash(word);

            // 将 hash 值的每一位累加到 v 数组
            for (int i = 0; i < HASH_BITS; i++) {
                if (((hash >> i) & 1) == 1) {
                    v[i]++;
                } else {
                    v[i]--;
                }
            }
        }

        // 生成最终的 SimHash 值
        long simHash = 0;
        for (int i = 0; i < HASH_BITS; i++) {
            if (v[i] > 0) {
                simHash |= (1L << i);
            }
        }

        return simHash;
    }

    /**
     * 计算两个 SimHash 值的汉明距离
     *
     * @param hash1 SimHash 值1
     * @param hash2 SimHash 值2
     * @return 汉明距离
     */
    public static int hammingDistance(long hash1, long hash2) {
        long xor = hash1 ^ hash2;
        int distance = 0;

        while (xor != 0) {
            distance++;
            xor &= (xor - 1); // 清除最右边的1
        }

        return distance;
    }

    /**
     * 使用 MD5 计算字符串的 hash 值
     *
     * @param str 字符串
     * @return hash 值
     */
    private static long hash(String str) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] bytes = md.digest(str.getBytes());
            BigInteger bigInt = new BigInteger(1, bytes);
            return bigInt.longValue();
        } catch (Exception e) {
            return str.hashCode();
        }
    }
}
