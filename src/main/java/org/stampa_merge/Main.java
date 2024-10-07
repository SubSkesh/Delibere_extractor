package org.stampa_merge;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.io.MemoryUsageSetting;
import org.apache.pdfbox.multipdf.PDFMergerUtility;

import javax.swing.JFrame;
import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;
import javax.swing.JOptionPane;

public class Main {

    // Funzione che trova il file con il nome più grande all'interno di una cartella
    public static File findLargestNamedPDF(File folder) {
        File[] pdfFiles = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".pdf"));
        if (pdfFiles == null || pdfFiles.length == 0) {
            return null;
        }
        File largestNamedFile = pdfFiles[0];
        for (File file : pdfFiles) {
            if (file.getName().compareTo(largestNamedFile.getName()) > 0) {
                largestNamedFile = file;
            }
        }
        return largestNamedFile;
    }

    // Funzione per eliminare tutti i PDF tranne il più grande (per nome)
    public static void removeAllExceptLargestPDF(File folder) {
        File largestPDF = findLargestNamedPDF(folder);
        if (largestPDF != null) {
            File[] pdfFiles = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".pdf"));
            for (File file : pdfFiles) {
                if (!file.equals(largestPDF)) {
                    boolean deleted = file.delete(); // Elimina tutti i file tranne quello con il nome più grande
                    if (!deleted) {
                        System.err.println("Failed to delete: " + file.getAbsolutePath());
                    }
                }
            }
        }
    }

    // Funzione per trovare le cartelle che contengono PDF
    public static List<String> findMatchingFolders(String basePath) {
        List<String> matchingFolders = new ArrayList<>();
        File baseFolder = new File(basePath);
        File[] folders = baseFolder.listFiles(File::isDirectory);
        if (folders != null) {
            for (File folder : folders) {
                if (containsMatchingPDFs(folder)) {
                    // Elimina tutti i PDF tranne quello con il nome più grande
                    removeAllExceptLargestPDF(folder);
                    matchingFolders.add(folder.getAbsolutePath());
                }
            }
        }
        Collections.sort(matchingFolders);
        return matchingFolders;
    }

    public static boolean containsMatchingPDFs(File folder) {
        File[] files = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".pdf"));
        return files != null && files.length > 0;
    }

    public static int getNumberOfPages(String filePath) throws IOException {
        try (PDDocument document = PDDocument.load(new File(filePath))) {
            return document.getNumberOfPages();
        }
    }

    public static void mergePDFs(List<String> folderPaths, String mergedFilePath) throws IOException {
        PDFMergerUtility pdfMerger = new PDFMergerUtility();
        List<String> folderNames = new ArrayList<>();
        List<Integer> pageCounts = new ArrayList<>();

        for (String folderPath : folderPaths) {
            File folder = new File(folderPath);
            File[] files = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".pdf"));

            if (files != null) {
                for (File file : files) {
                    pdfMerger.addSource(file);

                    String folderName = folder.getName();
                    folderNames.add(folderName);

                    int numPages = getNumberOfPages(file.getAbsolutePath());

                    // Rimuovere l'aggiunta di una pagina vuota
                    // Commentare o eliminare la sezione seguente
                    /*
                    if (numPages % 2 == 1) {
                        PDDocument document = new PDDocument();
                        PDPage page = new PDPage(new PDRectangle(595, 842));
                        document.addPage(page);

                        Path tempFilePath = Files.createTempFile("temp", ".pdf");
                        document.save(tempFilePath.toFile());

                        document.close();

                        pdfMerger.addSource(new File(tempFilePath.toString()));
                        pageCounts.add(numPages + 1);
                    } else {
                        pageCounts.add(numPages);
                    }
                    */

                    // Aggiungi solo il numero di pagine effettive
                    pageCounts.add(numPages);
                }
            }
        }

        pdfMerger.setDestinationFileName(mergedFilePath);
        pdfMerger.mergeDocuments(MemoryUsageSetting.setupMainMemoryOnly());

        try (PDDocument mergedDocument = PDDocument.load(new File(mergedFilePath))) {
            int pageIndex = 0;
            for (int i = 0; i < folderNames.size(); i++) {
                String folderName = folderNames.get(i);
                int numPages = pageCounts.get(i);
                for (int j = 0; j < numPages; j++) {
                    PDPage page = mergedDocument.getPage(pageIndex);
                    try (PDPageContentStream contentStream = new PDPageContentStream(
                            mergedDocument, page, PDPageContentStream.AppendMode.APPEND, true, true)) {
                        if (j == 0) {
                            addTitle(contentStream, page, folderName);
                        }
                    }
                    pageIndex++;
                }
            }
            mergedDocument.save(new File(mergedFilePath));
        }
    }

    public static void addTitle(PDPageContentStream contentStream, PDPage page, String title) throws IOException {
        // Recupera la dimensione della pagina corrente
        PDRectangle mediaBox = page.getMediaBox();
        float yPosition = mediaBox.getUpperRightY() - 50; // 50 punti dal bordo superiore

        // Assicurati che yPosition sia all'interno della pagina
        if (yPosition < 0) {
            yPosition = mediaBox.getUpperRightY() - 20; // Regola a una posizione sicura
        }

        contentStream.beginText();
        contentStream.setFont(PDType1Font.HELVETICA_BOLD, 12);
        contentStream.newLineAtOffset(50, yPosition);
        contentStream.showText("Folder: " + title);
        contentStream.endText();
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame();
        JFileChooser fileChooser = new JFileChooser();

        SwingUtilities.invokeLater(() -> {
            // Rimuovere il ciclo while(true) per evitare loop infiniti
            fileChooser.setDialogTitle("Select Folder");
            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            fileChooser.setCurrentDirectory(new File(System.getProperty("user.home")));

            int option = fileChooser.showOpenDialog(null);

            if (option == JFileChooser.APPROVE_OPTION) {
                File selectedDirectory = fileChooser.getSelectedFile();
                String selectedFolderPath = selectedDirectory.getAbsolutePath();
                String timestamp = new SimpleDateFormat("yyMMddHHmmss").format(new Date());
                String mergedFilePath = System.getProperty("user.home") + File.separator + "Downloads" + File.separator + "merge_" + timestamp + ".pdf";

                try {
                    List<String> matchingFolders = findMatchingFolders(selectedFolderPath);
                    if (matchingFolders.isEmpty()) {
                        JOptionPane.showMessageDialog(frame, "No matching PDFs found in the selected folder.", "Info", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        mergePDFs(matchingFolders, mergedFilePath);
                        System.out.println("Merge completed successfully.");
                        JOptionPane.showMessageDialog(frame, "Merge completed successfully.");
                    }
                } catch (IOException e) {
                    System.err.println("Error merging PDFs: " + e.getMessage());
                    e.printStackTrace(); // Stampa lo stack trace completo
                    JOptionPane.showMessageDialog(frame, "Error merging PDFs: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                System.out.println("No folder selected.");
                System.exit(0);
            }
        });
    }
}
