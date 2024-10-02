package org.tableextractor;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import org.apache.pdfbox.pdmodel.PDDocument;
import technology.tabula.ObjectExtractor;
import technology.tabula.Page;
import technology.tabula.extractors.SpreadsheetExtractionAlgorithm;

import javax.swing.*;
import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

public class TableExtractor {
    public static void main(String[] args) throws Exception {

        // Crea un JFileChooser per selezionare il PDF
        JFileChooser fileChooser = new JFileChooser(); //Avvio finestra GUI
        fileChooser.setDialogTitle("Seleziona un file PDF");
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        int result = fileChooser.showOpenDialog(null);

        // Controlla se l'utente ha selezionato un file PDF
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            String filePath = selectedFile.getAbsolutePath();

            // Carica il documento PDF selezionato usando PDFBox (necessario per Tabula)
            PDDocument pdDocument = PDDocument.load(new File(filePath));

            // Ottieni il numero totale di pagine
            int totalPages = pdDocument.getNumberOfPages();

            // Crea un ObjectExtractor per estrarre le pagine del PDF
            ObjectExtractor extractor = new ObjectExtractor(pdDocument);

            // Usa SpreadsheetExtractionAlgorithm per estrarre le tabelle
            SpreadsheetExtractionAlgorithm algorithm = new SpreadsheetExtractionAlgorithm();

            // Creare un PDF di output per scrivere la tabella estratta
            String outputPdfPath = "C:\\Users\\Oscar Costanzelli\\Downloads\\tabellaestratta.pdf";
            PdfWriter writer = new PdfWriter(new FileOutputStream(outputPdfPath));
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);

            // Itera attraverso le pagine del documento PDF
            for (int pageNum = 1; pageNum <= totalPages; pageNum++) {
                // Estrai la pagina corrente
                Page page = extractor.extract(pageNum);

                // Estrai le tabelle dalla pagina corrente
                List<technology.tabula.Table> tables = algorithm.extract(page);

                // Controlla se ci sono tabelle nella pagina corrente
                if (!tables.isEmpty()) {
                    for (technology.tabula.Table table : tables) {
                        int columnCount = table.getColCount(); // Ottiene il numero di colonne

                        // Crea una tabella iText con il numero corretto di colonne rilevato
                        Table itextTable = new Table(columnCount);

                        // Itera attraverso le righe della tabella
                        for (List<technology.tabula.RectangularTextContainer> row : table.getRows()) {
                            // Itera attraverso le celle della riga corrente
                            for (technology.tabula.RectangularTextContainer cell : row) {
                                // Estrai il testo dalla cella e aggiungilo alla tabella iText
                                String text = cell.getText();
                                itextTable.addCell(new Cell().add(new Paragraph(text)));
                            }
                        }

                        // Aggiungi la tabella rilevata al documento PDF
                        document.add(itextTable);
                    }
                }
            }

            // Chiudi il documento PDF
            document.close();
            pdDocument.close();

            // Notifica l'utente del successo dell'operazione
            JOptionPane.showMessageDialog(null, "Dati estratti e salvati nel PDF: " + outputPdfPath);
        } else {
            // Notifica l'utente che non è stato selezionato alcun file
            JOptionPane.showMessageDialog(null, "Nessun file PDF selezionato");
        }
    }
}
