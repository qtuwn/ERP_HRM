package com.vthr.erp_hrm.infrastructure.controller.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Size(min = 8, max = 72)
    @Pattern(
            regexp = "^(?=.*[a-zA-Z])(?=.*\\d).+$",
            message = "password must contain at least 1 letter and 1 number"
    )
    private String password;

    @NotBlank
    private String fullName;

    private String phone;

    // CANDIDATE | HR
    private String accountType;

    private String companyName;

    private String department;
}
