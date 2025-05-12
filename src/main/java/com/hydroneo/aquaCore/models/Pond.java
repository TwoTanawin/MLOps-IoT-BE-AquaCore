package com.hydroneo.aquaCore.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("ponds")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Pond {
    @Id
    private Long id;

    private String address;

    @Column("serial_number")
    private String serialNumber;

    @Column("user_id")
    private Long userId;
}
