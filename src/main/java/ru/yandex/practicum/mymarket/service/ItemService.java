package ru.yandex.practicum.mymarket.service;

import lombok.RequiredArgsConstructor;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.dto.ItemDto;
import ru.yandex.practicum.mymarket.dto.PagingDto;
import ru.yandex.practicum.mymarket.dto.SortType;
import ru.yandex.practicum.mymarket.model.CartItem;
import ru.yandex.practicum.mymarket.model.Item;
import ru.yandex.practicum.mymarket.repository.CartItemRepository;
import ru.yandex.practicum.mymarket.repository.ItemRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class ItemService {

    private static final int ROW_SIZE = 3;

    private final DatabaseClient databaseClient;
    private final ItemRepository itemRepository;
    private final CartItemRepository cartItemRepository;

    public Mono<ItemPageResult> findItems(String search, SortType sort, int pageNumber, int pageSize) {
        int pageIndex = Math.max(pageNumber - 1, 0);
        long offset = (long) pageIndex * pageSize;

        String where = (search == null || search.isBlank())
                ? ""
                : " WHERE LOWER(title) LIKE LOWER(:search) OR LOWER(description) LIKE LOWER(:search)";

        String order = switch (sort) {
            case ALPHA -> "title ASC";
            case PRICE -> "price ASC";
            case NO -> "id ASC";
        };

        String selectSql = "SELECT id, title, description, img_path, price FROM items"
                + where + " ORDER BY " + order + " LIMIT :limit OFFSET :offset";
        String countSql = "SELECT COUNT(*) AS cnt FROM items" + where;

        DatabaseClient.GenericExecuteSpec selectSpec = databaseClient.sql(selectSql)
                .bind("limit", pageSize)
                .bind("offset", offset);

        DatabaseClient.GenericExecuteSpec countSpec = databaseClient.sql(countSql);

        if (search != null && !search.isBlank()) {
            selectSpec = selectSpec.bind("search", "%" + search + "%");
            countSpec = countSpec.bind("search", "%" + search + "%");
        }

        Flux<Item> itemsFlux = selectSpec
                .map((row, meta) -> Item.builder()
                        .id(row.get("id", Long.class))
                        .title(row.get("title", String.class))
                        .description(row.get("description", String.class))
                        .imgPath(row.get("img_path", String.class))
                        .price(row.get("price", Long.class))
                        .build())
                .all();

        Mono<Long> totalMono = countSpec
                .map((row, meta) -> row.get("cnt", Long.class))
                .one()
                .defaultIfEmpty(0L);

        return Mono.zip(itemsFlux.collectList(), totalMono)
                .flatMap(tuple -> {
                    List<Item> items = tuple.getT1();
                    long total = tuple.getT2();

                    List<Long> ids = items.stream().map(Item::getId).toList();
                    Mono<Map<Long, Integer>> countsMono = ids.isEmpty()
                            ? Mono.just(Map.of())
                            : cartItemRepository.findAllByItemIdIn(ids)
                            .collectMap(CartItem::getItemId, CartItem::getQuantity);

                    return countsMono.map(counts -> {
                        List<ItemDto> dtos = items.stream()
                                .map(item -> toDto(item, counts.getOrDefault(item.getId(), 0)))
                                .toList();

                        boolean hasNext = offset + pageSize < total;
                        boolean hasPrevious = pageIndex > 0;

                        return new ItemPageResult(
                                toRows(dtos),
                                new PagingDto(pageSize, pageNumber, hasPrevious, hasNext)
                        );
                    });
                });
    }

    public Mono<ItemDto> findById(long id) {
        return itemRepository.findById(id)
                .switchIfEmpty(Mono.error(() -> new NoSuchElementException("Товар не найден: " + id)))
                .flatMap(item -> cartItemRepository.findByItemId(id)
                        .map(CartItem::getQuantity)
                        .defaultIfEmpty(0)
                        .map(count -> toDto(item, count)));
    }

    public Mono<List<ItemDto>> findCartItems() {
        return cartItemRepository.findAll().collectList()
                .flatMap(cartItems -> {
                    if (cartItems.isEmpty()) {
                        return Mono.just(List.of());
                    }

                    List<Long> ids = cartItems.stream().map(CartItem::getItemId).toList();

                    return itemRepository.findAllById(ids)
                            .collectMap(Item::getId)
                            .map(items -> cartItems.stream()
                                    .filter(ci -> items.containsKey(ci.getItemId()))
                                    .map(ci -> {
                                        Item item = items.get(ci.getItemId());
                                        return new ItemDto(
                                                item.getId(),
                                                item.getTitle(),
                                                item.getDescription(),
                                                item.getImgPath(),
                                                item.getPrice(),
                                                ci.getQuantity()
                                        );
                                    })
                                    .toList());
                });
    }

    private ItemDto toDto(Item item, int count) {
        return new ItemDto(
                item.getId(),
                item.getTitle(),
                item.getDescription(),
                item.getImgPath(),
                item.getPrice(),
                count
        );
    }

    private List<List<ItemDto>> toRows(List<ItemDto> items) {
        List<List<ItemDto>> rows = new ArrayList<>();
        List<ItemDto> current = new ArrayList<>();

        for (ItemDto dto : items) {
            current.add(dto);
            if (current.size() == ROW_SIZE) {
                rows.add(current);
                current = new ArrayList<>();
            }
        }

        if (!current.isEmpty()) {
            while (current.size() < ROW_SIZE) {
                current.add(ItemDto.stub());
            }
            rows.add(current);
        }

        return rows;
    }

    public record ItemPageResult(List<List<ItemDto>> items, PagingDto paging) {
    }
}
