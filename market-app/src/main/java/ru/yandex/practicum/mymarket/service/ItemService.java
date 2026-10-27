package ru.yandex.practicum.mymarket.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
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

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class ItemService {

    private static final int ROW_SIZE = 3;
    private static final String ITEM_KEY_PREFIX = "item:";
    private static final String LIST_KEY_PREFIX = "items:list:";

    private final DatabaseClient databaseClient;
    private final ItemRepository itemRepository;
    private final CartItemRepository cartItemRepository;
    private final ReactiveRedisTemplate<String, Object> redis;

    @Value("${cache.items.ttl-seconds:120}")
    private long ttlSeconds;

    public Mono<ItemPageResult> findItems(String search, SortType sort, int pageNumber, int pageSize) {
        String cacheKey = LIST_KEY_PREFIX + safe(search) + ":" + sort + ":" + pageNumber + ":" + pageSize;

        return redis.opsForValue().get(cacheKey)
                .cast(ItemPageResult.class)
                .switchIfEmpty(Mono.defer(() -> loadItemsFromDb(search, sort, pageNumber, pageSize)
                        .flatMap(result -> redis.opsForValue()
                                .set(cacheKey, result, Duration.ofSeconds(ttlSeconds))
                                .thenReturn(result))))
                .flatMap(this::enrichPageWithCounts);
    }

    private Mono<ItemPageResult> loadItemsFromDb(String search, SortType sort, int pageNumber, int pageSize) {
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

        var selectSpec = databaseClient.sql(selectSql)
                .bind("limit", pageSize).bind("offset", offset);
        var countSpec = databaseClient.sql(countSql);

        if (search != null && !search.isBlank()) {
            selectSpec = selectSpec.bind("search", "%" + search + "%");
            countSpec = countSpec.bind("search", "%" + search + "%");
        }

        Flux<Item> itemsFlux = selectSpec.map((row, meta) -> Item.builder()
                .id(row.get("id", Long.class))
                .title(row.get("title", String.class))
                .description(row.get("description", String.class))
                .imgPath(row.get("img_path", String.class))
                .price(row.get("price", Long.class))
                .build()).all();

        Mono<Long> totalMono = countSpec.map((row, meta) -> row.get("cnt", Long.class))
                .one().defaultIfEmpty(0L);

        return Mono.zip(itemsFlux.collectList(), totalMono).map(tuple -> {
            List<Item> items = tuple.getT1();
            long total = tuple.getT2();

            List<ItemDto> dtos = items.stream()
                    .map(item -> toDto(item, 0))
                    .toList();

            boolean hasNext = offset + pageSize < total;
            boolean hasPrevious = pageIndex > 0;

            return new ItemPageResult(
                    toRows(dtos),
                    new PagingDto(pageSize, pageNumber, hasPrevious, hasNext));
        });
    }

    private Mono<ItemPageResult> enrichPageWithCounts(ItemPageResult cached) {
        List<Long> ids = cached.items().stream()
                .flatMap(List::stream)
                .filter(dto -> dto.id() != -1)
                .map(ItemDto::id)
                .toList();

        if (ids.isEmpty()) {
            return Mono.just(cached);
        }

        return cartItemRepository.findAllByItemIdIn(ids)
                .collectMap(CartItem::getItemId, CartItem::getQuantity)
                .map(counts -> {
                    List<List<ItemDto>> enrichedRows = cached.items().stream()
                            .map(row -> row.stream()
                                    .map(dto -> dto.id() == -1
                                            ? dto
                                            : new ItemDto(dto.id(), dto.title(), dto.description(),
                                            dto.imgPath(), dto.price(),
                                            counts.getOrDefault(dto.id(), 0)))
                                    .toList())
                            .toList();
                    return new ItemPageResult(enrichedRows, cached.paging());
                });
    }

    public Mono<ItemDto> findById(long id) {
        return findItemFromCache(id)
                .flatMap(dto -> cartItemRepository.findByItemId(id)
                        .map(CartItem::getQuantity)
                        .defaultIfEmpty(0)
                        .map(count -> new ItemDto(
                                dto.id(), dto.title(), dto.description(),
                                dto.imgPath(), dto.price(), count)));
    }

    public Mono<List<ItemDto>> findCartItems() {
        return cartItemRepository.findAll()
                .concatMap(ci -> findItemFromCache(ci.getItemId())
                        .map(dto -> new ItemDto(dto.id(), dto.title(), dto.description(),
                                dto.imgPath(), dto.price(), ci.getQuantity()))
                        .onErrorResume(NoSuchElementException.class, e -> Mono.empty()))
                .collectList();
    }

    private Mono<ItemDto> findItemFromCache(long id) {
        String key = ITEM_KEY_PREFIX + id;
        return redis.opsForValue().get(key)
                .cast(ItemDto.class)
                .switchIfEmpty(Mono.defer(() ->
                        itemRepository.findById(id)
                                .switchIfEmpty(Mono.error(() ->
                                        new NoSuchElementException("Товар не найден: " + id)))
                                .map(item -> toDto(item, 0))
                                .flatMap(dto -> redis.opsForValue()
                                        .set(key, dto, Duration.ofSeconds(ttlSeconds))
                                        .thenReturn(dto))));
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private ItemDto toDto(Item item, int count) {
        return new ItemDto(item.getId(), item.getTitle(), item.getDescription(),
                item.getImgPath(), item.getPrice(), count);
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
            while (current.size() < ROW_SIZE) current.add(ItemDto.stub());
            rows.add(current);
        }
        return rows;
    }

    public record ItemPageResult(List<List<ItemDto>> items, PagingDto paging) {
    }
}