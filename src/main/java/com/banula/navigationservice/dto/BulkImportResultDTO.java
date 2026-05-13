package com.banula.navigationservice.dto;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class BulkImportResultDTO {

    private int totalRows;
    private int successful;
    private int failed;
    private List<BulkImportErrorDTO> errors = new ArrayList<>();

    public void addError(long rowNumber, String locationKey, String message) {
        errors.add(new BulkImportErrorDTO(rowNumber, locationKey, message));
        failed++;
    }

    public void incrementSuccess() {
        successful++;
    }

    @Data
    @NoArgsConstructor
    public static class BulkImportErrorDTO {
        private long rowNumber;
        private String locationKey;
        private String message;

        public BulkImportErrorDTO(long rowNumber, String locationKey, String message) {
            this.rowNumber = rowNumber;
            this.locationKey = locationKey;
            this.message = message;
        }
    }
}
