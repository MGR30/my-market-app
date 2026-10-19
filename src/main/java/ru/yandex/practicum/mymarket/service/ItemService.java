package ru.yandex.practicum.mymarket.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ItemService {

    private static final int ROW_SIZE = 3;

    private final ItemRepository itemRepository;
    private final CartItemRepository cartItemRepository;

    @Transactional(readOnly = true)
    public ItemPageResult findItems(String search, SortType sort, int pageNumber, int pageSize) {
        Sort jpaSort = switch (sort) {
            case ALPHA -> Sort.by("title").ascending();
            case PRICE -> Sort.by("price").ascending();
            case NO -> Sort.unsorted();
        };

        int pageIndex = Math.max(pageNumber - 1, 0);
        Pageable pageable = PageRequest.of(pageIndex, pageSize, jpaSort);
        Page<Item> page = (search == null || search.isBlank())
                ? itemRepository.findAll(pageable)
                : itemRepository.findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
                search, search, pageable);

        List<Long> ids = page.getContent().stream().map(Item::getId).toList();
        Map<Long, Integer> cartCounts = ids.isEmpty()
                ? Map.of()
                : cartItemRepository.findAllByItemIdIn(ids).stream()
                .collect(Collectors.toMap(CartItem::getItemId, CartItem::getQuantity));

        List<ItemDto> itemDtos = page.getContent().stream()
                .map(item -> toDto(item, cartCounts.getOrDefault(item.getId(), 0)))
                .toList();

        PagingDto pagingDto = new PagingDto(pageSize, pageNumber, page.hasPrevious(), page.hasNext());
        return new ItemPageResult(toRows(itemDtos), pagingDto);
    }

    @Transactional(readOnly = true)
    public ItemDto findById(long id) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Товар не найден: " + id));
        int count = cartItemRepository.findByItemId(id).map(CartItem::getQuantity).orElse(0);
        return toDto(item, count);
    }

    @Transactional(readOnly = true)
    public List<ItemDto> findCartItems() {
        List<CartItem> cartItems = cartItemRepository.findAll();
        if (cartItems.isEmpty()) return List.of();

        Map<Long, Item> items = itemRepository.findAllById(
                        cartItems.stream().map(CartItem::getItemId).toList())
                .stream().collect(Collectors.toMap(Item::getId, Function.identity()));

        return cartItems.stream()
                .filter(ci -> items.containsKey(ci.getItemId()))
                .map(ci -> {
                    Item it = items.get(ci.getItemId());
                    return new ItemDto(it.getId(), it.getTitle(), it.getDescription(),
                            it.getImgPath(), it.getPrice(), ci.getQuantity());
                })
                .toList();
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
