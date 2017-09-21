package com.db.awmd.challenge.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Preconditions;
import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
public class MoneyTransfer {

    @NotNull
    @NotEmpty
    private final String fromAccountId;

    @NotNull
    @NotEmpty
    private final String toAccountId;

    @NotNull
    @Min(value = 0, message = "Minimum amount that can be transferred.")
    private BigDecimal amount;

    @JsonCreator
    public MoneyTransfer(@JsonProperty("fromAccountId") String fromAccountId,
                         @JsonProperty("toAccountId") String toAccountId,
                   @JsonProperty("amount") BigDecimal amount) {
        this.fromAccountId = fromAccountId;
        this.toAccountId = toAccountId;
        this.amount = amount;
    }

}
