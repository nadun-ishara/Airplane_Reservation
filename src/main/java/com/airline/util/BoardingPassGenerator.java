package com.airline.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility to generate modern, production-grade PDF boarding passes with embedded QR verification.
 */
public class BoardingPassGenerator {

    public static boolean generate(
            Stage owner,
            String pnr,
            String passengerName,
            String airline,
            String route,
            String departureTime,
            String seatNumber,
            double price,
            String status) {

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Boarding Pass");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files (*.pdf)", "*.pdf"));
        fileChooser.setInitialFileName("BoardingPass_" + pnr + ".pdf");

        File file = fileChooser.showSaveDialog(owner);
        if (file == null) {
            return false;
        }

        try {
            Document document = new Document(PageSize.A4, 36, 36, 36, 36);
            PdfWriter.getInstance(document, new FileOutputStream(file));
            document.open();

            // Color Palette
            BaseColor navy = new BaseColor(0, 51, 102);
            BaseColor skyBlue = new BaseColor(28, 161, 242);
            BaseColor darkGray = new BaseColor(51, 65, 85);
            BaseColor lightBg = new BaseColor(248, 250, 252);

            // Fonts
            Font brandFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, navy);
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 15, darkGray);
            Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, darkGray);
            Font valueFont = FontFactory.getFont(FontFactory.HELVETICA, 12, BaseColor.BLACK);
            Font pnrFont = FontFactory.getFont(FontFactory.COURIER_BOLD, 26, skyBlue);

            // Header Banner
            PdfPTable headerTable = new PdfPTable(2);
            headerTable.setWidthPercentage(100);
            headerTable.setWidths(new float[]{3, 2});

            PdfPCell c1 = new PdfPCell(new Phrase("✈  SkyLink Pro Airlines", brandFont));
            c1.setBorder(Rectangle.NO_BORDER);
            c1.setVerticalAlignment(Element.ALIGN_MIDDLE);
            headerTable.addCell(c1);

            PdfPCell c2 = new PdfPCell(new Phrase("BOARDING PASS", titleFont));
            c2.setBorder(Rectangle.NO_BORDER);
            c2.setHorizontalAlignment(Element.ALIGN_RIGHT);
            c2.setVerticalAlignment(Element.ALIGN_MIDDLE);
            headerTable.addCell(c2);

            document.add(headerTable);
            document.add(new Paragraph("\n"));

            // PNR Box with Embedded 2D QR Code
            PdfPTable pnrQrTable = new PdfPTable(2);
            pnrQrTable.setWidthPercentage(100);
            pnrQrTable.setWidths(new float[]{65f, 35f});

            PdfPCell pnrCell = new PdfPCell();
            pnrCell.setBackgroundColor(lightBg);
            pnrCell.setBorderColor(skyBlue);
            pnrCell.setBorderWidth(1.5f);
            pnrCell.setPadding(14);

            Paragraph pnrLabel = new Paragraph("BOOKING REFERENCE (PNR)", labelFont);
            Paragraph pnrCode = new Paragraph(pnr, pnrFont);
            Paragraph pnrSub = new Paragraph("Status: " + (status != null ? status.toUpperCase() : "CONFIRMED") + "  •  Electronic Ticket", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, new BaseColor(40, 167, 69)));

            pnrCell.addElement(pnrLabel);
            pnrCell.addElement(pnrCode);
            pnrCell.addElement(new Paragraph(" "));
            pnrCell.addElement(pnrSub);
            pnrQrTable.addCell(pnrCell);

            // Generate high-resolution verification QR Code
            PdfPCell qrCell = new PdfPCell();
            qrCell.setBackgroundColor(lightBg);
            qrCell.setBorderColor(skyBlue);
            qrCell.setBorderWidth(1.5f);
            qrCell.setPadding(8);
            qrCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            qrCell.setVerticalAlignment(Element.ALIGN_MIDDLE);

            try {
                String qrData = String.format("SKYLINK|PNR:%s|PAX:%s|SEAT:%s|AIRLINE:%s|ROUTE:%s",
                        pnr, passengerName, seatNumber, airline, route);
                QRCodeWriter qrWriter = new QRCodeWriter();
                Map<EncodeHintType, Object> hints = new HashMap<>();
                hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
                hints.put(EncodeHintType.MARGIN, 1);
                BitMatrix bitMatrix = qrWriter.encode(qrData, BarcodeFormat.QR_CODE, 200, 200, hints);

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                MatrixToImageWriter.writeToStream(bitMatrix, "PNG", baos);
                Image qrImage = Image.getInstance(baos.toByteArray());
                qrImage.scaleToFit(90, 90);
                qrImage.setAlignment(Element.ALIGN_CENTER);

                qrCell.addElement(qrImage);
                Paragraph qrHint = new Paragraph("GATE VERIFICATION SCAN", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7, darkGray));
                qrHint.setAlignment(Element.ALIGN_CENTER);
                qrCell.addElement(qrHint);
            } catch (Exception e) {
                qrCell.addElement(new Paragraph("[QR Code]", labelFont));
            }
            pnrQrTable.addCell(qrCell);

            document.add(pnrQrTable);
            document.add(new Paragraph("\n"));

            // Details Table
            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(100);
            table.setSpacingBefore(5f);

            addTableRow(table, "Passenger Name", passengerName, labelFont, valueFont, lightBg);
            addTableRow(table, "Seat Number", seatNumber, labelFont, valueFont, lightBg);
            addTableRow(table, "Operating Airline", airline, labelFont, valueFont, lightBg);
            addTableRow(table, "Flight Route", route, labelFont, valueFont, lightBg);
            addTableRow(table, "Departure Date & Time", departureTime != null ? departureTime : "Scheduled", labelFont, valueFont, lightBg);
            addTableRow(table, "Ticket Fare", String.format("$%.2f", price), labelFont, valueFont, lightBg);
            addTableRow(table, "Booking Status", status != null ? status : "Paid", labelFont, valueFont, lightBg);

            document.add(table);

            document.add(new Paragraph("\n\n"));
            Paragraph footer = new Paragraph("Please present this boarding pass along with a valid passport/NIC at the departure gate 45 minutes prior to scheduled departure time.", FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 9, BaseColor.GRAY));
            footer.setAlignment(Element.ALIGN_CENTER);
            document.add(footer);

            document.close();
            return true;
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }

    private static void addTableRow(PdfPTable table, String label, String val, Font labelFont, Font valFont, BaseColor bg) {
        PdfPCell cLabel = new PdfPCell(new Phrase(label, labelFont));
        cLabel.setBackgroundColor(bg);
        cLabel.setPadding(9);
        table.addCell(cLabel);

        PdfPCell cVal = new PdfPCell(new Phrase(val != null ? val : "N/A", valFont));
        cVal.setPadding(9);
        table.addCell(cVal);
    }
}
