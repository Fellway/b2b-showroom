package pl.wizytowka.linesheet.adapter.in.web;

import java.util.UUID;

record CreatedLineSheetResponse(UUID id, String referenceNumber, String pdfUrl) {
}
