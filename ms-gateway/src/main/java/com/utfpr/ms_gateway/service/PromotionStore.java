package com.utfpr.ms_gateway.service;

import com.utfpr.ms_gateway.dto.PromotionDTO;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class PromotionStore {

    private final List<PromotionDTO> promotions = new CopyOnWriteArrayList<>();

    public void add(PromotionDTO dto) {
        promotions.add(0, dto);
    }

    public List<PromotionDTO> getAll() {
        return List.copyOf(promotions);
    }
}
