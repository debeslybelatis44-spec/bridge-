package com.lotato.pro.print;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.lotato.pro.models.Bet;
import com.lotato.pro.models.Ticket;
import com.sunmi.peripheral.printer.InnerPrinterCallback;
import com.sunmi.peripheral.printer.InnerPrinterManager;
import com.sunmi.peripheral.printer.SunmiPrinterService;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SunmiPrintHelper {
    private static final String TAG = "SunmiPrintHelper";
    private static SunmiPrintHelper instance;

    private SunmiPrinterService sunmiPrinterService = null;
    private boolean isPrinterConnected = false;

    public interface PrintCallback {
        void onSuccess();
        void onError(String message);
    }

    private SunmiPrintHelper() {}

    public static SunmiPrintHelper getInstance() {
        if (instance == null) instance = new SunmiPrintHelper();
        return instance;
    }

    public void initPrinter(Context context) {
        try {
            InnerPrinterManager.getInstance().bindService(context, new InnerPrinterCallback() {
                @Override
                protected void onConnected(SunmiPrinterService service) {
                    sunmiPrinterService = service;
                    isPrinterConnected = true;
                    Log.d(TAG, "Sunmi printer connected");
                }

                @Override
                protected void onDisconnected() {
                    sunmiPrinterService = null;
                    isPrinterConnected = false;
                    Log.d(TAG, "Sunmi printer disconnected");
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Failed to init Sunmi printer: " + e.getMessage());
        }
    }

    public boolean isConnected() {
        return isPrinterConnected && sunmiPrinterService != null;
    }

    public void printTicket(Ticket ticket, String lotteryName, String agentName,
                            String footerLine1, String footerLine2, PrintCallback cb) {
        if (!isConnected()) {
            cb.onError("Imprimante non connectée. Assurez-vous d'être sur un appareil Sunmi.");
            return;
        }
        try {
            sunmiPrinterService.printerInit(null);

            printLine("================================");
            printCenterBold(lotteryName, 28);
            printLine("================================");
            printCenter("Tikè #" + ticket.getTicketId(), 22);
            printLine("Tiraj: " + ticket.getDrawName());
            printLine("Ajan: " + agentName);
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRENCH);
            printLine("Dat: " + sdf.format(new Date()));
            printLine("--------------------------------");

            List<Bet> bets = ticket.getBets();
            if (bets != null) {
                for (Bet bet : bets) {
                    if (bet.isFree()) {
                        printLine(String.format("  %-18s %s G (GRATIS)",
                            bet.getGameAbbreviation() + " " + bet.getNumber(), (int) bet.getAmount()));
                    } else {
                        printLine(String.format("  %-18s %s G",
                            bet.getGameAbbreviation() + " " + bet.getNumber(), (int) bet.getAmount()));
                    }
                }
            }

            printLine("================================");
            printLineBold("TOTAL: " + (int) ticket.getTotalAmount() + " Gdes");
            printLine("================================");

            if (footerLine1 != null && !footerLine1.isEmpty()) printCenter(footerLine1, 18);
            if (footerLine2 != null && !footerLine2.isEmpty()) printCenter(footerLine2, 18);
            printCenter("LOTATO S.A.", 18);

            feedPaper();
            cb.onSuccess();
        } catch (RemoteException e) {
            Log.e(TAG, "Print error: " + e.getMessage());
            cb.onError("Erreur d'impression: " + e.getMessage());
        }
    }

    public void printReprintTicket(Ticket ticket, String lotteryName, PrintCallback cb) {
        if (!isConnected()) {
            cb.onError("Imprimante non connectée.");
            return;
        }
        try {
            sunmiPrinterService.printerInit(null);
            printLine("================================");
            printCenterBold("** REIMPRESSION **", 22);
            printCenterBold(lotteryName, 26);
            printLine("================================");
            printCenter("Tikè #" + ticket.getTicketId(), 22);
            printLine("Tiraj: " + ticket.getDrawName());

            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRENCH);
            printLine("Dat: " + sdf.format(new Date()));
            printLine("--------------------------------");

            List<Bet> bets = ticket.getBets();
            if (bets != null) {
                for (Bet bet : bets) {
                    printLine(String.format("  %-18s %s G",
                        bet.getGameAbbreviation() + " " + bet.getNumber(), (int) bet.getAmount()));
                }
            }

            printLine("================================");
            printLineBold("TOTAL: " + (int) ticket.getTotalAmount() + " Gdes");

            if (ticket.isChecked()) {
                if (ticket.getWinAmount() > 0) {
                    printLine("ESTATOU: GANYEN " + (int) ticket.getWinAmount() + " G");
                } else {
                    printLine("ESTATOU: PEDI");
                }
            } else {
                printLine("ESTATOU: AP TANN");
            }

            printLine("================================");
            feedPaper();
            cb.onSuccess();
        } catch (RemoteException e) {
            cb.onError("Erreur: " + e.getMessage());
        }
    }

    public void printReport(String agentName, int totalTickets, double totalBets,
                            double totalWins, double balance, PrintCallback cb) {
        if (!isConnected()) {
            cb.onError("Imprimante non connectée.");
            return;
        }
        try {
            sunmiPrinterService.printerInit(null);
            printLine("================================");
            printCenterBold("LOTATO PRO", 26);
            printCenterBold("RAPÒ AJAN", 22);
            printLine("================================");
            printLine("Ajan: " + agentName);
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRENCH);
            printLine("Dat: " + sdf.format(new Date()));
            printLine("--------------------------------");
            printLine(String.format("%-20s %s", "Total Tikè:", totalTickets));
            printLine(String.format("%-20s %s G", "Total Pari:", (int) totalBets));
            printLine(String.format("%-20s %s G", "Total Ganyen:", (int) totalWins));
            printLine("================================");
            printLineBold(String.format("%-20s %s G", "Balans:", (int) balance));
            printLine("================================");
            feedPaper();
            cb.onSuccess();
        } catch (RemoteException e) {
            cb.onError("Erreur: " + e.getMessage());
        }
    }

    private void printLine(String text) throws RemoteException {
        sunmiPrinterService.setFontSize(20, null);
        sunmiPrinterService.setAlignment(0, null);
        sunmiPrinterService.printText(text + "\n", null);
    }

    private void printLineBold(String text) throws RemoteException {
        sunmiPrinterService.setFontSize(22, null);
        sunmiPrinterService.setBold(true, null);
        sunmiPrinterService.setAlignment(0, null);
        sunmiPrinterService.printText(text + "\n", null);
        sunmiPrinterService.setBold(false, null);
    }

    private void printCenter(String text, int size) throws RemoteException {
        sunmiPrinterService.setFontSize(size, null);
        sunmiPrinterService.setAlignment(1, null);
        sunmiPrinterService.printText(text + "\n", null);
    }

    private void printCenterBold(String text, int size) throws RemoteException {
        sunmiPrinterService.setFontSize(size, null);
        sunmiPrinterService.setBold(true, null);
        sunmiPrinterService.setAlignment(1, null);
        sunmiPrinterService.printText(text + "\n", null);
        sunmiPrinterService.setBold(false, null);
    }

    private void feedPaper() throws RemoteException {
        sunmiPrinterService.lineWrap(3, null);
    }

    public void destroyService(Context context) {
        try {
            InnerPrinterManager.getInstance().unBindService(context, null);
        } catch (Exception e) {
            Log.e(TAG, "Error unbinding printer: " + e.getMessage());
        }
    }
}

