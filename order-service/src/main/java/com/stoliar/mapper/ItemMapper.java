package com.stoliar.mapper;

import com.stoliar.dto.item.ItemCreateDto;
import com.stoliar.dto.item.ItemDto;
import com.stoliar.dto.orderItem.OrderItemDto;
import com.stoliar.entity.Item;
import com.stoliar.entity.OrderItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ItemMapper {

    @Mapping(target = "itemId", source = "item.id")
    @Mapping(target = "itemName", source = "item", qualifiedByName = "mapItemName")
    @Mapping(target = "itemPrice", source = "item", qualifiedByName = "mapItemPrice")
    OrderItemDto toDto(OrderItem orderItem);

    ItemDto toDto(Item item);

    List<ItemDto> toDtoList(List<Item> items);

    Item toEntity(ItemCreateDto itemCreateDto);

    ItemCreateDto toCreateDto(Item item);

    @Named("mapItemName")
    default String mapItemName(Item item) {
        return item != null ? item.getName() : null;
    }

    @Named("mapItemPrice")
    default Double mapItemPrice(Item item) {
        return item != null ? item.getPrice() : null;
    }
}