package com.example.springjwt.pantry;

import com.example.springjwt.ingredient.IngredientMaster;
import com.example.springjwt.ingredient.UnitEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "pantry_history",
        indexes = {
                @Index(name = "ix_hist_pantry", columnList = "pantry_id"),
                @Index(name = "ix_hist_pantry_ing", columnList = "pantry_id, ingredient_id")
        }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PantryHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pantry_id", foreignKey = @ForeignKey(name = "fk_hist_pantry"))
    private Pantry pantry;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ingredient_id", foreignKey = @ForeignKey(name = "fk_hist_ing"))
    private IngredientMaster ingredient;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "unit_id", foreignKey = @ForeignKey(name = "fk_hist_unit"))
    private UnitEntity unit;

    /** +입고, -사용/폐기/조정 */
    @Column(name = "change_qty", precision = 12, scale = 3, nullable = false)
    private BigDecimal changeQty;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private HistoryAction action;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_id", foreignKey = @ForeignKey(name = "fk_hist_stock"))
    private PantryStock stock;

    @Column(name = "ref_type", length = 30)
    private String refType;

    @Column(name = "ref_id")
    private Long refId;

    @Column(length = 255)
    private String note;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    private void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
