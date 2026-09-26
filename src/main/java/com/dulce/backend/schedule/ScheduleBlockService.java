package com.dulce.backend.schedule;

import com.dulce.backend.common.exception.BadRequestException;
import com.dulce.backend.common.exception.ResourceNotFoundException;
import com.dulce.backend.schedule.dto.ScheduleBlockCreateRequest;
import com.dulce.backend.schedule.dto.ScheduleBlockResponse;
import com.dulce.backend.schedule.dto.ScheduleBlockUpdateRequest;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ScheduleBlockService {

    private final ScheduleBlockRepository scheduleBlockRepository;

    public ScheduleBlockService(ScheduleBlockRepository scheduleBlockRepository) {
        this.scheduleBlockRepository = scheduleBlockRepository;
    }

    @Transactional(readOnly = true)
    public List<ScheduleBlockResponse> list(Boolean active) {
        List<ScheduleBlock> blocks =
                active == null
                        ? scheduleBlockRepository.findAll()
                        : scheduleBlockRepository.findByActive(active);
        return blocks.stream().map(this::toResponse).toList();
    }

    @Transactional
    public ScheduleBlockResponse create(ScheduleBlockCreateRequest request) {
        ScheduleBlock block = new ScheduleBlock();
        block.setType(request.type());
        block.setBlockDate(request.blockDate());
        block.setRecurrence(request.recurrence());
        block.setWeekday(request.weekday());
        block.setMonthDay(request.monthDay());
        block.setStartTime(request.startTime());
        block.setEndTime(request.endTime());
        block.setValidFrom(request.validFrom());
        block.setValidUntil(request.validUntil());
        block.setActive(true);

        applyEventualDefaults(block);
        validateConsistency(block);

        return toResponse(scheduleBlockRepository.save(block));
    }

    @Transactional
    public ScheduleBlockResponse update(Long id, ScheduleBlockUpdateRequest request) {
        ScheduleBlock block = findOrThrow(id);
        boolean typeSwitch = request.type() != null && request.type() != block.getType();

        if (typeSwitch) {
            block.setType(request.type());
            block.setBlockDate(request.blockDate());
            block.setRecurrence(request.recurrence());
            block.setWeekday(request.weekday());
            block.setMonthDay(request.monthDay());
            block.setValidFrom(request.validFrom());
            block.setValidUntil(request.validUntil());
        } else {
            if (request.blockDate() != null) {
                block.setBlockDate(request.blockDate());
            }

            if (request.recurrence() != null) {
                block.setRecurrence(request.recurrence());
            }

            if (request.weekday() != null) {
                block.setWeekday(request.weekday());
            }

            if (request.monthDay() != null) {
                block.setMonthDay(request.monthDay());
            }

            if (request.validFrom() != null) {
                block.setValidFrom(request.validFrom());
            }

            if (request.validUntil() != null) {
                block.setValidUntil(request.validUntil());
            }
        }

        if (request.startTime() != null) {
            block.setStartTime(request.startTime());
        }

        if (request.endTime() != null) {
            block.setEndTime(request.endTime());
        }

        applyEventualDefaults(block);
        validateConsistency(block);

        return toResponse(block);
    }

    @Transactional
    public void deactivate(Long id) {
        ScheduleBlock block = findOrThrow(id);
        block.setActive(false);
    }

    private void applyEventualDefaults(ScheduleBlock block) {
        if (block.getType() == ScheduleBlockType.EVENTUAL) {
            block.setValidFrom(block.getBlockDate());
            block.setValidUntil(block.getBlockDate());
        }
    }

    private void validateConsistency(ScheduleBlock block) {
        if (block.getType() == null) {
            throw new BadRequestException("Tipo de bloqueio é obrigatório.");
        }

        if (block.getType() == ScheduleBlockType.EVENTUAL) {
            if (block.getBlockDate() == null) {
                throw new BadRequestException("Data é obrigatória para bloqueio eventual.");
            }

            if (block.getRecurrence() != null) {
                throw new BadRequestException(
                        "Recorrência não deve ser informada para bloqueio eventual.");
            }

            if (block.getWeekday() != null || block.getMonthDay() != null) {
                throw new BadRequestException(
                        "Dia da semana/mês não deve ser informado para bloqueio eventual.");
            }
        } else {
            if (block.getBlockDate() != null) {
                throw new BadRequestException(
                        "Data não deve ser informada para bloqueio recorrente.");
            }

            if (block.getRecurrence() == null) {
                throw new BadRequestException(
                        "Recorrência é obrigatória para bloqueio recorrente.");
            }

            validateRecurrenceFields(block);

            if (block.getValidFrom() == null) {
                throw new BadRequestException("Início da vigência é obrigatório.");
            }
        }

        if (block.getStartTime() == null || block.getEndTime() == null) {
            throw new BadRequestException("Horário de início e fim são obrigatórios.");
        }

        if (block.getValidFrom() != null
                && block.getValidUntil() != null
                && block.getValidUntil().isBefore(block.getValidFrom())) {
            throw new BadRequestException("Fim da vigência não pode ser anterior ao início.");
        }
    }

    private void validateRecurrenceFields(ScheduleBlock block) {
        switch (block.getRecurrence()) {
            case WEEKLY -> {
                if (block.getWeekday() == null) {
                    throw new BadRequestException(
                            "Dia da semana é obrigatório para recorrência semanal.");
                }

                if (block.getWeekday() < 1 || block.getWeekday() > 7) {
                    throw new BadRequestException(
                            "Dia da semana deve ser entre 1 (segunda) e 7 (domingo).");
                }

                if (block.getMonthDay() != null) {
                    throw new BadRequestException(
                            "Dia do mês só deve ser informado para recorrência mensal.");
                }
            }

            case MONTHLY -> {
                if (block.getMonthDay() == null) {
                    throw new BadRequestException(
                            "Dia do mês é obrigatório para recorrência mensal.");
                }

                if (block.getMonthDay() < 1 || block.getMonthDay() > 31) {
                    throw new BadRequestException("Dia do mês deve ser entre 1 e 31.");
                }

                if (block.getWeekday() != null) {
                    throw new BadRequestException(
                            "Dia da semana só deve ser informado para recorrência semanal.");
                }
            }

            case DAILY -> {
                if (block.getWeekday() != null || block.getMonthDay() != null) {
                    throw new BadRequestException(
                            "Dia da semana/mês não deve ser informado para recorrência diária.");
                }
            }
        }
    }

    private ScheduleBlock findOrThrow(Long id) {
        return scheduleBlockRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bloqueio não encontrado."));
    }

    private ScheduleBlockResponse toResponse(ScheduleBlock block) {
        return new ScheduleBlockResponse(
                block.getId(),
                block.getType(),
                block.getBlockDate(),
                block.getWeekday(),
                block.getMonthDay(),
                block.getRecurrence(),
                block.getStartTime(),
                block.getEndTime(),
                block.getValidFrom(),
                block.getValidUntil(),
                block.isActive());
    }
}
