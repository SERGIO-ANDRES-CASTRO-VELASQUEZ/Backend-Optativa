package com.sports.backend.dto;

import com.sports.backend.model.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record CreateRentalRequest(

        @NotEmpty(message = "Debe incluir al menos un producto")
        List<@Valid RentalItemRequest> items,

        @NotNull(message = "La fecha de inicio es obligatoria")
        LocalDate startDate,

        @NotNull(message = "La fecha de fin es obligatoria")
        LocalDate endDate,

        @NotNull(message = "El método de pago es obligatorio")
        PaymentMethod paymentMethod
) {}
