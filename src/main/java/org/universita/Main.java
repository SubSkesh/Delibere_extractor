package org.universita;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Date;


public class Main {

    public static void main(String[] args) {
        // Interfaccia grafica per selezionare la cartella
        JFrame frame = new JFrame();
        JFileChooser fileChooser = new JFileChooser();

        SwingUtilities.invokeLater(() -> {  //E' un metodo di utilità di swing SwingUtilities.invokeLater(Runnable doRun) ,() -> { è una lambda expression che implementa l'interfaccia runnable passata come parametro
            while (true) {
                fileChooser.setDialogTitle("Seleziona la cartella principale");
                fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                fileChooser.setCurrentDirectory(new File(System.getProperty("user.home")));
                int option = fileChooser.showOpenDialog(null);  // apre la selzione del file JFileChooser.APPROVE_OPTION: L'utente ha selezionato una cartella e ha confermato.
//                                                                                                   JFileChooser.CANCEL_OPTION o JFileChooser.ERROR_OPTION: L'utente ha annullato l'operazione o si è verificato un errore.

                if (option == JFileChooser.APPROVE_OPTION) {
                    File selectedDirectory = fileChooser.getSelectedFile();
                    String selectedFolderPath = selectedDirectory.getAbsolutePath();
                    String timestamp = new SimpleDateFormat("yyMMddHHmmss").format(new Date());
                    String pdfFilePath = System.getProperty("user.home") + File.separator + "Downloads" + File.separator + "delibere_" + timestamp + ".pdf";

                    try {
                        // Trova i PDF nelle cartelle e genera il PDF
                        List<File> lastPdfFiles = findLastMatchingPdfFiles(selectedFolderPath);
                        generatePDF(lastPdfFiles, pdfFilePath);

                        JOptionPane.showMessageDialog(frame, "File PDF generato con successo: " + pdfFilePath);
                    } catch (IOException e) {
                        JOptionPane.showMessageDialog(frame, "Errore nella generazione del PDF: " + e.getMessage(), "Errore", JOptionPane.ERROR_MESSAGE);
                    }
                } else {
                    System.exit(0);
                }
            }
        });
    }


    // Trova l'ultimo file PDF che segue il pattern "000_-*.pdf" nelle sottocartelle
    public static List<File> findLastMatchingPdfFiles(String baseFolderPath) {
        List<File> matchingPdfFiles = new ArrayList<>();
        File baseFolder = new File(baseFolderPath);

        // Cicla attraverso tutte le cartelle figlie
        File[] folders = baseFolder.listFiles(File::isDirectory);
        if (folders != null) {
            for (File folder : folders) {
                // Trova tutti i file PDF che seguono il pattern "000_-*.pdf"
                File[] pdfFiles = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".pdf") && name.matches("^\\d{3}_-.*\\.pdf"));

                if (pdfFiles != null && pdfFiles.length > 0) {
                    // Ordina i file in base al numero estratto dal nome (es. 000_- diventa 0, 001_- diventa 1)
                    Arrays.sort(pdfFiles, (file1, file2) -> {
                        int num1 = extractNumberFromFileName(file1.getName());
                        int num2 = extractNumberFromFileName(file2.getName());
                        return Integer.compare(num1, num2);
                    });

                    // Aggiungi solo l'ultimo file PDF (quello con il numero più alto)
                    matchingPdfFiles.add(pdfFiles[pdfFiles.length - 1]);
                }
            }
        }

        return matchingPdfFiles;
    }

    // Funzione per estrarre il numero dal nome del file (es: 000_- diventa 0, 002_- diventa 2)
    public static int extractNumberFromFileName(String fileName) {
        String[] parts = fileName.split("_");
        try {
            return Integer.parseInt(parts[0]);  // Il numero è la prima parte del nome del file
        } catch (NumberFormatException e) {
            return -1;  // Restituisce -1 se non riesce a convertire il numero (fallisce)
        }
    }

    // Genera il file PDF con i dati estratti dai PDF
    public static void generatePDF(List<File> pdfFiles, String pdfFilePath) throws IOException {
        // Inizializza il PDF Writer e crea il documento
        PdfWriter writer = new PdfWriter(pdfFilePath);
        PdfDocument pdfDocument = new PdfDocument(writer);
        Document document = new Document(pdfDocument);

        // Crea la tabella con 3 colonne
        Table table = new Table(UnitValue.createPercentArray(new float[]{1, 2, 4}));
        table.setWidth(UnitValue.createPercentValue(100));

        // Crea la prima riga con i titoli delle colonne
        table.addHeaderCell(new Cell().add(new Paragraph("Numero Cartella").setBold()).setTextAlignment(TextAlignment.CENTER));
        table.addHeaderCell(new Cell().add(new Paragraph("Titolo Delibera").setBold()).setTextAlignment(TextAlignment.CENTER));
        table.addHeaderCell(new Cell().add(new Paragraph("Abstract").setBold()).setTextAlignment(TextAlignment.CENTER));

        for (File pdfFile : pdfFiles) {
            String folderName = pdfFile.getParentFile().getName();  // Numero della cartella
            String[] deliberaInfo = extractTextFromPDF(pdfFile);

            // Sostituisce spazi multipli con un solo spazio
            String titolo = deliberaInfo[0].replaceAll("\\s+", " ");
            String abstractText = deliberaInfo[1].replaceAll("\\s+", " ");

            // Aggiungi una riga alla tabella
            table.addCell(new Cell().add(new Paragraph(folderName)));
            table.addCell(new Cell().add(new Paragraph(titolo)));
            table.addCell(new Cell().add(new Paragraph(abstractText)));
        }

        // Aggiungi la tabella al documento
        document.add(table);

        // Chiudi il documento
        document.close();
    }

    // Funzione aggiornata per estrarre titolo e abstract da un file PDF
    public static String[] extractTextFromPDF(File pdfFile) throws IOException {
        try (PDDocument document = PDDocument.load(pdfFile)) {
            PDFTextStripper pdfStripper = new PDFTextStripper();
            String text = pdfStripper.getText(document);

            StringBuilder titoloBuilder = new StringBuilder();
            StringBuilder abstractBuilder = new StringBuilder();

            // Suddividi il testo in linee singole
            String[] lines = text.split("\\r?\\n");

            boolean oggettoFound = false;
            boolean inTitle = false;

            // Definisci parole chiave che indicano l'inizio dell'abstract
            List<String> abstractStartWords = Arrays.asList("Il", "La", "Viene", "L'", "L’", "I", "Le", "Gli", "Lo");

            for (String line : lines) {
                String trimmedLine = line.trim();
                if (trimmedLine.isEmpty()) {
                    continue; // Salta le linee vuote
                }

                if (!oggettoFound) {
                    // Cerca "Oggetto:" nel testo
                    if (trimmedLine.toLowerCase().startsWith("oggetto:")) {
                        oggettoFound = true;
                        inTitle = true;
                        String afterOggetto = trimmedLine.substring("oggetto:".length()).trim();
                        titoloBuilder.append(afterOggetto);
                        continue;
                    } else {
                        // Se "Oggetto:" non è presente, considera la prima linea non vuota come inizio del titolo
                        oggettoFound = true;
                        inTitle = true;
                        titoloBuilder.append(trimmedLine);
                        continue;
                    }
                }

                if (inTitle) {
                    // Estrai la prima parola della linea
                    String[] words = trimmedLine.split("\\s+");
                    String firstWord = words.length > 0 ? words[0] : "";

                    if (abstractStartWords.contains(firstWord)) {
                        inTitle = false;
                        abstractBuilder.append(trimmedLine).append(" ");
                    } else {
                        titoloBuilder.append(" ").append(trimmedLine);
                    }
                } else {
                    // Appende all'abstract
                    abstractBuilder.append(trimmedLine).append(" ");
                }
            }

            // Pulizia finale: rimuovi spazi multipli
            String titolo = titoloBuilder.toString().replaceAll("\\s+", " ").trim();
            String abstractText = abstractBuilder.toString().replaceAll("\\s+", " ").trim();

            return new String[]{titolo, abstractText};
        }
    }
}
