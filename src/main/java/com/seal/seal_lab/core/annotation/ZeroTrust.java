package com.seal.seal_lab.core.annotation;

import java.lang.annotation.*;

/**
 * [Zero Trust Architecture - PAP]
 * 이 어노테이션이 붙은 메소드는 신뢰 점수 기반의 동적 접근 제어를 받습니다.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ZeroTrust {
    int requiredScore() default 70; // 정책 설정 (최소 요구 점수)
}
