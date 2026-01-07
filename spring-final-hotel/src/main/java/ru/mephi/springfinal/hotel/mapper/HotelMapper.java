package ru.mephi.springfinal.hotel.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import ru.mephi.springfinal.hotel.dto.HotelDto;
import ru.mephi.springfinal.hotel.entity.Hotel;

import java.util.List;

@Mapper(componentModel = "spring")
public interface HotelMapper {

    @Named("toDto")
    @Mapping(target = "rooms", ignore = true)
    HotelDto toDto(Hotel hotel);

    @Named("toDtoWithRooms")
    HotelDto toDtoWithRooms(Hotel hotel);

    @Named("toDtoList")
    default List<HotelDto> toDtoList(List<Hotel> hotels) {
        return hotels.stream()
                .map(this::toDto)
                .toList();
    }

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "rooms", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Hotel toEntity(HotelDto dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "rooms", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(HotelDto dto, @MappingTarget Hotel hotel);
}

