package org.wltea.analyzer.lucene;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayDeque;
import java.util.Queue;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;

import org.utils.FPEChinese;
import org.utils.UnicodeUtils;

/**
 * 加密TokenFilter
 */
public final class SecurityTokenFilter extends TokenFilter {

    // 词性
    private final TypeAttribute typeAttribute = addAttribute(TypeAttribute.class);
    // 当前词
    private final CharTermAttribute charTermAttribute = addAttribute(CharTermAttribute.class);
    // 是否保留原词
    private final boolean original;
    // 待输出加密队列
    private final Queue<CharSequence> queue;

    public SecurityTokenFilter(TokenStream input, boolean original) {
        super(input);
        this.original = original;
        this.queue = new ArrayDeque<>();
    }


    @Override
    public boolean incrementToken() throws IOException {
        while (true) {
            CharSequence term = queue.poll();
            if (term != null) {
                typeAttribute.setType("encrypt");
                charTermAttribute.setEmpty().append(term);
                return true;
            }
            if (input.incrementToken()) {
                String text = charTermAttribute.toString();
                // 模拟加密
//                queue.offer(UnicodeUtils.encryptStr(text));
                try {
                    queue.offer(FPEChinese.encrypt(text));
                } catch (NoSuchAlgorithmException e) {
                    throw new RuntimeException(e);
                }
                if (original) {
                    return true;
                }
            } else {
                return false;
            }
        }
    }

}
