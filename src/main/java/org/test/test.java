package org.test;

import java.io.File;
import java.io.IOException;
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

import javax.swing.JFrame;
import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;
import javax.swing.JOptionPane;

public class test {

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
            System.out.println("Keeping largest PDF: " + largestPDF.getName() + " in folder: " + folder.getName());
            File[] pdfFiles = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".pdf"));
            for (File file : pdfFiles) {
                if (!file.equals(largestPDF)) {
                    System.out.println("Deleting PDF: " + file.getName() + " from folder: " + folder.getName());
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

    // Metodo per unire i PDF
    public static void mergePDFs(List<String> folderPaths, String mergedFilePath) throws IOException {
        try (PDDocument mergedDocument = new PDDocument()) {
            // Calcola il totale dei PDF da unire per gestire l'ultimo PDF
            int totalPDFs = 0;
            for (String folderPath : folderPaths) {
                File folder = new File(folderPath);
                File[] files = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".pdf"));
                if (files != null) {
                    totalPDFs += files.length;
                }
            }

            int currentPDF = 0;

            for (String folderPath : folderPaths) {
                File folder = new File(folderPath);
                File[] files = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".pdf"));
                if (files != null) {
                    for (File file : files) {
                        currentPDF++;
                        System.out.println("Processing PDF: " + file.getName() + " from folder: " + folder.getName());
                        try (PDDocument pdf = PDDocument.load(file)) {
                            int numPages = pdf.getNumberOfPages();
                            if (numPages > 0) {
                                List<PDPage> importedPages = new ArrayList<>();

                                for (int i = 0; i < numPages; i++) {
                                    PDPage page = pdf.getPage(i);
                                    PDPage importedPage = mergedDocument.importPage(page);
                                    importedPages.add(importedPage);
                                    System.out.println("Imported page " + (i + 1) + " from PDF: " + file.getName());

                                    if (i == 0) { // Solo la prima pagina di ogni PDF riceve un titolo
                                        System.out.println("Adding title to first page of PDF: " + file.getName());
                                        try (PDPageContentStream contentStream = new PDPageContentStream(
                                                mergedDocument, importedPage, PDPageContentStream.AppendMode.APPEND, true, true)) {
                                            addTitle(contentStream, importedPage, folder.getName());
                                            System.out.println("Added title to PDF: " + file.getName());
                                        }
                                    }
                                }
                                System.out.println("Added " + numPages + " pages from PDF: " + file.getName());

                                // Aggiungi una pagina vuota se il numero di pagine è dispari e non è l'ultimo PDF
                                boolean isLastPDF = currentPDF == totalPDFs;
                                if (numPages % 2 != 0 && !isLastPDF) {
                                    PDPage blankPage = new PDPage(new PDRectangle(595, 842)); // Dimensione A4
                                    mergedDocument.addPage(blankPage);
                                    System.out.println("Added blank page after PDF: " + file.getName());
                                }
                            }
                        } catch (IOException e) {
                            System.err.println("Failed to process PDF: " + file.getName() + " due to: " + e.getMessage());
                            e.printStackTrace(); // Stampa lo stack trace completo
                            throw e;
                        }
                    }
                }
            }

            // Salva il documento unito
            try {
                mergedDocument.save(mergedFilePath);
                System.out.println("Merge completed successfully.");
                JOptionPane.showMessageDialog(new JFrame(), "Merge completed successfully.");
            } catch (IOException e) {
                System.err.println("Error saving merged PDF: " + e.getMessage());
                e.printStackTrace(); // Stampa lo stack trace completo
                throw e;
            }
        } catch (IOException e) {
            System.err.println("Error merging PDFs: " + e.getMessage());
            e.printStackTrace(); // Stampa lo stack trace completo
            JOptionPane.showMessageDialog(new JFrame(), "Error merging PDFs: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            throw e;
        }
    }

    // Metodo helper per aggiungere un titolo a una pagina PDF
    public static void addTitle(PDPageContentStream contentStream, PDPage page, String title) throws IOException {
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
