package co.edu.uniandes.dse.parcial1.entities;

import jakarta.persistence.Entity;
import lombok.Data;

@Data
@Entity
public class RutaEntity extends BaseEntity {

    private String nombre;
    private String color;
    private String tipo;

}
