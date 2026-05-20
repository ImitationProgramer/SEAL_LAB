package com.seal.seal_lab.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserProfileUpdateDto {

    @NotBlank(message = "이름은 비워둘 수 없습니다.")
    @Size(max = 100, message = "이름은 100자를 넘길 수 없습니다.")
    private String name;

    @Email(message = "유효한 이메일 형식이 아닙니다.")
    @Size(max = 255, message = "이메일은 255자를 넘길 수 없습니다.")
    private String email;

    @Size(max = 255, message = "학과/소속은 255자를 넘길 수 없습니다.")
    private String department;

    @Size(max = 500, message = "키워드는 500자를 넘길 수 없습니다.")
    private String keywords;

    @Size(max = 1000, message = "소개는 1000자를 넘길 수 없습니다.")
    private String bio;
}
