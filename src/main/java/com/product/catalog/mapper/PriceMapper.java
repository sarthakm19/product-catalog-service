package com.product.catalog.mapper;

import com.product.catalog.domain.PriceDomain;
import com.product.catalog.dto.PriceDto;
import com.product.catalog.entity.Price;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface PriceMapper {

    PriceDomain entityToDomain(Price entity);

    Price domainToEntity(PriceDomain domain);

    PriceDomain dtoToDomain(PriceDto dto);

    PriceDto domainToDto(PriceDomain domain);

    Price dtoToEntity(PriceDto dto);

    PriceDto entityToDto(Price entity);
}

