package musicEventsNearMe.utilities;

import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import org.modelmapper.Converter;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.modelmapper.spi.MappingContext;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import musicEventsNearMe.dto.MusicEventDTO;
import musicEventsNearMe.entities.MusicEvent;
import musicEventsNearMe.interfaces.BaseEntity;
import musicEventsNearMe.interfaces.BaseRepository;

@Service
public class DataUtilities {

    private final ModelMapper modelMapper = new ModelMapper();

    public <D, T> D getDTOEntityFromObject(T data, Type type) {
        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);

        if (type == MusicEventDTO.class) {
            Converter<String, LocalDateTime> stringToLocalDateTimeConverterStartDate = new Converter<String, LocalDateTime>() {
                @Override
                public LocalDateTime convert(MappingContext<String, LocalDateTime> context) {
                    if (context.getSource() == null) {
                        return null;
                    }
                    String str = context.getSource();
                    try {
                        return LocalDateTime.parse(str, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
                    } catch (DateTimeParseException e) {
                        return LocalDate.parse(str, DateTimeFormatter.ofPattern("yyyy-MM-dd")).atStartOfDay();
                    }
                }
            };
            modelMapper.addConverter(stringToLocalDateTimeConverterStartDate);

            modelMapper.typeMap(MusicEvent.class, MusicEventDTO.class)
                    .addMappings(mapping -> {
                        mapping.using(stringToLocalDateTimeConverterStartDate)
                                .map(MusicEvent::getStartDate, MusicEventDTO::setStartDate);
                    });
        }
        return modelMapper.map(data, type);
    }

    public <T extends BaseEntity> T checkForDuplicateDataAndReturnEntity(T entity,
            BaseRepository<T> repository) {
        return repository.findByIdentifier(entity.getIdentifier()).orElse(null);
    }

    public <T extends BaseEntity> boolean checkForDuplicateDataAndReturnBoolean(T entity,
            BaseRepository<T> repository) {
        return repository.findByIdentifier(entity.getIdentifier()).isPresent();
    }

    public <T extends BaseEntity> T updateEntity(T newEntity, T existingEntity, JpaRepository<T, Long> repository) {
        if (newEntity != null && existingEntity != null) {
            Long id = existingEntity.getId();
            modelMapper.map(newEntity, existingEntity);
            existingEntity.setTimeRecordWasEntered(LocalDateTime.now());
            existingEntity.setId(id);
            return repository.saveAndFlush(existingEntity);
        }
        return null;
    }

}
