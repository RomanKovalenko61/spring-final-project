package ru.mephi.springfinal.hotel.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import ru.mephi.springfinal.hotel.dto.RoomDto;
import ru.mephi.springfinal.hotel.entity.Room;

import java.util.List;

@Mapper(componentModel = "spring")
public interface RoomMapper {

    @Mapping(source = "hotel.id", target = "hotelId")
    @Mapping(source = "type", target = "type", qualifiedByName = "roomTypeToString")
    RoomDto toDto(Room room);

    List<RoomDto> toDtoList(List<Room> rooms);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "hotel", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(source = "type", target = "type", qualifiedByName = "stringToRoomType")
    Room toEntity(RoomDto dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "hotel", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(source = "type", target = "type", qualifiedByName = "stringToRoomType")
    void updateEntity(RoomDto dto, @MappingTarget Room room);

    @Named("roomTypeToString")
    default String roomTypeToString(Room.RoomType type) {
        return type != null ? type.name() : null;
    }

    @Named("stringToRoomType")
    default Room.RoomType stringToRoomType(String type) {
        return type != null ? Room.RoomType.valueOf(type.toUpperCase()) : null;
    }
}

