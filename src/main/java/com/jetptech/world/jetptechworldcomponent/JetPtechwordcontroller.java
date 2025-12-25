package com.jetptech.world.jetptechworldcomponent;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.file.Files;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/convert")
@CrossOrigin(origins = "http://localhost:5173")  
public class JetPtechwordcontroller {

    private static final String LIBREOFFICE_PATH = "C:\\Program Files\\LibreOffice\\program\\soffice.exe";

    // WORD → PDF
    @PostMapping(value = "/word", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<byte[]> wordToPdf(@RequestParam("file") MultipartFile file) throws Exception {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("No file uploaded".getBytes());
        }

        File inputFile = File.createTempFile("input-", getExtension(file.getOriginalFilename()));
        file.transferTo(inputFile);

        File outputDir = new File(System.getProperty("java.io.tmpdir"));

        Process process = new ProcessBuilder(
                LIBREOFFICE_PATH,
                "--headless",
                "--convert-to", "pdf",
                "--outdir", outputDir.getAbsolutePath(),
                inputFile.getAbsolutePath()
        ).start();

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("LibreOffice conversion failed with exit code: " + exitCode);
        }

        String pdfName = inputFile.getName().replaceAll("\\.[^.]+$", ".pdf");
        File pdfFile = new File(outputDir, pdfName);

        if (!pdfFile.exists()) {
            throw new RuntimeException("Converted PDF file not found");
        }

        byte[] pdfBytes = Files.readAllBytes(pdfFile.toPath());

        // Clean up temp files
        inputFile.delete();
        pdfFile.delete();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=converted-word.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    private String getExtension(String originalFilename) {
		// TODO Auto-generated method stub
		return null;
	}

	// IMAGE → PDF
    @PostMapping(value = "/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<byte[]> imageToPdf(@RequestParam("file") MultipartFile file) throws Exception {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("No file uploaded".getBytes());
        }

        PDDocument doc = new PDDocument();
        PDPage page = new PDPage();
        doc.addPage(page);

        PDImageXObject pdImage = PDImageXObject.createFromByteArray(doc, file.getBytes(), file.getOriginalFilename());

        float pageWidth = page.getMediaBox().getWidth();
        float pageHeight = page.getMediaBox().getHeight();

        float imageWidth = pdImage.getWidth();
        float imageHeight = pdImage.getHeight();

        float ratio = Math.min((pageWidth - 100) / imageWidth, (pageHeight - 100) / imageHeight);
        float scaledWidth = imageWidth * ratio;
        float scaledHeight = imageHeight * ratio;

        float x = (pageWidth - scaledWidth) / 2;
        float y = (pageHeight - scaledHeight) / 2;

        try (PDPageContentStream content = new PDPageContentStream(doc, page)) {
            content.drawImage(pdImage, x, y, scaledWidth, scaledHeight);
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        doc.save(out);
        doc.close();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=converted-image.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(out.toByteArray());
    }
    
    
    
    
    
    @PostMapping(value = "/excel", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<byte[]> excelToPdf(@RequestParam("file") MultipartFile file) throws Exception {

        // 1. Save Excel file
        File inputFile = File.createTempFile("excel-", ".xlsx");
        file.transferTo(inputFile);

        // 2. Output directory
        File outputDir = Files.createTempDirectory("excel-pdf").toFile();

        // 3. LibreOffice path
        String libreOfficePath =
                "C:\\Program Files\\LibreOffice\\program\\soffice.exe";

        // 4. SAFE conversion command (NO EMPTY PDF)
        Process process = new ProcessBuilder(
                libreOfficePath,
                "--headless",
                "--nologo",
                "--nofirststartwizard",
                "--convert-to", "pdf",
                "--outdir", outputDir.getAbsolutePath(),
                inputFile.getAbsolutePath()
        ).redirectErrorStream(true).start();

        process.waitFor();

        // 5. Read generated PDF
        File[] pdfFiles = outputDir.listFiles((dir, name) -> name.endsWith(".pdf"));

        if (pdfFiles == null || pdfFiles.length == 0) {
            throw new RuntimeException("PDF not generated");
        }

        byte[] pdfBytes = Files.readAllBytes(pdfFiles[0].toPath());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=excel.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }
    
    
    
    
    
    //pdf passoword
   
    @PostMapping(value = "/protect", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<byte[]> protectPdf(
            @RequestParam("file") MultipartFile file,
            @RequestParam("password") String password) throws Exception {

        if (file.isEmpty() || password.isEmpty()) {
            return ResponseEntity.badRequest().body("File or password missing".getBytes());
        }

        PDDocument document = PDDocument.load(file.getInputStream());

        AccessPermission ap = new AccessPermission();
        ap.setCanPrint(true);
        ap.setCanModify(false);
        ap.setCanExtractContent(false);

        StandardProtectionPolicy policy = new StandardProtectionPolicy(password, password, ap);
        policy.setEncryptionKeyLength(128);
        policy.setPreferAES(true);

        document.protect(policy);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        document.save(out);
        document.close();

        String protectedName = file.getOriginalFilename().replace(".pdf", "_protected.pdf");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + protectedName)
                .contentType(MediaType.APPLICATION_PDF)
                .body(out.toByteArray());
    }


    
}