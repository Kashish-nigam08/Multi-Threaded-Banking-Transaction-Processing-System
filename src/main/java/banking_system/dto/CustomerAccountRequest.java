package banking_system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CustomerAccountRequest(

        @NotBlank(message = "Account number is required")
        @Pattern(
                regexp = "[0-9]{10,16}",
                message = "Account number must contain 10 to 16 digits"
        )
        String accountNumber

) {
}
