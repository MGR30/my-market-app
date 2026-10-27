package ru.yandex.practicum.mymarket.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Item {
    @Id
    private Long id;

    private String title;
    private String description;

    @Column("img_path")
    private String imgPath;

    private Long price;
}
