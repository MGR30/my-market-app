package ru.yandex.practicum.mymarket.dto;

public record ItemDto(
        long id,
        String title,
        String description,
        String imgPath,
        long price,
        int count
) {
    public static ItemDto stub() {
        return new ItemDto(-1, "", "", "", 0, 0);
    }
}
