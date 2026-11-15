package com.jobapply.resume;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Service
public class ResumeParserService {

    public String extract(MultipartFile file) throws Exception {
        String name = file.getOriginalFilename() == null
                ? ""
                : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        byte[] bytes = file.getBytes();

        if (name.endsWith(".pdf")) {
            if (!hasPdfSignature(bytes)) {
                throw new IllegalArgumentException("The uploaded file does not contain a valid PDF signature");
            }
            try (var pdf = Loader.loadPDF(bytes)) {
                return new PDFTextStripper().getText(pdf).trim();
            }
        }

        if (name.endsWith(".docx")) {
            if (!hasZipLocalFileHeader(bytes)) {
                throw new IllegalArgumentException("The uploaded file is not a valid DOCX container");
            }
            try (var input = new ByteArrayInputStream(bytes);
                 var document = new XWPFDocument(input);
                 var extractor = new XWPFWordExtractor(document)) {
                return extractor.getText().trim();
            }
        }

        throw new IllegalArgumentException("Only PDF and DOCX files are supported");
    }

    /**
     * PDF readers should find the header close to the start of the file.
     * Allow a small leading preamble for otherwise valid PDF documents.
     */
    private boolean hasPdfSignature(byte[] bytes) {
        byte[] signature = "%PDF-".getBytes(StandardCharsets.US_ASCII);
        int lastPossibleOffset = Math.min(1024, bytes.length - signature.length);

        for (int offset = 0; offset <= lastPossibleOffset; offset++) {
            boolean matches = true;
            for (int index = 0; index < signature.length; index++) {
                if (bytes[offset + index] != signature[index]) {
                    matches = false;
                    break;
                }
            }
            if (matches) {
                return true;
            }
        }
        return false;
    }

    /**
     * DOCX files are ZIP-based OOXML packages. This rejects files that merely
     * have a .docx filename before passing their bytes to Apache POI.
     */
    private boolean hasZipLocalFileHeader(byte[] bytes) {
        return bytes.length >= 4
                && bytes[0] == 'P'
                && bytes[1] == 'K'
                && bytes[2] == 0x03
                && bytes[3] == 0x04;
    }
}
