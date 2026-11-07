    package com.TrackFlow.shipment.dtos.entitiesDTO;

    import com.TrackFlow.shipment.entities.enums.IdType;
    import jakarta.validation.constraints.Email;
    import jakarta.validation.constraints.NotBlank;
    import jakarta.validation.constraints.NotNull;
    import jakarta.validation.constraints.Size;

    public record CustomerDTO(
            @NotNull IdType idType,
            @NotBlank @Size(max=30) String idNumber,
            @NotBlank @Size(max=100) String firstName,
            @NotBlank @Size(max=100) String lastName,
            @NotBlank @Size(max = 20) String phone,
            @Email @Size(max = 255) String email
            // Note: verification will happen in customer service
    ) {
        @Override
        public String toString() {
            return "CustomerDTO[idType=" + idType + ", idNumber=***]";
        }
    }
